package me.feeldev.networking.serialization.serializers;

import me.feeldev.networking.models.Test;
import me.feeldev.networking.serialization.FriendlyByteBuf;

public class TestSerializer implements ICustomSerializer<Test> {

    @Override
    public void write(FriendlyByteBuf buf, Test object) {
        buf.writeByte(object.testInt());
        buf.writeUtf(object.testString());
    }

    @Override
    public Test read(FriendlyByteBuf buf) {
        int testInt = buf.readByte();
        String testString = buf.readUtf();
        return new Test(testInt, testString);
    }
}
