package me.feeldev.networkingmessages.networking;

import me.feeldev.networkingmessages.networking.managers.MessagesManager;
import me.feeldev.networkingmessages.networking.managers.TypesManager;
import me.feeldev.networkingmessages.networking.models.AbstractMessage;
import me.feeldev.networkingmessages.networking.models.NetworkAPI;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public class ServerAPI implements NetworkAPI<AbstractMessage<?>> {
    private final TypesManager typesManager;
    private final MessagesManager messagesManager;
    private boolean compressionEnabled;

    /**
     * Use during mod initialization (e.g. inside your {@code @Mod} constructor).
     * The library auto-registers payloads with NeoForge — no manual event call needed.
     * Call {@link #setServer(MinecraftServer)} when the server is available.
     */
    public ServerAPI(String namespace) {
        this.typesManager = new TypesManager(namespace);
        this.messagesManager = new MessagesManager(null, namespace);
        this.compressionEnabled = false;
        CommonAPI.setNetworkAPI(this);
    }

    public ServerAPI(MinecraftServer server, String namespace) {
        this.typesManager = new TypesManager(namespace);
        this.messagesManager = new MessagesManager(server, namespace);
        this.compressionEnabled = false;
        CommonAPI.setNetworkAPI(this);
    }

    /**
     * NeoForge only — call this from your {@code @SubscribeEvent} on {@link RegisterPayloadHandlersEvent}.
     * Not needed in Fabric or Forge.
     */
    public void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        messagesManager.flush(event.registrar(messagesManager.getNamespace()));
    }

    public void setServer(MinecraftServer server) {
        messagesManager.setServer(server);
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
