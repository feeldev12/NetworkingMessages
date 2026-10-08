package me.feeldev.networkingmessages.networking.models;

import me.feeldev.networkingmessages.networking.common.MessageType;
import me.feeldev.networkingmessages.networking.interfaces.IModMessage;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractMessage<T extends AbstractMessage<T>> implements IModMessage<T> {

    private static final Map<Class<?>, AbstractMessage<?>> classInstances = new ConcurrentHashMap<>();

    public static Map<Class<?>, AbstractMessage<?>> getClassInstances() {
        return classInstances;
    }

    protected MessageType messageType;
    private ResourceLocation id;

    public AbstractMessage(MessageType messageType) {
        this.messageType = messageType;
        this.id = new ResourceLocation(messageType.getNamespace(), messageType.getChannelId());
    }

    public AbstractMessage() {
        AbstractMessage<?> registered = classInstances.get(this.getClass());
        if (registered != null) {
            this.messageType = registered.messageType;
            this.id = registered.id;
        }
    }

    @Override
    public MessageType getMessageType() {
        return messageType;
    }

    @Override
    public ResourceLocation type() {
        return id;
    }

    public void updateProperties(MessageType messageType, ResourceLocation id) {
        this.messageType = messageType;
        this.id = id;
    }
}
