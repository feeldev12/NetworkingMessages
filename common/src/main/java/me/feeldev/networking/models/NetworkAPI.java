package me.feeldev.networking.models;

import me.feeldev.networking.managers.TypesManager;

public interface NetworkAPI<T> {
    TypesManager getTypesManager();

    IMessagesManager<T> getMessagesManager();

    boolean isServer();

    boolean isClient();

    boolean isCompressionEnabled();
}
