package me.feeldev.networking;

import me.feeldev.networking.managers.MessagesManager;
import me.feeldev.networking.managers.TypesManager;
import me.feeldev.networking.models.AbstractMessage;
import me.feeldev.networking.models.NetworkAPI;
import org.bukkit.plugin.java.JavaPlugin;

public class ServerAPI implements NetworkAPI<AbstractMessage<?>> {
    private final TypesManager typesManager;
    private final MessagesManager messagesManager;
    private boolean compressionEnabled;

    public ServerAPI(JavaPlugin javaPlugin, String namespace) {
        this.typesManager = new TypesManager(namespace);
        this.messagesManager = new MessagesManager(javaPlugin, this, namespace);
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
        return true;
    }

    @Override
    public boolean isClient() {
        return false;
    }

    @Override
    public boolean isCompressionEnabled() {
        return compressionEnabled;
    }
}
