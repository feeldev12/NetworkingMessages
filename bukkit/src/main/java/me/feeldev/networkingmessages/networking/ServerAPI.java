package me.feeldev.networkingmessages.networking;

import me.feeldev.networkingmessages.networking.common.CommonAPI;
import me.feeldev.networkingmessages.networking.common.NetworkAPI;
import me.feeldev.networkingmessages.networking.common.TypesManager;
import me.feeldev.networkingmessages.networking.managers.MessagesManager;
import me.feeldev.networkingmessages.networking.models.AbstractMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ServerAPI implements NetworkAPI<Player, AbstractMessage<?>> {
    private final TypesManager typesManager;
    private final MessagesManager messagesManager;
    private boolean compressionEnabled;

    public ServerAPI(JavaPlugin javaPlugin, String namespace) {
        this.typesManager = new TypesManager(namespace);
        this.messagesManager = new MessagesManager(javaPlugin, this, namespace);
        this.compressionEnabled = false;
        CommonAPI.setNetworkAPI(this);
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
