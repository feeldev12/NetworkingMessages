package me.feeldev.networkingmessages.networking;

import me.feeldev.networkingmessages.networking.common.CommonAPI;
import me.feeldev.networkingmessages.networking.common.NetworkAPI;
import me.feeldev.networkingmessages.networking.common.TypesManager;
import me.feeldev.networkingmessages.networking.managers.MessagesManager;
import me.feeldev.networkingmessages.networking.models.AbstractMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class ServerAPI implements NetworkAPI<ServerPlayer, AbstractMessage<?>> {
    private final TypesManager typesManager;
    private final MessagesManager messagesManager;
    private boolean compressionEnabled;

    public ServerAPI(MinecraftServer server, String namespace) {
        this.typesManager = new TypesManager(namespace);
        this.messagesManager = new MessagesManager(server, namespace);
        this.compressionEnabled = false;
        CommonAPI.setNetworkAPI(this);
    }

    public TypesManager getTypesManager() {
        return typesManager;
    }

    public MessagesManager getMessagesManager() {
        return messagesManager;
    }

    public MinecraftServer getServer() {
        return messagesManager.getServer();
    }

    /**
     * Kept for source compatibility with the NeoForge build. Minecraft 1.20.1 has no payload
     * registry that could make a client refuse to join a server without the channel, so there is
     * nothing to relax here: this is a no-op.
     */
    public void setOptional(boolean optional) {
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
