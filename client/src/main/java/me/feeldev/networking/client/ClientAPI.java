package me.feeldev.networking.client;

import me.feeldev.networking.client.managers.MessagesManager;
import me.feeldev.networking.client.models.AbstractMessage;
import me.feeldev.networking.managers.TypesManager;
import me.feeldev.networking.models.NetworkAPI;

public class ClientAPI implements NetworkAPI<AbstractMessage<?>> {
    private final TypesManager typesManager;
    private final MessagesManager messagesManager;
    private boolean compressionEnabled;

    public ClientAPI(String namespace) {
        this.typesManager = new TypesManager(namespace);
        this.messagesManager = new MessagesManager(namespace);
        this.compressionEnabled = false;
    }

    public TypesManager getTypesManager() {
        return typesManager;
    }

    public MessagesManager getMessagesManager() {
        return messagesManager;
    }

    public void setCompressionEnabled(boolean compressionEnabled) {
        this.compressionEnabled = compressionEnabled;
    }

    @Override
    public boolean isServer() {
        return false;
    }

    @Override
    public boolean isClient() {
        return true;
    }

    @Override
    public boolean isCompressionEnabled() {
        return compressionEnabled;
    }
}
