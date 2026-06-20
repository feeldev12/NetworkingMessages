package me.feeldev.networkingmessages.networking.common;

import me.feeldev.networkingmessages.networking.exceptions.RegistryMessageTypeException;

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

    private MessageType doRegister(String channelId, boolean serverListener, boolean configurationPhase) {
        if (messageTypes.containsKey(channelId)) {
            throw new RegistryMessageTypeException("That channelId already exist");
        }
        int packetId = messageTypes.size();
        MessageType messageType = new MessageType(channelId, packetId, serverListener, configurationPhase);
        messageType.setNamespace(namespace);
        messageTypes.put(channelId, messageType);
        return messageType;
    }

    public MessageType registerMessageType(String channelId, boolean serverListener) {
        return doRegister(channelId, serverListener, false);
    }

    public MessageType registerMessageType(String channelId) {
        return doRegister(channelId, false, false);
    }

    public MessageType registerConfigurationMessageType(String channelId, boolean serverListener) {
        return doRegister(channelId, serverListener, true);
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
