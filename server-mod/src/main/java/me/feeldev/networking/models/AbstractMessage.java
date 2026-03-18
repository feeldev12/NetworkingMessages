package me.feeldev.networking.models;

import me.feeldev.networking.interfaces.IModMessage;
import me.feeldev.networking.managers.MessagesManager;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public abstract class AbstractMessage<T extends AbstractMessage<T>> implements IModMessage<T> {
    protected MessageType messageType;

    private CustomPayload.Id<T> id;

    public AbstractMessage(MessageType messageType) {
        this.messageType = messageType;
        this.id = new CustomPayload.Id<>(Identifier.of(messageType.getNamespace(), messageType.getChannelId()));
    }

    public AbstractMessage() {
    }

    public MessageType getMessageType() {
        return messageType;
    }

    @Override
    public Id<T> getId() {
        return id;
    }
}
