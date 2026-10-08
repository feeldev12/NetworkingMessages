package me.feeldev.networkingmessages.networking.managers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.feeldev.networkingmessages.networking.common.CommonAPI;
import me.feeldev.networkingmessages.networking.common.IMessagesManager;
import me.feeldev.networkingmessages.networking.common.MessageType;
import me.feeldev.networkingmessages.networking.exceptions.RegistryMessageException;
import me.feeldev.networkingmessages.networking.models.AbstractMessage;
import me.feeldev.networkingmessages.networking.models.MessageCodec;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessagesManager implements IMessagesManager<ServerPlayer, AbstractMessage<?>> {
    private final Map<MessageType, AbstractMessage> messages;
    private final Map<Class<?>, MessageType> classTypes;
    private final String namespace;
    private final MinecraftServer server;

    public MessagesManager(MinecraftServer server, String namespace) {
        this.server = server;
        this.messages = new ConcurrentHashMap<>();
        this.namespace = namespace;
        this.classTypes = new ConcurrentHashMap<>();
    }

    @SuppressWarnings("unchecked")
    public void registerMessage(MessageType messageType, @NotNull AbstractMessage message) {
        if (classTypes.putIfAbsent(message.getClass(), messageType) != null) {
            throw new RegistryMessageException("Message " + messageType.getChannelIdWithNamespace() + " already registered");
        }
        if (messageType.isConfigurationPhase()) {
            CommonAPI.LOGGER.warn("[NetworkingMessages] {} asks for the configuration phase, which does not exist on 1.20.1; it is sent in the play phase", messageType.getChannelIdWithNamespace());
        }

        messages.put(messageType, message);
        AbstractMessage.getClassInstances().put(message.getClass(), message);

        if (messageType.isServerListener()) {
            ResourceLocation id = message.type();
            // Replaces a receiver left behind by an earlier server in the same JVM (integrated server restarts).
            ServerPlayNetworking.unregisterGlobalReceiver(id);
            ServerPlayNetworking.registerGlobalReceiver(id, (srv, player, handler, buf, responseSender) -> {
                // Decode on the network thread (the buffer is released after this returns), handle on the server thread.
                AbstractMessage<?> payload;
                try {
                    payload = MessageCodec.decode(message, buf);
                } catch (Exception e) {
                    CommonAPI.LOGGER.error("[NetworkingMessages] Exception decoding message: {}", messageType.getChannelIdWithNamespace(), e);
                    return;
                }
                srv.execute(() -> {
                    try {
                        payload.handleOnServer(player);
                    } catch (Exception e) {
                        CommonAPI.LOGGER.error("[NetworkingMessages] Exception in handler for message: {}", messageType.getChannelIdWithNamespace(), e);
                    }
                });
            });
        }
    }

    @Override
    public void unregister() {
        messages.forEach((messageType, abstractMessage) -> {
            if (messageType.isServerListener()) {
                ServerPlayNetworking.unregisterGlobalReceiver(abstractMessage.type());
            }
        });
        messages.clear();
        classTypes.clear();
        AbstractMessage.getClassInstances().clear();
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

    private static byte[] serialize(AbstractMessage<?> prototype, AbstractMessage<?> message) {
        ByteBuf buf = MessageCodec.encode(prototype, message);
        try {
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return bytes;
        } finally {
            buf.release();
        }
    }

    // One buffer per recipient: the connection releases the buffer it is handed.
    private static void sendTo(ServerPlayer player, ResourceLocation id, byte[] bytes) {
        ServerPlayNetworking.send(player, id, new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes)));
    }

    public void sendMessageToClient(AbstractMessage<?> message) {
        sendMessageToClient(null, message);
    }

    public void sendMessageToClient(ServerPlayer player, AbstractMessage<?> message) {
        AbstractMessage<?> prototype = prototypeOf(message);
        byte[] bytes = serialize(prototype, message);
        if (player == null) {
            server.getPlayerList().getPlayers().forEach(p -> sendTo(p, message.type(), bytes));
            return;
        }
        sendTo(player, message.type(), bytes);
    }

    public void sendMessageTrackerToClient(ServerPlayer player, AbstractMessage<?> message) {
        AbstractMessage<?> prototype = prototypeOf(message);
        byte[] bytes = serialize(prototype, message);
        if (player == null) {
            server.getPlayerList().getPlayers().forEach(p -> sendTo(p, message.type(), bytes));
            return;
        }
        PlayerLookup.tracking(player).forEach(p -> sendTo(p, message.type(), bytes));
    }

    public Map<MessageType, AbstractMessage> getMessages() {
        return messages;
    }

    public MinecraftServer getServer() {
        return server;
    }

    @Override
    public MessageType getMessageTypeByClass(AbstractMessage<?> message) {
        return classTypes.get(message.getClass());
    }
}
