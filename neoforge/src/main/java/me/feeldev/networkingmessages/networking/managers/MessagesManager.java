package me.feeldev.networkingmessages.networking.managers;

import me.feeldev.networkingmessages.networking.CommonAPI;
import me.feeldev.networkingmessages.networking.exceptions.RegistryMessageException;
import me.feeldev.networkingmessages.networking.models.AbstractMessage;
import me.feeldev.networkingmessages.networking.models.IMessagesManager;
import me.feeldev.networkingmessages.networking.models.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class MessagesManager implements IMessagesManager<AbstractMessage<?>> {
    private final Map<MessageType, AbstractMessage<?>> messages = new HashMap<>();
    private final Map<Class<?>, MessageType> classTypes = new HashMap<>();
    private static final Map<Class<?>, AbstractMessage<?>> classInstances = new HashMap<>();

    private MinecraftServer server;
    private final String namespace;

    public MessagesManager(MinecraftServer server, String namespace) {
        this.server = server;
        this.namespace = namespace;
    }

    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    public static Map<Class<?>, AbstractMessage<?>> getClassInstances() {
        return classInstances;
    }

    public String getNamespace() {
        return namespace;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void registerMessage(MessageType messageType, @NotNull AbstractMessage message) {
        if (classTypes.containsKey(message.getClass())) {
            throw new RegistryMessageException("Message " + messageType.getChannelIdWithNamespace() + " already registered");
        }
        messages.put(messageType, message);
        classTypes.put(message.getClass(), messageType);
        classInstances.put(message.getClass(), message);
    }

    @SuppressWarnings("unchecked")
    public void flush(PayloadRegistrar registrar) {
        messages.forEach((messageType, message) ->
            registerPayload(registrar, messageType, (AbstractMessage) message)
        );
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

    @Override
    public void unregister() {
        messages.clear();
        classTypes.clear();
        classInstances.clear();
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
        player.serverLevel().getChunkSource().chunkMap
            .getPlayers(player.chunkPosition(), false)
            .forEach(p -> PacketDistributor.sendToPlayer(p, message));
    }

    @Override
    public MessageType getMessageTypeByClass(AbstractMessage<?> message) {
        return classTypes.get(message.getClass());
    }
}
