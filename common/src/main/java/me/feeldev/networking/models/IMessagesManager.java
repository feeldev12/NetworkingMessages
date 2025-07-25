package me.feeldev.networking.models;

public interface IMessagesManager<T> {

    void registerMessage(MessageType messageType, T message);

    void unregister();

    MessageType getMessageTypeByClass(T message);
}