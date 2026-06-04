package me.feeldev.networkingmessages.networking.client.models;

import me.feeldev.networkingmessages.networking.client.interfaces.IPluginMessage;
import me.feeldev.networkingmessages.networking.client.managers.MessagesManager;
import me.feeldev.networkingmessages.networking.models.MessageType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public abstract class AbstractMessage<T extends AbstractMessage<T>> implements IPluginMessage<T> {
    protected final MessageType messageType;
    private final CustomPacketPayload.Type<T> id;

    public AbstractMessage(MessageType messageType) {
        this.messageType = messageType;
        this.id = new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath(messageType.getNamespace(), messageType.getChannelId())
        );
    }

    public AbstractMessage() {
        this.messageType = MessagesManager.getClassTypes().get(this.getClass()).getMessageType();
        this.id = new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath(messageType.getNamespace(), messageType.getChannelId())
        );
    }

    public MessageType getMessageType() {
        return messageType;
    }

    @Override
    public CustomPacketPayload.Type<T> type() {
        return id;
    }
}
