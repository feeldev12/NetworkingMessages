package me.feeldev.networkingmessages.networking.serialization.serializers;

import me.feeldev.networkingmessages.networking.serialization.FriendlyByteBuf;

public interface ICustomSerializer<T> {
    void write(FriendlyByteBuf buf, T object);
    T read(FriendlyByteBuf buf);
}
