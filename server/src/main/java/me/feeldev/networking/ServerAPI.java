package me.feeldev.networking;

import me.feeldev.networking.managers.MessagesManager;
import me.feeldev.networking.managers.TypesManager;
import me.feeldev.networking.models.MessageType;
import me.feeldev.networking.models.TestMessage;
import org.bukkit.plugin.java.JavaPlugin;

public class ServerAPI {
    private final TypesManager typesManager;
    private final MessagesManager messagesManager;

    public ServerAPI(JavaPlugin javaPlugin, String namespace) {
        this.typesManager = new TypesManager();
        this.messagesManager = new MessagesManager(javaPlugin, this, namespace);
    }

    private void registerTest(JavaPlugin javaPlugin) {
        MessageType messageType = typesManager.registerMessageType("test");
        messagesManager.registerMessage(messageType, new TestMessage(javaPlugin, messageType));
        messagesManager.sendMessage(new TestMessage(1, "a"));
    }

    public TypesManager getTypesManager() {
        return typesManager;
    }

    public MessagesManager getMessagesManager() {
        return messagesManager;
    }
}
