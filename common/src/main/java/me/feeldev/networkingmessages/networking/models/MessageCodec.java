package me.feeldev.networkingmessages.networking.models;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Loader-independent wire helpers. The payload of every message is exactly the bytes its
 * {@link AbstractMessage#encode} writes, sent over the channel named by {@link AbstractMessage#type()};
 * Fabric, Forge and the Bukkit plugin-message side all carry those same bytes.
 */
public final class MessageCodec {
    private MessageCodec() {}

    /** Serializes {@code message} using the encoder of the registered {@code prototype}. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static ByteBuf encode(AbstractMessage<?> prototype, AbstractMessage<?> message) {
        ByteBuf buf = Unpooled.buffer();
        try {
            ((AbstractMessage) prototype).encode(buf, message);
        } catch (RuntimeException | Error e) {
            buf.release();
            throw e;
        }
        return buf;
    }

    /** Deserializes a message from {@code buf} using the decoder of the registered {@code prototype}. */
    public static AbstractMessage<?> decode(AbstractMessage<?> prototype, ByteBuf buf) {
        return prototype.decode(buf);
    }

    /** Serializes {@code message} into a byte array (convenience for tests and non-Netty transports). */
    public static byte[] toBytes(AbstractMessage<?> prototype, AbstractMessage<?> message) {
        ByteBuf buf = encode(prototype, message);
        try {
            byte[] out = new byte[buf.readableBytes()];
            buf.readBytes(out);
            return out;
        } finally {
            buf.release();
        }
    }

    public static AbstractMessage<?> fromBytes(AbstractMessage<?> prototype, byte[] data) {
        ByteBuf buf = Unpooled.wrappedBuffer(data);
        try {
            return decode(prototype, buf);
        } finally {
            buf.release();
        }
    }
}
