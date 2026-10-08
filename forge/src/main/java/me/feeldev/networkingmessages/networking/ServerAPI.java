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

    public ServerAPI(String namespace) {
        this(null, namespace);
    }

    public ServerAPI(MinecraftServer server, String namespace) {
        this.typesManager = new TypesManager(namespace);
        this.messagesManager = new MessagesManager(server, namespace);
        this.compressionEnabled = false;
        CommonAPI.setNetworkAPI(this);
    }

    public void setServer(MinecraftServer server) {
        messagesManager.setServer(server);
    }

    /**
     * Kept for source compatibility with the NeoForge build. The channels this library registers on
     * Forge 1.20.1 already accept every version, including an absent channel, so a client can always
     * join a server that lacks them (Bukkit, vanilla): this is a no-op.
     */
    public void setOptional(boolean optional) {
    }

    @Override
    public TypesManager getTypesManager() {
        return typesManager;
    }

    @Override
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
