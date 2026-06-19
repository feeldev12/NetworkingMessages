package me.feeldev.networkingmessages.networking.models;

import me.feeldev.networkingmessages.networking.managers.TypesManager;

public interface NetworkAPI<P, T> {
    TypesManager getTypesManager();

    IMessagesManager<P, T> getMessagesManager();

    boolean isServer();

    boolean isClient();

    boolean isCompressionEnabled();

    void setCompressionEnabled(boolean enabled);

    default void sendMessageToServer(T message) {
        throw new UnsupportedOperationException("Unsupported operation");
    }
}
