package me.feeldev.networkingmessages.networking;

import me.feeldev.networkingmessages.networking.managers.MessagesManager;
import me.feeldev.networkingmessages.networking.managers.TypesManager;
import me.feeldev.networkingmessages.networking.models.AbstractMessage;
import me.feeldev.networkingmessages.networking.models.NetworkAPI;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.network.GatherLoginConfigurationTasksEvent;
import net.minecraftforge.network.config.SimpleConfigurationTask;

public class ServerAPI implements NetworkAPI<ServerPlayer, AbstractMessage<?>> {
    private final TypesManager typesManager;
    private final MessagesManager messagesManager;
    private boolean compressionEnabled;

    /**
     * Use this constructor during mod initialization (e.g. FMLCommonSetupEvent).
     * The SimpleChannel is created immediately; call registerMessage() right after.
     * Call {@link #setServer(MinecraftServer)} when the server is available.
     */
    public ServerAPI(String namespace) {
        this.typesManager = new TypesManager(namespace);
        this.messagesManager = new MessagesManager(null, namespace);
        this.compressionEnabled = false;
        CommonAPI.setNetworkAPI(this);
        MinecraftForge.EVENT_BUS.addListener(this::onGatherLoginConfigurationTasks);
    }

    public ServerAPI(MinecraftServer server, String namespace) {
        this.typesManager = new TypesManager(namespace);
        this.messagesManager = new MessagesManager(server, namespace);
        this.compressionEnabled = false;
        CommonAPI.setNetworkAPI(this);
        MinecraftForge.EVENT_BUS.addListener(this::onGatherLoginConfigurationTasks);
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

    private void onGatherLoginConfigurationTasks(GatherLoginConfigurationTasksEvent event) {
        typesManager.getMessageTypes().values().stream()
            .filter(mt -> mt.isConfigurationPhase() && !mt.isServerListener())
            .forEach(mt -> {
                AbstractMessage<?> message = messagesManager.getMessages().get(mt);
                if (message != null) {
                    ConfigurationTask.Type taskType = new ConfigurationTask.Type(mt.getChannelIdWithNamespace());
                    event.addTask(new SimpleConfigurationTask(taskType, ctx ->
                        ctx.send(new ClientboundCustomPayloadPacket(message))
                    ));
                }
            });
    }
}
