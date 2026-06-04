package me.feeldev.networkingmessages.networking.client.interfaces;

import io.netty.buffer.ByteBuf;
import me.feeldev.networkingmessages.networking.client.models.AbstractMessage;
import me.feeldev.networkingmessages.networking.models.MessageType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

@Environment(EnvType.CLIENT)
public interface IPluginMessage<T extends AbstractMessage<T>> extends CustomPacketPayload, StreamCodec<ByteBuf, T> {

    MessageType getMessageType();

    @Override
    Type<T> type();

    default void handleOnClient() {}
}
