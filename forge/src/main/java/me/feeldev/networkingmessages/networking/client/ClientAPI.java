package me.feeldev.networkingmessages.networking.client;

import me.feeldev.networkingmessages.networking.common.CommonAPI;
import me.feeldev.networkingmessages.networking.managers.MessagesManager;
import me.feeldev.networkingmessages.networking.common.TypesManager;
import me.feeldev.networkingmessages.networking.models.AbstractMessage;
import me.feeldev.networkingmessages.networking.common.NetworkAPI;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientAPI implements NetworkAPI<ServerPlayer, AbstractMessage<?>> {
    private final TypesManager typesManager;
    private final String namespace;
    private boolean compressionEnabled;

    /**
     * Sends client→server messages through the {@link MessagesManager} of this namespace, which
     * the mod's {@code ServerAPI} creates (on this loader the {@code ServerAPI} also registers the
     * messages, on both distributions).
     */
    public ClientAPI(String namespace) {
        this.typesManager = new TypesManager(namespace);
        this.namespace = namespace;
        this.compressionEnabled = false;
        CommonAPI.setNetworkAPI(this);
    }

    public void sendMessageToServer(AbstractMessage<?> message) {
        getMessagesManager().sendMessageToServer(message);
    }

    @Override
    public TypesManager getTypesManager() {
        return typesManager;
    }

    @Override
    public MessagesManager getMessagesManager() {
        MessagesManager manager = MessagesManager.forNamespace(namespace);
        if (manager == null) {
            throw new IllegalStateException("No MessagesManager for namespace '" + namespace + "': create the ServerAPI for it first");
        }
        return manager;
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
