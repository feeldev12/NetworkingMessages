package me.feeldev.networking;

import me.feeldev.networking.managers.MessagesManager;
import me.feeldev.networking.managers.TypesManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ServerAPI {
    private final TypesManager typesManager;
    private final MessagesManager messagesManager;

    public ServerAPI(JavaPlugin javaPlugin, String namespace) {
        this.typesManager = new TypesManager();
        this.messagesManager = new MessagesManager(javaPlugin, this, namespace);
    }

    public TypesManager getTypesManager() {
        return typesManager;
    }

    public MessagesManager getMessagesManager() {
        return messagesManager;
    }
}
