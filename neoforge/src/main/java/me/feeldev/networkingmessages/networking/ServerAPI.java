package me.feeldev.networkingmessages.networking;

import me.feeldev.networkingmessages.networking.common.CommonAPI;
import me.feeldev.networkingmessages.networking.common.NetworkAPI;
import me.feeldev.networkingmessages.networking.common.TypesManager;
import me.feeldev.networkingmessages.networking.managers.MessagesManager;
import me.feeldev.networkingmessages.networking.models.AbstractMessage;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ConfigurationTask;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.function.Consumer;

public class ServerAPI implements NetworkAPI<ServerPlayer, AbstractMessage<?>> {
    private final TypesManager typesManager;
    private final MessagesManager messagesManager;
    private boolean compressionEnabled;

    /**
     * Use during mod initialization (e.g. inside your {@code @Mod} constructor). This does
     * NOT register payloads by itself: call {@link #onRegisterPayloads(RegisterPayloadHandlersEvent)}
     * and {@link #onRegisterConfigTasks(RegisterConfigurationTasksEvent)} from your own mod
     * event bus listeners, or use {@link #ServerAPI(String, IEventBus)} to have that done for you.
     * Call {@link #setServer(MinecraftServer)} when the server is available.
     */
    public ServerAPI(String namespace) {
        this.typesManager = new TypesManager(namespace);
        this.messagesManager = new MessagesManager(null, namespace);
        this.compressionEnabled = false;
        CommonAPI.setNetworkAPI(this);
    }

    /**
     * Same as {@link #ServerAPI(String)}, but also registers this instance's payload and
     * configuration-task handlers on {@code modEventBus}, so no manual event wiring is needed.
     */
    public ServerAPI(String namespace, IEventBus modEventBus) {
        this(namespace);
        modEventBus.addListener(this::onRegisterPayloads);
        modEventBus.addListener(this::onRegisterConfigTasks);
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
