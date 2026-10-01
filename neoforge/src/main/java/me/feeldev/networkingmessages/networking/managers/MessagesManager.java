package me.feeldev.networkingmessages.networking.managers;

import me.feeldev.networkingmessages.networking.common.CommonAPI;
import me.feeldev.networkingmessages.networking.exceptions.RegistryMessageException;
import me.feeldev.networkingmessages.networking.models.AbstractMessage;
import me.feeldev.networkingmessages.networking.common.IMessagesManager;
import me.feeldev.networkingmessages.networking.common.MessageType;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessagesManager implements IMessagesManager<ServerPlayer, AbstractMessage<?>> {
    private static MessagesManager instance;
    /**
     * Every manager by namespace. Each mod that uses this library on the same game builds its own
     * {@code ServerAPI} (and so its own manager); a {@code ClientAPI} sends through the one for its
     * namespace instead of whichever was built last.
     */
    private static final Map<String, MessagesManager> BY_NAMESPACE = new ConcurrentHashMap<>();

    private final Map<MessageType, AbstractMessage<?>> messages = new ConcurrentHashMap<>();
    private final Map<Class<?>, MessageType> classTypes = new ConcurrentHashMap<>();
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

    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    public String getNamespace() {
        return namespace;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void registerMessage(MessageType messageType, @NotNull AbstractMessage message) {
        if (classTypes.putIfAbsent(message.getClass(), messageType) != null) {
            throw new RegistryMessageException("Message " + messageType.getChannelIdWithNamespace() + " already registered");
        }
        messages.put(messageType, message);
        AbstractMessage.getClassInstances().put(message.getClass(), message);
    }

    @SuppressWarnings("unchecked")
    public void flush(PayloadRegistrar registrar) {
        messages.forEach((messageType, message) -> {
            if (!messageType.isConfigurationPhase()) {
                registerPayload(registrar, messageType, (AbstractMessage) message);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public void flushConfig(PayloadRegistrar registrar) {
        messages.forEach((messageType, message) -> {
            if (messageType.isConfigurationPhase()) {
                registerConfigPayload(registrar, messageType, (AbstractMessage) message);
            }
        });
    }

    // NeoForge allows one registration per payload id: a message that travels both ways must be
    // registered once as bidirectional, with a handler per direction.
    private <T extends AbstractMessage<T>> void registerPayload(PayloadRegistrar registrar, MessageType messageType, T message) {
        IPayloadHandler<T> toClient = (payload, context) -> {
            try {
                payload.handleOnClient();
            } catch (Exception e) {
                CommonAPI.LOGGER.error("[NetworkingMessages] Client handler exception for {}: {}", messageType.getChannelIdWithNamespace(), e.getMessage());
                throw e;
            }
        };
        if (messageType.isServerListener()) {
            IPayloadHandler<T> toServer = (payload, context) -> {
                try {
                    payload.handleOnServer((ServerPlayer) context.player());
                } catch (Exception e) {
                    CommonAPI.LOGGER.error("[NetworkingMessages] Server handler exception for {}: {}", messageType.getChannelIdWithNamespace(), e.getMessage());
                    throw e;
                }
            };
            registrar.playBidirectional(message.type(), message, new DirectionalPayloadHandler<>(toClient, toServer));
        } else {
            registrar.playToClient(message.type(), message, toClient);
        }
        CommonAPI.LOGGER.info("[NetworkingMessages] Registered message: {}", messageType.getChannelIdWithNamespace());
    }

    private <T extends AbstractMessage<T>> void registerConfigPayload(PayloadRegistrar registrar, MessageType messageType, T message) {
        IPayloadHandler<T> toClient = (payload, context) -> {
            try {
                payload.handleOnClient();
            } catch (Exception e) {
                CommonAPI.LOGGER.error("[NetworkingMessages] Client handler exception for {}: {}", messageType.getChannelIdWithNamespace(), e.getMessage());
                throw e;
            }
        };
        if (messageType.isServerListener()) {
            IPayloadHandler<T> toServer = (payload, context) -> {
                try {
                    ServerConfigurationPacketListenerImpl handler =
                        (ServerConfigurationPacketListenerImpl) context.listener();
                    payload.handleOnConfigurationServer(handler);
                } catch (Exception e) {
                    CommonAPI.LOGGER.error("[NetworkingMessages] Server handler exception for {}: {}", messageType.getChannelIdWithNamespace(), e.getMessage());
                    throw e;
                }
            };
            registrar.configurationBidirectional(message.type(), message, new DirectionalPayloadHandler<>(toClient, toServer));
        } else {
            registrar.configurationToClient(message.type(), message, toClient);
        }
        CommonAPI.LOGGER.info("[NetworkingMessages] Registered configuration message: {}", messageType.getChannelIdWithNamespace());
    }

    @Override
    public void unregister() {
        messages.clear();
        classTypes.clear();
        AbstractMessage.getClassInstances().clear();
    }

    public Map<MessageType, AbstractMessage<?>> getMessages() {
        return messages;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void sendConfigurationMessageToClient(ServerConfigurationPacketListenerImpl handler, AbstractMessage<?> message) {
        if (!classTypes.containsKey(message.getClass())) {
            throw new RegistryMessageException("Message " + message.getMessageType().getChannelIdWithNamespace() + " not registered");
        }
        MessageType messageType = getMessageTypeByClass(message);
        AbstractMessage abstractMessage = messages.get(messageType);
        message.updateProperties(messageType, abstractMessage.type());

        handler.send(new ClientboundCustomPayloadPacket(message));
    }

    public void sendMessageToClient(AbstractMessage<?> message) {
        sendMessageToClient(null, message);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void sendMessageToClient(ServerPlayer player, AbstractMessage<?> message) {
        if (!classTypes.containsKey(message.getClass())) {
            throw new RegistryMessageException("Message " + message.getMessageType().getChannelIdWithNamespace() + " not registered");
        }
        MessageType messageType = getMessageTypeByClass(message);
        AbstractMessage abstractMessage = messages.get(messageType);
        message.updateProperties(messageType, abstractMessage.type());

        if (player == null) {
            server.getPlayerList().getPlayers().forEach(p -> PacketDistributor.sendToPlayer(p, message));
            return;
        }
        PacketDistributor.sendToPlayer(player, message);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void sendMessageTrackerToClient(ServerPlayer player, AbstractMessage<?> message) {
        MessageType messageType = getMessageTypeByClass(message);
        if (messageType == null) {
            throw new RegistryMessageException("Message " + message.getMessageType().getChannelIdWithNamespace() + " not registered");
        }
        AbstractMessage abstractMessage = messages.get(messageType);
        message.updateProperties(messageType, abstractMessage.type());

        if (player == null) {
            server.getPlayerList().getPlayers().forEach(p -> PacketDistributor.sendToPlayer(p, message));
            return;
        }
        PacketDistributor.sendToPlayersTrackingEntity(player, message);
    }

    @Override
    public MessageType getMessageTypeByClass(AbstractMessage<?> message) {
        return classTypes.get(message.getClass());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void sendMessageToServer(AbstractMessage<?> message) {
        MessageType messageType = getMessageTypeByClass(message);
        if (messageType == null) {
            throw new RegistryMessageException("Message " + message.getClass().getName() + " not registered");
        }
        if (!messageType.isServerListener()) {
            throw new RegistryMessageException("Message " + messageType.getChannelIdWithNamespace() + " is not a server listener");
        }
        AbstractMessage abstractMessage = messages.get(messageType);
        message.updateProperties(messageType, abstractMessage.type());
        PacketDistributor.sendToServer(message);
    }
}
