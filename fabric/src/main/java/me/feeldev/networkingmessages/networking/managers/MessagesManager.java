package me.feeldev.networkingmessages.networking.managers;

import me.feeldev.networkingmessages.networking.models.AbstractMessage;
import me.feeldev.networkingmessages.networking.common.CommonAPI;
import me.feeldev.networkingmessages.networking.exceptions.RegistryMessageException;
import me.feeldev.networkingmessages.networking.common.IMessagesManager;
import me.feeldev.networkingmessages.networking.common.MessageType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public class MessagesManager implements IMessagesManager<ServerPlayer, AbstractMessage<?>> {
    private static final Set<ResourceLocation> globallyRegisteredS2C = new HashSet<>();
    private static final Set<ResourceLocation> globallyRegisteredC2S = new HashSet<>();

    private final Map<MessageType, AbstractMessage> messages;
    private final Map<Class<?>, MessageType> classTypes;
    private final String namespace;
    private final MinecraftServer server;

    public MessagesManager(MinecraftServer server, String namespace) {
        this.server = server;
        this.messages = new HashMap<>();
        this.namespace = namespace;
        this.classTypes = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    public void registerMessage(MessageType messageType, @NotNull AbstractMessage message) {
        if (classTypes.containsKey(message.getClass())) {
            throw new RegistryMessageException("Message " + messageType.getChannelIdWithNamespace() + " already registered");
        }

        messages.put(messageType, message);
        classTypes.put(message.getClass(), messageType);
        AbstractMessage.getClassInstances().put(message.getClass(), message);

        CustomPacketPayload.Type<? extends AbstractMessage<?>> id = message.type();

        ResourceLocation payloadId = id.id();
        if (messageType.isConfigurationPhase()) {
            if (globallyRegisteredS2C.add(payloadId)) {
                PayloadTypeRegistry.configurationS2C().register(id, message);
            }
            if (messageType.isServerListener()) {
                if (globallyRegisteredC2S.add(payloadId)) {
                    PayloadTypeRegistry.configurationC2S().register(id, message);
                }
                ServerConfigurationNetworking.registerGlobalReceiver(id, (payload, context) -> {
                    try {
                        server.execute(() -> payload.handleOnConfigurationServer(context.networkHandler()));
                    } catch (Exception e) {
                        CommonAPI.LOGGER.error("[NetworkingMessages] Exception in handler for message: {}", messageType.getChannelIdWithNamespace(), e);
                        throw e;
                    }
                });
            }
        } else {
            if (globallyRegisteredS2C.add(payloadId)) {
                PayloadTypeRegistry.playS2C().register(id, message);
            }
            if (messageType.isServerListener()) {
                if (globallyRegisteredC2S.add(payloadId)) {
                    PayloadTypeRegistry.playC2S().register(id, message);
                }
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
    }

    @Override
    public void unregister() {
        messages.forEach((messageType, abstractMessage) -> {
            if (messageType.isServerListener()) {
                if (messageType.isConfigurationPhase()) {
                    ServerConfigurationNetworking.unregisterGlobalReceiver(abstractMessage.type().id());
                } else {
                    ServerPlayNetworking.unregisterGlobalReceiver(abstractMessage.type().id());
                }
            }
        });
        messages.clear();
        classTypes.clear();
        globallyRegisteredS2C.clear();
        globallyRegisteredC2S.clear();
        AbstractMessage.getClassInstances().clear();
    }

    @SuppressWarnings("unchecked")
    public void sendConfigurationMessageToClient(ServerConfigurationPacketListenerImpl handler, AbstractMessage<?> message) {
        if (!classTypes.containsKey(message.getClass())) {
            throw new RegistryMessageException("Message " + message.getMessageType().getChannelIdWithNamespace() + " not registered");
        }
        MessageType messageType = getMessageTypeByClass(message);
        if (messageType == null) {
            throw new RegistryMessageException("Message " + message.getMessageType().getChannelIdWithNamespace() + " not registered");
        }

        AbstractMessage abstractMessage = messages.get(messageType);
        message.updateProperties(messageType, abstractMessage.type());

        ServerConfigurationNetworking.send(handler, message);
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
