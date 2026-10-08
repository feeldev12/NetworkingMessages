package me.feeldev.networkingmessages.networking.managers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.feeldev.networkingmessages.networking.client.ClientSender;
import me.feeldev.networkingmessages.networking.common.CommonAPI;
import me.feeldev.networkingmessages.networking.common.IMessagesManager;
import me.feeldev.networkingmessages.networking.common.MessageType;
import me.feeldev.networkingmessages.networking.exceptions.RegistryMessageException;
import me.feeldev.networkingmessages.networking.models.AbstractMessage;
import me.feeldev.networkingmessages.networking.models.MessageCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.event.EventNetworkChannel;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Forge 1.20.1 transport. Every message type gets its own {@link EventNetworkChannel} named
 * {@code namespace:channelId} (the same id Fabric and the Bukkit plugin channels use) and the payload
 * is the raw bytes of {@link AbstractMessage#encode}, so all loaders are byte-compatible.
 * The channels accept every version (including an absent channel) so a Forge client can still join a
 * Bukkit/vanilla server that does not know them.
 */
public class MessagesManager implements IMessagesManager<ServerPlayer, AbstractMessage<?>> {
    private static final String PROTOCOL_VERSION = "1";

    /** A registered message, by channel id. Channels are created once per JVM; this table is what they dispatch to. */
    private record Registration(MessageType messageType, AbstractMessage<?> prototype) {}

    private static final Map<ResourceLocation, Registration> REGISTRATIONS = new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> CHANNELS = ConcurrentHashMap.newKeySet();

    private final Map<MessageType, AbstractMessage<?>> messages = new ConcurrentHashMap<>();
    private final Map<Class<?>, MessageType> classTypes = new ConcurrentHashMap<>();
    private static MessagesManager instance;
    /**
     * Every manager by namespace. Each mod that uses this library on the same game builds its own
     * {@code ServerAPI} (and so its own manager); a {@code ClientAPI} sends through the one for its
     * namespace instead of whichever was built last.
     */
    private static final Map<String, MessagesManager> BY_NAMESPACE = new ConcurrentHashMap<>();

    private MinecraftServer server;
    private final String namespace;

    public MessagesManager(MinecraftServer server, String namespace) {
        this.server = server;
        this.namespace = namespace;
        instance = this;
        BY_NAMESPACE.put(namespace, this);
    }

    /**
     * The manager most recently built, whatever its namespace.
     *
     * @deprecated with several mods on this library there's one manager per namespace; use
     * {@link #forNamespace(String)}.
     */
    @Deprecated
    public static MessagesManager getInstance() {
        return instance;
    }

    /** The manager built for {@code namespace}, or {@code null} if no {@code ServerAPI} created one yet. */
    public static MessagesManager forNamespace(String namespace) {
        return BY_NAMESPACE.get(namespace);
    }

    public String getNamespace() {
        return namespace;
    }

    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void registerMessage(MessageType messageType, @NotNull AbstractMessage message) {
        if (classTypes.putIfAbsent(message.getClass(), messageType) != null) {
            throw new RegistryMessageException("Message " + messageType.getChannelIdWithNamespace() + " already registered");
        }
        if (messageType.isConfigurationPhase()) {
            CommonAPI.LOGGER.warn("[NetworkingMessages] {} asks for the configuration phase, which does not exist on 1.20.1; it is sent in the play phase", messageType.getChannelIdWithNamespace());
        }
        messages.put(messageType, message);
        AbstractMessage.getClassInstances().put(message.getClass(), message);

        ResourceLocation id = message.type();
        REGISTRATIONS.put(id, new Registration(messageType, message));
        if (CHANNELS.add(id)) {
            EventNetworkChannel channel = NetworkRegistry.ChannelBuilder
                .named(id)
                .networkProtocolVersion(() -> PROTOCOL_VERSION)
                .clientAcceptedVersions(version -> true)
                .serverAcceptedVersions(version -> true)
                .eventNetworkChannel();
            channel.addListener((NetworkEvent event) -> onPayload(id, event));
        }
        CommonAPI.LOGGER.info("[NetworkingMessages] Registered message: {}", messageType.getChannelIdWithNamespace());
    }

    private static void onPayload(ResourceLocation id, NetworkEvent event) {
        NetworkEvent.Context ctx = event.getSource().get();
        NetworkDirection direction = ctx.getDirection();
        if (direction != NetworkDirection.PLAY_TO_CLIENT && direction != NetworkDirection.PLAY_TO_SERVER) {
            return;
        }
        ctx.setPacketHandled(true);

        Registration registration = REGISTRATIONS.get(id);
        if (registration == null) {
            return;
        }
        boolean toServer = direction == NetworkDirection.PLAY_TO_SERVER;
        if (toServer && !registration.messageType().isServerListener()) {
            CommonAPI.LOGGER.warn("[NetworkingMessages] Ignoring client-to-server packet for non server-listener message {}", id);
            return;
        }

        // Decode right here (the buffer is released once this returns), handle on the game thread.
        AbstractMessage<?> payload;
        try {
            payload = MessageCodec.decode(registration.prototype(), event.getPayload());
        } catch (Exception e) {
            CommonAPI.LOGGER.error("[NetworkingMessages] Exception decoding message: {}", id, e);
            return;
        }
        ServerPlayer sender = ctx.getSender();
        ctx.enqueueWork(() -> {
            try {
                if (toServer) {
                    payload.handleOnServer(sender);
                } else {
                    payload.handleOnClient();
                }
            } catch (Exception e) {
                CommonAPI.LOGGER.error("[NetworkingMessages] Exception in handler for message: {}", id, e);
            }
        });
    }

    @Override
    public void unregister() {
        messages.values().forEach(message -> REGISTRATIONS.remove(message.type()));
        messages.clear();
        classTypes.clear();
        AbstractMessage.getClassInstances().clear();
    }

    public Map<MessageType, AbstractMessage<?>> getMessages() {
        return messages;
    }

    /** Resolves the registered prototype of {@code message} and stamps its channel id onto it. */
    private AbstractMessage<?> prototypeOf(AbstractMessage<?> message) {
        MessageType messageType = getMessageTypeByClass(message);
        if (messageType == null) {
            throw new RegistryMessageException("Message " + message.getMessageType().getChannelIdWithNamespace() + " not registered");
        }
        AbstractMessage<?> prototype = messages.get(messageType);
        message.updateProperties(messageType, prototype.type());
        return prototype;
    }

    private static FriendlyByteBuf serialize(AbstractMessage<?> prototype, AbstractMessage<?> message) {
        ByteBuf buf = MessageCodec.encode(prototype, message);
        return new FriendlyByteBuf(buf);
    }

    private static Packet<?> toClientPacket(ResourceLocation id, FriendlyByteBuf buf) {
        return NetworkDirection.PLAY_TO_CLIENT.buildPacket(Pair.of(buf, 0), id).getThis();
    }

    public void sendMessageToClient(AbstractMessage<?> message) {
        sendMessageToClient(null, message);
    }

    public void sendMessageToClient(ServerPlayer player, AbstractMessage<?> message) {
        Packet<?> packet = toClientPacket(message.type(), serialize(prototypeOf(message), message));
        if (player == null) {
            PacketDistributor.ALL.noArg().send(packet);
            return;
        }
        PacketDistributor.PLAYER.with(() -> player).send(packet);
    }

    public void sendMessageTrackerToClient(ServerPlayer player, AbstractMessage<?> message) {
        Packet<?> packet = toClientPacket(message.type(), serialize(prototypeOf(message), message));
        if (player == null) {
            PacketDistributor.ALL.noArg().send(packet);
            return;
        }
        PacketDistributor.TRACKING_ENTITY.with(() -> player).send(packet);
    }

    @Override
    public MessageType getMessageTypeByClass(AbstractMessage<?> message) {
        return classTypes.get(message.getClass());
    }

    public void sendMessageToServer(AbstractMessage<?> message) {
        MessageType messageType = getMessageTypeByClass(message);
        if (messageType == null) {
            throw new RegistryMessageException("Message " + message.getClass().getName() + " not registered");
        }
        if (!messageType.isServerListener()) {
            throw new RegistryMessageException("Message " + messageType.getChannelIdWithNamespace() + " is not a server listener");
        }
        FriendlyByteBuf buf = serialize(prototypeOf(message), message);
        Packet<?> packet = NetworkDirection.PLAY_TO_SERVER.buildPacket(Pair.of(buf, 0), message.type()).getThis();
        // Separate class so a dedicated server never loads the client-only Minecraft class.
        ClientSender.send(packet);
    }
}
