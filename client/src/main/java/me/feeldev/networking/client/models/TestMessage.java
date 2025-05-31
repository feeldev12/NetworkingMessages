package me.feeldev.networking.client.models;

import io.netty.buffer.ByteBuf;
import me.feeldev.networking.CommonAPI;
import me.feeldev.networking.models.Test;
import me.feeldev.networking.serialization.FriendlyByteBuf;
import me.feeldev.networking.models.MessageType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class TestMessage extends AbstractMessage<TestMessage> {
    private int testInt;
    private String testString;
    private Test test;

    public TestMessage(MessageType messageType) {
        super(messageType);
    }

    public TestMessage(int testInt, String testString, Test test) {
        this.testInt = testInt;
        this.testString = testString;
        this.test = test;
    }

    @Override
    public void handler(ClientPlayNetworking.Context context) {
        CommonAPI.LOGGER.info("Test Packet: {} - {} | {} - {}", testInt, testString, test.testInt(), test.testString());
    }

    @Override
    public TestMessage decode(ByteBuf buf) {
        FriendlyByteBuf friendlyByteBuf = new FriendlyByteBuf(buf);
        int testInt = friendlyByteBuf.readByte();
        String testString = friendlyByteBuf.readUtf();
        Test test = friendlyByteBuf.readCustomObject(Test.class);
        return new TestMessage(testInt, testString, test);
    }

    @Override
    public void encode(ByteBuf buf, TestMessage value) {
        FriendlyByteBuf friendlyByteBuf = new FriendlyByteBuf(buf);
        friendlyByteBuf.writeByte(value.testInt);
        friendlyByteBuf.writeUtf(value.testString);
        friendlyByteBuf.writeCustomObject(value.test);
    }

}
