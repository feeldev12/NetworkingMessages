package me.feeldev.networking.client;

import me.feeldev.networking.client.managers.MessagesManager;
import me.feeldev.networking.managers.TypesManager;

public class ClientAPI {
    private final TypesManager typesManager;
    private final MessagesManager messagesManager;

    public ClientAPI(String namespace) {
        this.typesManager = new TypesManager();
        this.messagesManager = new MessagesManager(namespace);
    }

    public TypesManager getTypesManager() {
        return typesManager;
    }

    public MessagesManager getMessagesManager() {
        return messagesManager;
    }
}
