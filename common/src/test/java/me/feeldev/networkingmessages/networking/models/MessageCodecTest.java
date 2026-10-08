package me.feeldev.networkingmessages.networking.models;

import io.netty.buffer.ByteBuf;
import me.feeldev.networkingmessages.networking.common.MessageType;
import me.feeldev.networkingmessages.networking.serialization.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class MessageCodecTest {

    /** Same shape as an Animorph message: library FriendlyByteBuf over the raw ByteBuf. */
    static final class SampleMessage extends AbstractMessage<SampleMessage> {
        int number;
        String text;
        UUID id;

        SampleMessage(MessageType type) {
            super(type);
        }

        SampleMessage(int number, String text, UUID id) {
            this.number = number;
            this.text = text;
            this.id = id;
        }

        @Override
        public void encode(ByteBuf buf, SampleMessage value) {
            FriendlyByteBuf out = new FriendlyByteBuf(buf);
            out.disableCompression();
            out.writeVarInt(value.number);
            out.writeUtf(value.text);
            out.writeUUID(value.id);
        }

        @Override
        public SampleMessage decode(ByteBuf buf) {
            FriendlyByteBuf in = new FriendlyByteBuf(buf);
            in.disableCompression();
            return new SampleMessage(in.readVarInt(), in.readUtf(), in.readUUID());
        }
    }

    private static SampleMessage prototype() {
        MessageType type = new MessageType("sample", 1, true);
        type.setNamespace("test");
        return new SampleMessage(type);
    }

    @Test
    void channelIdIsNamespaceAndPath() {
        assertEquals("test:sample", prototype().type().toString());
    }

    @Test
    void roundTripKeepsEveryField() {
        SampleMessage proto = prototype();
        UUID uuid = UUID.fromString("00000000-0000-0001-0000-000000000002");
        SampleMessage sent = new SampleMessage(300, "héllo", uuid);

        SampleMessage received = (SampleMessage) MessageCodec.fromBytes(proto, MessageCodec.toBytes(proto, sent));

        assertEquals(300, received.number);
        assertEquals("héllo", received.text);
        assertEquals(uuid, received.id);
    }

    @Test
    void wireBytesAreTheRawEncodedPayload() {
        SampleMessage proto = prototype();
        byte[] bytes = MessageCodec.toBytes(proto, new SampleMessage(300, "hi", new UUID(1L, 2L)));

        byte[] expected = {
            (byte) 0xAC, 0x02,                     // varint 300
            0x02, 'h', 'i',                        // varint length + utf8
            0, 0, 0, 0, 0, 0, 0, 1,                // uuid most significant bits
            0, 0, 0, 0, 0, 0, 0, 2                 // uuid least significant bits
        };
        assertArrayEquals(expected, bytes);
    }

    @Test
    void decodeUsesThePrototypeInstance() {
        SampleMessage proto = prototype();
        AbstractMessage.getClassInstances().put(SampleMessage.class, proto);
        SampleMessage received = (SampleMessage) MessageCodec.fromBytes(proto, MessageCodec.toBytes(proto, new SampleMessage(1, "a", new UUID(3L, 4L))));
        // A message built by decode() (no-arg super) picks the registered channel up from the registry.
        assertSame(proto.type(), AbstractMessage.getClassInstances().get(SampleMessage.class).type());
        assertEquals(1, received.number);
        AbstractMessage.getClassInstances().remove(SampleMessage.class);
    }
}
