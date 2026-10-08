package me.feeldev.networkingmessages.networking.client.managers;

import io.netty.buffer.ByteBuf;
import me.feeldev.networkingmessages.networking.common.CommonAPI;
import me.feeldev.networkingmessages.networking.common.IMessagesManager;
import me.feeldev.networkingmessages.networking.common.MessageType;
import me.feeldev.networkingmessages.networking.exceptions.RegistryMessageException;
import me.feeldev.networkingmessages.networking.models.AbstractMessage;
import me.feeldev.networkingmessages.networking.models.MessageCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public class MessagesManager implements IMessagesManager<ServerPlayer, AbstractMessage<?>> {
    private final Map<MessageType, AbstractMessage<?>> messages;
    private final Map<Class<?>, MessageType> classTypes;

    private final String namespace;

    public MessagesManager(String namespace) {
        this.messages = new ConcurrentHashMap<>();
        this.classTypes = new ConcurrentHashMap<>();
        this.namespace = namespace;
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

        ClientPlayNetworking.unregisterGlobalReceiver(message.type());
        ClientPlayNetworking.registerGlobalReceiver(message.type(), (client, handler, buf, responseSender) -> {
            // Decode on the network thread (the buffer is released after this returns), handle on the render thread.
            AbstractMessage<?> payload;
            try {
                payload = MessageCodec.decode(message, buf);
            } catch (Exception e) {
                CommonAPI.LOGGER.error("[NetworkingMessages] Exception decoding message: {}", messageType.getChannelIdWithNamespace(), e);
                return;
            }
            client.execute(() -> {
                try {
                    payload.handleOnClient();
                } catch (Exception e) {
                    CommonAPI.LOGGER.error("[NetworkingMessages] Exception in handler for message: {}", messageType.getChannelIdWithNamespace(), e);
                }
            });
        });

        CommonAPI.LOGGER.info("Registered message: {}", messageType.getChannelIdWithNamespace());
    }

    public Map<MessageType, AbstractMessage<?>> getMessages() {
        return messages;
    }

    public void sendMessageToServer(AbstractMessage<?> abstractMessage) {
        MessageType messageType = classTypes.get(abstractMessage.getClass());
        if (messageType == null) {
            throw new RegistryMessageException("Message " + abstractMessage.getMessageType().getChannelIdWithNamespace() + " not registered");
        }
        if (!messageType.isServerListener()) {
            throw new RegistryMessageException("Message " + messageType.getChannelIdWithNamespace() + " is not a server listener");
        }
        AbstractMessage<?> prototype = messages.get(messageType);
        abstractMessage.updateProperties(messageType, prototype.type());
        ByteBuf bytes = MessageCodec.encode(prototype, abstractMessage);
        ClientPlayNetworking.send(abstractMessage.type(), new FriendlyByteBuf(bytes));
    }

    public Map<Class<?>, MessageType> getClassTypes() {
        return classTypes;
    }

    @Override
    public void unregister() {
        messages.forEach((messageType, message) -> ClientPlayNetworking.unregisterGlobalReceiver(message.type()));
        messages.clear();
        classTypes.clear();
    }

    @Override
    public MessageType getMessageTypeByClass(AbstractMessage<?> message) {
        return classTypes.get(message.getClass());
    }
}
