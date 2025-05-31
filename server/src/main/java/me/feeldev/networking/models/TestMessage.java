package me.feeldev.networking.models;

import org.bukkit.plugin.java.JavaPlugin;

public class TestMessage extends AbstractMessage<TestMessage> {
    private int testInt;
    private String testString;

    public TestMessage(JavaPlugin plugin, MessageType type) {
        super(plugin, type);
    }

    public TestMessage(int testInt, String testString) {
        this.testInt = testInt;
        this.testString = testString;
    }

    @Override
    public byte[] sendMessage(TestMessage message) {
        return new byte[0];
    }

    @Override
    public byte[] deactivateMessage() {
        return new byte[0];
    }
}
