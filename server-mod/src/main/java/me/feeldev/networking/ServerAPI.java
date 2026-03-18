package me.feeldev.networking;

import me.feeldev.networking.managers.MessagesManager;
import me.feeldev.networking.managers.TypesManager;
import me.feeldev.networking.models.AbstractMessage;
import me.feeldev.networking.models.NetworkAPI;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class ServerAPI implements NetworkAPI<AbstractMessage<?>> {
    private TypesManager typesManager;
    private MessagesManager messagesManager;
    private boolean compressionEnabled;

    public ServerAPI(String namespace) {
        ServerLifecycleEvents.SERVER_STARTING.register(minecraftServer -> {
            this.typesManager = new TypesManager(namespace);
            this.messagesManager = new MessagesManager(minecraftServer, namespace);
            this.compressionEnabled = false;
            CommonAPI.setNetworkAPI(this);
        });
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
