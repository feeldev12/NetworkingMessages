package me.feeldev.networkingmessages.networking.client;

import me.feeldev.networkingmessages.networking.client.models.AbstractMessage;
import me.feeldev.networkingmessages.networking.CommonAPI;
import me.feeldev.networkingmessages.networking.client.managers.MessagesManager;
import me.feeldev.networkingmessages.networking.managers.TypesManager;
import me.feeldev.networkingmessages.networking.models.NetworkAPI;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ClientAPI implements NetworkAPI<AbstractMessage<?>> {
    private final TypesManager typesManager;
    private final MessagesManager messagesManager;
    private boolean compressionEnabled;

    public ClientAPI(String namespace) {
        this.typesManager = new TypesManager(namespace);
        this.messagesManager = new MessagesManager(namespace);
        this.compressionEnabled = false;
        CommonAPI.setNetworkAPI(this);
    }

    public void sendMessageToServer(me.feeldev.networkingmessages.networking.client.models.AbstractMessage<?> message) {
        messagesManager.sendMessageToServer(message);
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
