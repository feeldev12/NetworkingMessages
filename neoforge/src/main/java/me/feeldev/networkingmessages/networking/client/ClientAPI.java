package me.feeldev.networkingmessages.networking.client;

import me.feeldev.networkingmessages.networking.CommonAPI;
import me.feeldev.networkingmessages.networking.managers.MessagesManager;
import me.feeldev.networkingmessages.networking.managers.TypesManager;
import me.feeldev.networkingmessages.networking.models.AbstractMessage;
import me.feeldev.networkingmessages.networking.models.NetworkAPI;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public class ClientAPI implements NetworkAPI<AbstractMessage<?>> {
    private final TypesManager typesManager;
    private boolean compressionEnabled;

    public ClientAPI(String namespace) {
        this.typesManager = new TypesManager(namespace);
        this.compressionEnabled = false;
        CommonAPI.setNetworkAPI(this);
    }

    /**
     * Sends a message from client to server (C2S).
     * The message type must have been registered with serverListener=true.
     */
    public void sendMessageToServer(AbstractMessage<?> message) {
        PacketDistributor.sendToServer(message);
    }

    @Override
    public TypesManager getTypesManager() {
        return typesManager;
    }

    @Override
    public MessagesManager getMessagesManager() {
        return null;
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
