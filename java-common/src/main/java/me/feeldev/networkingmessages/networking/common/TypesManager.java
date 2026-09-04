package me.feeldev.networkingmessages.networking.common;

import me.feeldev.networkingmessages.networking.exceptions.RegistryMessageTypeException;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TypesManager {
    private final Map<String, MessageType> messageTypes;
    private final AtomicInteger nextPacketId = new AtomicInteger();
    private final String namespace;

    public TypesManager(String namespace) {
        this.namespace = namespace;
        this.messageTypes = new ConcurrentHashMap<>();
    }

    private MessageType doRegister(String channelId, boolean serverListener, boolean configurationPhase) {
        MessageType messageType = new MessageType(channelId, nextPacketId.getAndIncrement(), serverListener, configurationPhase);
        messageType.setNamespace(namespace);
        if (messageTypes.putIfAbsent(channelId, messageType) != null) {
            throw new RegistryMessageTypeException("That channelId already exist");
        }
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
