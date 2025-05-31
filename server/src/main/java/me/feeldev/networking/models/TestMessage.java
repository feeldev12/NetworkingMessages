package me.feeldev.networking.models;

import io.netty.buffer.Unpooled;
import me.feeldev.networking.CommonAPI;
import me.feeldev.networking.serialization.FriendlyByteBuf;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public class TestMessage extends AbstractMessage<TestMessage> implements PluginMessageListener {
    private int testInt;
    private String testString;
    private Test test;

    public TestMessage(JavaPlugin plugin, MessageType type) {
        super(plugin, type);
    }

    public TestMessage(int testInt, String testString, Test test) {
        this.testInt = testInt;
        this.testString = testString;
        this.test = test;
    }

    @Override
    public byte[] sendMessage(TestMessage message) {
//        ByteArrayDataOutput output = ByteStreams.newDataOutput();
//        output.writeByte(message.testInt);
//        output.writeUTF(message.testString);
//        return output.toByteArray();
        FriendlyByteBuf friendlyByteBuf = new FriendlyByteBuf();
        friendlyByteBuf.writeByte(message.testInt);
        friendlyByteBuf.writeUtf(message.testString);
        friendlyByteBuf.writeCustomObject(message.test);
        return friendlyByteBuf.readOnlyNecessaryBytes();
    }

    @Override
    public byte[] deactivateMessage() {
        return new byte[0];
    }

    @Override
    public void onPluginMessageReceived(@NotNull String s, @NotNull Player player, @NotNull byte[] bytes) {
        FriendlyByteBuf friendlyByteBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
        int testInt = friendlyByteBuf.readByte();
        String testString = friendlyByteBuf.readUtf();
        Test test = friendlyByteBuf.readCustomObject(Test.class);
        CommonAPI.LOGGER.info("Server listener Test Packet: {} - {} | {} - {}", testInt, testString, test.testInt(), test.testString());
    }
}
