package me.feeldev.networkingmessages.networking.models;

import me.feeldev.networkingmessages.networking.managers.TypesManager;

public interface NetworkAPI<T> {
    TypesManager getTypesManager();

    IMessagesManager<T> getMessagesManager();

    boolean isServer();

    boolean isClient();

    boolean isCompressionEnabled();

    void setCompressionEnabled(boolean enabled);
}
