package me.feeldev.networking.client.interfaces;

import me.feeldev.networking.client.models.AbstractMessage;
import me.feeldev.networking.models.MessageType;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;

public interface IPluginMessage<T extends AbstractMessage> extends CustomPayload, FabricPacket {

    MessageType getMessageType();

    @Override
    PacketType<T> getType();

    void handler(ClientPlayerEntity player, PacketSender packetSender);

    T read(PacketByteBuf packetByteBuf);
}
