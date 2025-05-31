package me.feeldev.networking.client.interfaces;

import io.netty.buffer.ByteBuf;
import me.feeldev.networking.client.models.AbstractMessage;
import me.feeldev.networking.models.MessageType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public interface IPluginMessage<T extends AbstractMessage> extends CustomPayload, PacketCodec<ByteBuf, T> {

    MessageType getMessageType();
    @Override
    Id<T> getId();
    void handler(ClientPlayNetworking.Context context);
}
