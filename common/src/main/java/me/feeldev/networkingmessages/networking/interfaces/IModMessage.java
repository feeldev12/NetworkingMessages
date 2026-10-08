package me.feeldev.networkingmessages.networking.interfaces;

import io.netty.buffer.ByteBuf;
import me.feeldev.networkingmessages.networking.common.MessageType;
import me.feeldev.networkingmessages.networking.models.AbstractMessage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * A message that travels over a plain custom-payload channel. On Minecraft 1.20.1 there is no
 * {@code CustomPacketPayload}/{@code StreamCodec}: the wire format is the channel id
 * ({@link #type()}) plus the raw bytes written by {@link #encode(ByteBuf, AbstractMessage)}.
 */
public interface IModMessage<T extends AbstractMessage<T>> {

    MessageType getMessageType();

    /** The channel id this message is sent over ({@code namespace:channelId}). */
    ResourceLocation type();

    void encode(ByteBuf buf, T value);

    T decode(ByteBuf buf);

    default void handleOnServer(ServerPlayer sender) {}

    default void handleOnClient() {}
}
