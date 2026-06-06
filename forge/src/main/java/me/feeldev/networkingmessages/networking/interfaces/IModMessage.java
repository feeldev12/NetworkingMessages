package me.feeldev.networkingmessages.networking.interfaces;

import io.netty.buffer.ByteBuf;
import me.feeldev.networkingmessages.networking.models.AbstractMessage;
import me.feeldev.networkingmessages.networking.models.MessageType;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;

public interface IModMessage<T extends AbstractMessage<T>> extends CustomPacketPayload, StreamCodec<ByteBuf, T> {

    MessageType getMessageType();

    @Override
    Type<T> type();

    default void handleOnConfigurationServer(ServerConfigurationPacketListenerImpl handler) {}
}
