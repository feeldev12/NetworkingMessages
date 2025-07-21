package me.feeldev.networking.client.managers;

import me.feeldev.networking.CommonAPI;
import me.feeldev.networking.client.models.AbstractMessage;
import me.feeldev.networking.exceptions.RegistryMessageException;
import me.feeldev.networking.models.MessageType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class MessagesManager {
    private final Map<MessageType, AbstractMessage<?>> messages;
    private static final Map<Class<?>, AbstractMessage<?>> classTypes = new HashMap<>();

    private final String namespace;

    public MessagesManager(String namespace) {
        this.messages = new HashMap<>();
        this.namespace = namespace;
    }

    public void registerMessage(MessageType messageType, @NotNull AbstractMessage message) {
        if(classTypes.containsKey(message.getClass())) {
            throw new RegistryMessageException("Message " + messageType.getChannelIdWithNamespace() + " already registered");
        }

        messages.put(messageType, message);
        classTypes.put(message.getClass(), message);

        PacketType<? extends AbstractMessage<?>> id = message.getType();
        ClientPlayNetworking.registerGlobalReceiver(id, (abstractMessage, clientPlayerEntity, packetSender) -> {
            message.handler(clientPlayerEntity, packetSender);
        });

        CommonAPI.LOGGER.info("Registered message: {}", messageType.getChannelIdWithNamespace());
    }

    public Map<MessageType, AbstractMessage<?>> getMessages() {
        return messages;
    }

    public void sendMessageToServer(AbstractMessage<?> abstractMessage) {
        if(!classTypes.containsKey(abstractMessage.getClass())) {
            throw new RegistryMessageException("Message " + abstractMessage.getMessageType().getChannelIdWithNamespace() + " not registered");
        }

        if(!abstractMessage.getMessageType().isServerListener()) {
            throw new RegistryMessageException("Message " + abstractMessage.getMessageType().getChannelIdWithNamespace() + " is not a server listener");
        }

        ClientPlayNetworking.send(abstractMessage);
    }

    public static Map<Class<?>, AbstractMessage<?>> getClassTypes() {
        return classTypes;
    }

    //    public void sendSpawnLaserMessage() {
//        ClientPlayNetworking.send(new SpawnLaserMessage());
//    }
}
