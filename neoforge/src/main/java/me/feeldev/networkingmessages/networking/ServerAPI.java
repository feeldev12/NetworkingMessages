package me.feeldev.networkingmessages.networking;

import me.feeldev.networkingmessages.networking.managers.MessagesManager;
import me.feeldev.networkingmessages.networking.managers.TypesManager;
import me.feeldev.networkingmessages.networking.models.AbstractMessage;
import me.feeldev.networkingmessages.networking.models.NetworkAPI;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ConfigurationTask;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.function.Consumer;

public class ServerAPI implements NetworkAPI<ServerPlayer, AbstractMessage<?>> {
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

    public void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(messagesManager.getNamespace());
        messagesManager.flush(registrar);
        messagesManager.flushConfig(registrar);
    }

    public void onRegisterConfigTasks(RegisterConfigurationTasksEvent event) {
        typesManager.getMessageTypes().values().stream()
            .filter(mt -> mt.isConfigurationPhase() && !mt.isServerListener())
            .forEach(mt -> {
                AbstractMessage<?> message = messagesManager.getMessages().get(mt);
                if (message != null) {
                    ConfigurationTask.Type taskType = new ConfigurationTask.Type(mt.getChannelIdWithNamespace());
                    event.register(new ICustomConfigurationTask() {
                        @Override
                        public void run(Consumer<CustomPacketPayload> sender) {
                            sender.accept(message);
                        }

                        @Override
                        public ConfigurationTask.Type type() {
                            return taskType;
                        }
                    });
                }
            });
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
