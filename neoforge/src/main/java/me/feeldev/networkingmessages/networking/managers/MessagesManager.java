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
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessagesManager implements IMessagesManager<ServerPlayer, AbstractMessage<?>> {
    private static MessagesManager instance;

    private final Map<MessageType, AbstractMessage<?>> messages = new ConcurrentHashMap<>();
    private final Map<Class<?>, MessageType> classTypes = new ConcurrentHashMap<>();
    private MinecraftServer server;
    private final String namespace;

    public MessagesManager(MinecraftServer server, String namespace) {
        this.server = server;
        this.namespace = namespace;
        instance = this;
    }

    public static MessagesManager getInstance() {
        return instance;
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

    private <T extends AbstractMessage<T>> void registerPayload(PayloadRegistrar registrar, MessageType messageType, T message) {
        registrar.playBidirectional(
            message.type(),
            message,
            new DirectionalPayloadHandler<>(
                (payload, context) -> {
                    try {
                        payload.handleOnClient();
                    } catch (Exception e) {
                        CommonAPI.LOGGER.error("[NetworkingMessages] Client handler exception for {}: {}", messageType.getChannelIdWithNamespace(), e.getMessage());
                        throw e;
                    }
                },
                (payload, context) -> {
                    try {
                        payload.handleOnServer((ServerPlayer) context.player());
                    } catch (Exception e) {
                        CommonAPI.LOGGER.error("[NetworkingMessages] Server handler exception for {}: {}", messageType.getChannelIdWithNamespace(), e.getMessage());
                        throw e;
                    }
                }
            )
        );
        CommonAPI.LOGGER.info("[NetworkingMessages] Registered message: {}", messageType.getChannelIdWithNamespace());
    }

    private <T extends AbstractMessage<T>> void registerConfigPayload(PayloadRegistrar registrar, MessageType messageType, T message) {
        registrar.configurationToClient(
            message.type(),
            message,
            (payload, context) -> {
                try {
                    payload.handleOnClient();
                } catch (Exception e) {
                    CommonAPI.LOGGER.error("[NetworkingMessages] Client handler exception for {}: {}", messageType.getChannelIdWithNamespace(), e.getMessage());
                    throw e;
                }
            }
        );
        if (messageType.isServerListener()) {
            registrar.configurationToServer(
                message.type(),
                message,
                (payload, context) -> {
                    try {
                        ServerConfigurationPacketListenerImpl handler =
                            (ServerConfigurationPacketListenerImpl) context.listener();
                        payload.handleOnConfigurationServer(handler);
                    } catch (Exception e) {
                        CommonAPI.LOGGER.error("[NetworkingMessages] Server handler exception for {}: {}", messageType.getChannelIdWithNamespace(), e.getMessage());
                        throw e;
                    }
                }
            );
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
}
