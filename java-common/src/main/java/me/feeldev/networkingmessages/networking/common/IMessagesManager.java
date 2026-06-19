package me.feeldev.networkingmessages.networking.common;

public interface IMessagesManager<P, T> {

    void registerMessage(MessageType messageType, T message);

    void unregister();

    MessageType getMessageTypeByClass(T message);

    default void sendMessageToClient(T message) {
        throw new UnsupportedOperationException("Not implemented");
    }

    default void sendMessageToClient(P player, T message) {
        throw new UnsupportedOperationException("Not implemented");
    }

    default void sendMessageTrackerToClient(P player, T message) {
        throw new UnsupportedOperationException("Not implemented");
    }

}
