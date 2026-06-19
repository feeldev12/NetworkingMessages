package me.feeldev.networkingmessages.networking.client;

import me.feeldev.networkingmessages.networking.common.CommonAPI;
import me.feeldev.networkingmessages.networking.client.managers.MessagesManager;
import me.feeldev.networkingmessages.networking.common.TypesManager;
import me.feeldev.networkingmessages.networking.models.AbstractMessage;
import me.feeldev.networkingmessages.networking.common.NetworkAPI;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.level.ServerPlayer;

@Environment(EnvType.CLIENT)
public class ClientAPI implements NetworkAPI<ServerPlayer, AbstractMessage<?>> {
    private final TypesManager typesManager;
    private final MessagesManager messagesManager;
    private boolean compressionEnabled;

    public ClientAPI(String namespace) {
        this.typesManager = new TypesManager(namespace);
        this.messagesManager = new MessagesManager(namespace);
        this.compressionEnabled = false;
        CommonAPI.setNetworkAPI(this);
    }

    public void sendMessageToServer(AbstractMessage<?> message) {
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
