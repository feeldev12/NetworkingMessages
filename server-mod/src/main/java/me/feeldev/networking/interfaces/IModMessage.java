package me.feeldev.networking.interfaces;

import io.netty.buffer.ByteBuf;
import me.feeldev.networking.models.AbstractMessage;
import me.feeldev.networking.models.MessageType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public interface IModMessage<T extends AbstractMessage<T>> extends CustomPayload, PacketCodec<ByteBuf, T> {

    MessageType getMessageType();
    @Override
    Id<T> getId();
    void handler(ServerPlayNetworking.Context context);

}
