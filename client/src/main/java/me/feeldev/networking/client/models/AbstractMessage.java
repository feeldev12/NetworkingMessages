package me.feeldev.networking.client.models;

import me.feeldev.networking.client.interfaces.IPluginMessage;
import me.feeldev.networking.client.managers.MessagesManager;
import me.feeldev.networking.models.MessageType;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public abstract class AbstractMessage<T extends AbstractMessage<T>> implements IPluginMessage<T> {
    protected final MessageType messageType;

    private final CustomPayload.Id<T> id;

    public AbstractMessage(MessageType messageType) {
        this.messageType = messageType;
        this.id = new CustomPayload.Id<>(Identifier.of(messageType.getNamespace(), messageType.getChannelId()));
    }

    public AbstractMessage() {
        this.messageType = MessagesManager.getClassTypes().get(this.getClass()).getMessageType();
        this.id = new CustomPayload.Id<>(Identifier.of(messageType.getNamespace(), messageType.getChannelId()));
    }

    public MessageType getMessageType() {
        return messageType;
    }

    @Override
    public Id<T> getId() {
        return id;
    }
}
