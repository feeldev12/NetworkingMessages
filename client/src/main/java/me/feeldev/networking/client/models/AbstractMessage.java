package me.feeldev.networking.client.models;

import me.feeldev.networking.client.interfaces.IPluginMessage;
import me.feeldev.networking.models.MessageType;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public abstract class AbstractMessage<T extends AbstractMessage<T>> implements IPluginMessage<T> {
    protected MessageType messageType;
    protected String namespace;

    private CustomPayload.Id<T> id;

    public AbstractMessage(MessageType messageType) {
        this.messageType = messageType;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    @Override
    public Id<T> getId() {
        return this.id;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
        this.id = getId(namespace);
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public void setId(Id<T> id) {
        this.id = id;
    }

    public CustomPayload.Id<T> getId(String namespace) {
        return new CustomPayload.Id<>(Identifier.of(namespace, messageType.getChannelId()));
    }
}
