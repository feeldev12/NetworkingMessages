package me.feeldev.networking.client.models;

import me.feeldev.networking.client.interfaces.IPluginMessage;
import me.feeldev.networking.models.MessageType;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public abstract class AbstractMessage<T extends AbstractMessage<T>> implements IPluginMessage<T> {
    private MessageType messageType;
    private String namespace;

    public AbstractMessage(MessageType messageType) {
        this.messageType = messageType;
    }

    public AbstractMessage() {

    }

    public MessageType getMessageType() {
        return messageType;
    }

    @Override
    public Id<T> getId() {
        return getId(namespace == null ? "minecraft" : namespace);
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public CustomPayload.Id<T> getId(String namespace) {
        return new CustomPayload.Id<>(Identifier.of(namespace, messageType.getChannelId()));
    }
}
