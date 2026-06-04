package me.feeldev.networkingmessages.networking.client.managers;

import me.feeldev.networkingmessages.networking.client.models.AbstractMessage;
import me.feeldev.networkingmessages.networking.CommonAPI;
import me.feeldev.networkingmessages.networking.exceptions.RegistryMessageException;
import me.feeldev.networkingmessages.networking.models.IMessagesManager;
import me.feeldev.networkingmessages.networking.models.MessageType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class MessagesManager implements IMessagesManager<AbstractMessage<?>> {
    private final Map<MessageType, AbstractMessage<?>> messages;
    private static final Map<Class<?>, AbstractMessage<?>> classTypes = new HashMap<>();

    private final String namespace;

    public MessagesManager(String namespace) {
        this.messages = new HashMap<>();
        this.namespace = namespace;
    }

    @SuppressWarnings("unchecked")
    public void registerMessage(MessageType messageType, @NotNull AbstractMessage message) {
        if (classTypes.containsKey(message.getClass())) {
            throw new RegistryMessageException("Message " + messageType.getChannelIdWithNamespace() + " already registered");
        }

        messages.put(messageType, message);
        classTypes.put(message.getClass(), message);

        CustomPacketPayload.Type<? extends AbstractMessage<?>> id = message.type();
        PayloadTypeRegistry.playC2S().register(id, message);
        PayloadTypeRegistry.playS2C().register(id, message);
        ClientPlayNetworking.registerGlobalReceiver(id, (payload, context) -> {
            try {
                payload.handleOnClient();
            } catch (Exception e) {
                CommonAPI.LOGGER.error("[NetworkingMessages] Exception in handler for message: {}", messageType.getChannelIdWithNamespace(), e);
                throw e;
            }
        });

        CommonAPI.LOGGER.info("Registered message: {}", messageType.getChannelIdWithNamespace());
    }

    public Map<MessageType, AbstractMessage<?>> getMessages() {
        return messages;
    }

    public void sendMessageToServer(AbstractMessage<?> abstractMessage) {
        if (!classTypes.containsKey(abstractMessage.getClass())) {
            throw new RegistryMessageException("Message " + abstractMessage.getMessageType().getChannelIdWithNamespace() + " not registered");
        }
        if (!abstractMessage.getMessageType().isServerListener()) {
            throw new RegistryMessageException("Message " + abstractMessage.getMessageType().getChannelIdWithNamespace() + " is not a server listener");
        }
        ClientPlayNetworking.send(abstractMessage);
    }

    public static Map<Class<?>, AbstractMessage<?>> getClassTypes() {
        return classTypes;
    }

    @Override
    public void unregister() {
    }

    @Override
    public MessageType getMessageTypeByClass(AbstractMessage<?> message) {
        return null;
    }
}
