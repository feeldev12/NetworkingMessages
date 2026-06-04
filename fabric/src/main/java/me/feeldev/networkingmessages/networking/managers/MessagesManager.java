package me.feeldev.networkingmessages.networking.managers;

import me.feeldev.networkingmessages.networking.models.AbstractMessage;
import me.feeldev.networkingmessages.networking.CommonAPI;
import me.feeldev.networkingmessages.networking.exceptions.RegistryMessageException;
import me.feeldev.networkingmessages.networking.models.IMessagesManager;
import me.feeldev.networkingmessages.networking.models.MessageType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class MessagesManager implements IMessagesManager<AbstractMessage<?>> {
    private final Map<MessageType, AbstractMessage> messages;
    private final Map<Class<?>, MessageType> classTypes;
    private static final Map<Class<?>, AbstractMessage<?>> classInstances = new HashMap<>();

    private final String namespace;
    private final MinecraftServer server;

    public MessagesManager(MinecraftServer server, String namespace) {
        this.server = server;
        this.messages = new HashMap<>();
        this.namespace = namespace;
        this.classTypes = new HashMap<>();
    }

    public static Map<Class<?>, AbstractMessage<?>> getClassInstances() {
        return classInstances;
    }

    @SuppressWarnings("unchecked")
    public void registerMessage(MessageType messageType, @NotNull AbstractMessage message) {
        if (classTypes.containsKey(message.getClass())) {
            throw new RegistryMessageException("Message " + messageType.getChannelIdWithNamespace() + " already registered");
        }

        messages.put(messageType, message);
        classTypes.put(message.getClass(), messageType);
        classInstances.put(message.getClass(), message);

        CustomPacketPayload.Type<? extends AbstractMessage<?>> id = message.type();
        PayloadTypeRegistry.playS2C().register(id, message);
        if (messageType.isServerListener()) {
            PayloadTypeRegistry.playC2S().register(id, message);
            ServerPlayNetworking.registerGlobalReceiver(id, (payload, context) -> {
                try {
                    payload.handleOnServer(context.player());
                } catch (Exception e) {
                    CommonAPI.LOGGER.error("[NetworkingMessages] Exception in handler for message: {}", messageType.getChannelIdWithNamespace(), e);
                    throw e;
                }
            });
        }
    }

    @Override
    public void unregister() {
        messages.forEach((messageType, abstractMessage) -> {
            if (messageType.isServerListener()) {
                ServerPlayNetworking.unregisterGlobalReceiver(abstractMessage.type().id());
            }
        });
        messages.clear();
        classTypes.clear();
        classInstances.clear();
    }

    public void sendMessageToClient(AbstractMessage<?> message) {
        sendMessageToClient(null, message);
    }

    @SuppressWarnings("unchecked")
    public void sendMessageToClient(ServerPlayer player, AbstractMessage<?> message) {
        if (!classTypes.containsKey(message.getClass())) {
            throw new RegistryMessageException("Message " + message.getMessageType().getChannelIdWithNamespace() + " not registered");
        }
        MessageType messageType = getMessageTypeByClass(message);
        if (messageType == null) {
            throw new RegistryMessageException("Message " + message.getMessageType().getChannelIdWithNamespace() + " not registered");
        }

        AbstractMessage abstractMessage = messages.get(messageType);
        message.updateProperties(messageType, abstractMessage.type());

        if (player == null) {
            server.getPlayerList().getPlayers().forEach(p -> ServerPlayNetworking.send(p, message));
            return;
        }
        ServerPlayNetworking.send(player, message);
    }

    @SuppressWarnings("unchecked")
    public void sendMessageTrackerToClient(ServerPlayer player, AbstractMessage<?> message) {
        MessageType messageType = getMessageTypeByClass(message);
        if (messageType == null) {
            throw new RegistryMessageException("Message " + message.getMessageType().getChannelIdWithNamespace() + " not registered");
        }

        AbstractMessage abstractMessage = messages.get(messageType);
        message.updateProperties(messageType, abstractMessage.type());

        if (player == null) {
            server.getPlayerList().getPlayers().forEach(p -> ServerPlayNetworking.send(p, message));
            return;
        }
        PlayerLookup.tracking(player).forEach(p -> ServerPlayNetworking.send(p, message));
    }

    @Override
    public MessageType getMessageTypeByClass(AbstractMessage<?> message) {
        return classTypes.get(message.getClass());
    }
}
