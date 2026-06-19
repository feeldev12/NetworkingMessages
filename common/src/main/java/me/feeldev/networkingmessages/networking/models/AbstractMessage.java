package me.feeldev.networkingmessages.networking.models;

import me.feeldev.networkingmessages.networking.common.MessageType;
import me.feeldev.networkingmessages.networking.interfaces.IModMessage;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractMessage<T extends AbstractMessage<T>> implements IModMessage<T> {

    private static final Map<Class<?>, AbstractMessage<?>> classInstances = new HashMap<>();

    public static Map<Class<?>, AbstractMessage<?>> getClassInstances() {
        return classInstances;
    }

    protected MessageType messageType;
    private CustomPacketPayload.Type<T> id;

    public AbstractMessage(MessageType messageType) {
        this.messageType = messageType;
        this.id = new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath(messageType.getNamespace(), messageType.getChannelId())
        );
    }

    @SuppressWarnings("unchecked")
    public AbstractMessage() {
        AbstractMessage<?> registered = classInstances.get(this.getClass());
        if (registered != null) {
            this.messageType = registered.messageType;
            this.id = (CustomPacketPayload.Type<T>) registered.id;
        }
    }

    @Override
    public MessageType getMessageType() {
        return messageType;
    }

    @Override
    public CustomPacketPayload.Type<T> type() {
        return id;
    }

    public void updateProperties(MessageType messageType, CustomPacketPayload.Type<T> id) {
        this.messageType = messageType;
        this.id = id;
    }
}
