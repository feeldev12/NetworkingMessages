package me.feeldev.networking.client.managers;

import me.feeldev.networking.CommonAPI;
import me.feeldev.networking.client.interfaces.IPluginMessage;
import me.feeldev.networking.client.models.AbstractMessage;
import me.feeldev.networking.exceptions.RegistryMessageException;
import me.feeldev.networking.models.MessageType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.packet.CustomPayload;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class MessagesManager {
    private final Map<MessageType, AbstractMessage<?>> messages;
    private final String namespace;

    public MessagesManager(String namespace) {
        this.messages = new HashMap<>();
        this.namespace = namespace;
    }

    public void registerMessage(MessageType messageType, @NotNull AbstractMessage message) {
        if(messages.containsKey(messageType)) {
            throw new RegistryMessageException("Message already registered");
        }

        messages.put(messageType, message);

        message.setNamespace(namespace);

        CustomPayload.Id<? extends AbstractMessage<?>> id = message.getId();
        PayloadTypeRegistry.playC2S().register(id, message);
        PayloadTypeRegistry.playS2C().register(id, message);
        ClientPlayNetworking.registerGlobalReceiver(id, IPluginMessage::handler);

        CommonAPI.LOGGER.info("Registered message: {}", messageType);
    }

    public Map<MessageType, AbstractMessage<?>> getMessages() {
        return messages;
    }

    public void sendMessageToServer(AbstractMessage<?> abstractMessage) {
        abstractMessage.setNamespace(namespace);
        ClientPlayNetworking.send(abstractMessage);
    }

//    public void sendSpawnLaserMessage() {
//        ClientPlayNetworking.send(new SpawnLaserMessage());
//    }
}
