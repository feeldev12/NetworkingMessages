package me.feeldev.networkingmessages.networking.models;

import me.feeldev.networkingmessages.networking.interfaces.IModMessage;
import me.feeldev.networkingmessages.networking.managers.MessagesManager;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;

public abstract class AbstractMessage<T extends AbstractMessage<T>> implements IModMessage<T> {
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
        AbstractMessage<?> registered = MessagesManager.getClassInstances().get(this.getClass());
        if (registered != null) {
            this.messageType = registered.messageType;
            this.id = (CustomPacketPayload.Type<T>) registered.id;
        }
    }

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
