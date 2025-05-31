package me.feeldev.networking.client;

import me.feeldev.networking.client.managers.MessagesManager;
import me.feeldev.networking.client.models.TestMessage;
import me.feeldev.networking.managers.TypesManager;
import me.feeldev.networking.models.MessageType;
import me.feeldev.networking.models.Test;

public class ClientAPI {
    private final TypesManager typesManager;
    private final MessagesManager messagesManager;

    public ClientAPI(String namespace) {
        this.typesManager = new TypesManager();
        this.messagesManager = new MessagesManager(namespace);
    }

    private void registerTest() {
        MessageType messageType = typesManager.registerMessageType("test");
        messagesManager.registerMessage(messageType, new TestMessage(messageType));
        messagesManager.sendMessageToServer(new TestMessage(1, "a", new Test(2, "b")));
    }

    public TypesManager getTypesManager() {
        return typesManager;
    }

    public MessagesManager getMessagesManager() {
        return messagesManager;
    }
}
