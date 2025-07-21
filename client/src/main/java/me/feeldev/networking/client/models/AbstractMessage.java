package me.feeldev.networking.client.models;

import me.feeldev.networking.client.interfaces.IPluginMessage;
import me.feeldev.networking.client.managers.MessagesManager;
import me.feeldev.networking.models.MessageType;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public abstract class AbstractMessage<T extends AbstractMessage<T>> implements IPluginMessage<T> {
    protected final MessageType messageType;

    private final PacketType<T> type;

    public AbstractMessage(MessageType messageType) {
        this.messageType = messageType;
        this.type = PacketType.create(Identifier.of(messageType.getNamespace(), messageType.getChannelId()), this::read);
    }

    public AbstractMessage() {
        this.messageType = MessagesManager.getClassTypes().get(this.getClass()).getMessageType();
        this.type = PacketType.create(Identifier.of(messageType.getNamespace(), messageType.getChannelId()), this::read);
    }

    public MessageType getMessageType() {
        return messageType;
    }

    @Override
    public PacketType<T> getType() {
        return type;
    }
}
