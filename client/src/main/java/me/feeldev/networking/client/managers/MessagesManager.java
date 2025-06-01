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
    private final Map<Class<?>, AbstractMessage<?>> classTypes;

    private final String namespace;

    public MessagesManager(String namespace) {
        this.messages = new HashMap<>();
        this.classTypes = new HashMap<>();
        this.namespace = namespace;
    }

    public void registerMessage(MessageType messageType, @NotNull AbstractMessage message) {
        if(classTypes.containsKey(message.getClass())) {
            throw new RegistryMessageException("Message already registered");
        }
        message.setNamespace(namespace);

        messages.put(messageType, message);
        classTypes.put(message.getClass(), message);

        CustomPayload.Id<? extends AbstractMessage<?>> id = message.getId();
        PayloadTypeRegistry.playC2S().register(id, message);
        PayloadTypeRegistry.playS2C().register(id, message);
        ClientPlayNetworking.registerGlobalReceiver(id, (abstractMessage, context) -> {

            message.handler(context);
        });

        CommonAPI.LOGGER.info("Registered message: {}", id.id().toString());
    }

    public Map<MessageType, AbstractMessage<?>> getMessages() {
        return messages;
    }

    public void sendMessageToServer(AbstractMessage<?> abstractMessage) {
        if(!classTypes.containsKey(abstractMessage.getClass())) {
            throw new RegistryMessageException("Message " + abstractMessage.getClass().getSimpleName() + " not registered");
        }

        AbstractMessage message = classTypes.get(abstractMessage.getClass());
        abstractMessage.setId(message.getId());

        ClientPlayNetworking.send(abstractMessage);
    }

//    public void sendSpawnLaserMessage() {
//        ClientPlayNetworking.send(new SpawnLaserMessage());
//    }
}
