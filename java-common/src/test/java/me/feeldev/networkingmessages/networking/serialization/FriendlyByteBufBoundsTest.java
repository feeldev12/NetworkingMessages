package me.feeldev.networkingmessages.networking.serialization;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/** Lengths and compressed data come from the other side: reading them must not allocate unbounded memory. */
class FriendlyByteBufBoundsTest {

    private static FriendlyByteBuf wrap(ByteBuf netty) {
        return new FriendlyByteBuf(netty);
    }

    private static byte[] gzip(byte[] raw) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(out)) {
            gz.write(raw);
        }
        return out.toByteArray();
    }

    /** A gzip stream of {@code size} zero bytes, written in chunks (a few KB on the wire). */
    private static byte[] zeroBomb(long size) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] chunk = new byte[1 << 20];
        try (GZIPOutputStream gz = new GZIPOutputStream(out)) {
            for (long written = 0; written < size; written += chunk.length) gz.write(chunk);
        }
        return out.toByteArray();
    }

    @Test
    void stringLengthLargerThanTheDataIsRefusedBeforeAllocating() {
        ByteBuf netty = Unpooled.buffer();
        FriendlyByteBuf writer = wrap(netty);
        writer.writeVarInt(Integer.MAX_VALUE); // claims 2 GB, sends nothing
        DecoderException e = assertThrows(DecoderException.class, () -> wrap(netty).readUtf());
        assertTrue(e.getMessage().contains("2147483647"), e.getMessage());
    }

    @Test
    void byteArrayLengthLargerThanTheDataIsRefused() {
        ByteBuf netty = Unpooled.buffer();
        wrap(netty).writeVarInt(1_000_000);
        netty.writeBytes(new byte[10]);
        assertThrows(DecoderException.class, () -> wrap(netty).readByteArray(Integer.MAX_VALUE));
    }

    @Test
    void stringsStillRoundTripPlainAndCompressed() {
        ByteBuf netty = Unpooled.buffer();
        FriendlyByteBuf writer = wrap(netty);
        writer.writeUtf("hola ñandú");
        writer.writeUtf("x".repeat(100_000), true);
        FriendlyByteBuf reader = wrap(netty);
        assertEquals("hola ñandú", reader.readUtf());
        assertEquals("x".repeat(100_000), reader.readUtf(true));
    }

    @Test
    void gzipPayloadIsStillDecompressedWhenWithinTheLimit() throws IOException {
        ByteBuf netty = Unpooled.buffer();
        wrap(netty).writeUtf("payload");
        byte[] plain = new byte[netty.readableBytes()];
        netty.readBytes(plain);
        assertEquals("payload", wrap(Unpooled.wrappedBuffer(gzip(plain))).readUtf());
    }

    @Test
    void gzipPayloadThatInflatesPastTheLimitIsRefused() throws IOException {
        byte[] bomb = zeroBomb(FriendlyByteBuf.MAX_DECOMPRESSED_BYTES + (1 << 20));
        assertTrue(bomb.length < 64 * 1024, "small on the wire: " + bomb.length);
        assertThrows(DecoderException.class, () -> wrap(Unpooled.wrappedBuffer(bomb)));
    }

    @Test
    void compressedStringThatInflatesPastTheLimitIsRefused() throws IOException {
        byte[] bomb = zeroBomb(FriendlyByteBuf.MAX_DECOMPRESSED_BYTES + (1 << 20));
        ByteBuf netty = Unpooled.buffer();
        netty.writeByte(0); // not a gzip header: the buffer itself is not decompressed
        FriendlyByteBuf writer = wrap(netty);
        writer.writeVarInt(bomb.length);
        netty.writeBytes(bomb);
        FriendlyByteBuf reader = wrap(netty);
        reader.readByte();
        assertThrows(DecoderException.class, () -> reader.readUtf(true));
    }
}
