package me.feeldev.networking.managers;

import me.feeldev.networking.exceptions.RegistryMessageTypeException;
import me.feeldev.networking.models.MessageType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TypesManager {
    private final Map<String, MessageType> messageTypes;
    private final String namespace;

    public TypesManager(String namespace) {
        this.namespace = namespace;
        this.messageTypes = new HashMap<>();
    }

    public MessageType registerMessageType(String channelId, boolean serverListener) {
        int packetId = messageTypes.size();
        if(messageTypes.containsKey(channelId)) {
            throw new RegistryMessageTypeException("That channelId already exist");
        }
        MessageType messageType = new MessageType(channelId, packetId, serverListener);
        messageType.setNamespace(namespace);
        messageTypes.put(channelId, messageType);

        return messageType;
    }

    public MessageType registerMessageType(String channelId) {
        return registerMessageType(channelId, false);
    }

    public void unregisterMessageType(String channelId) {
        messageTypes.remove(channelId);
    }

    public MessageType getMessageType(String channelId) {
        return messageTypes.get(channelId);
    }

    public Optional<MessageType> getMessageTypeByChannel(String channelId) {
        return Optional.ofNullable(messageTypes.get(channelId));
    }

    public Optional<MessageType> getMessageTypeByPacketId(int packetId) {
        return messageTypes.values().stream().filter(type -> type.getPacketId() == packetId).findFirst();
    }

    public Map<String, MessageType> getMessageTypes() {
        return messageTypes;
    }
}
