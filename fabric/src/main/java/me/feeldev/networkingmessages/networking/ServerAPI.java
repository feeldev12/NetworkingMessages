package me.feeldev.networkingmessages.networking;

import me.feeldev.networkingmessages.networking.models.AbstractMessage;
import me.feeldev.networkingmessages.networking.managers.MessagesManager;
import me.feeldev.networkingmessages.networking.managers.TypesManager;
import me.feeldev.networkingmessages.networking.models.MessageType;
import me.feeldev.networkingmessages.networking.models.NetworkAPI;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConfigurationTask;

import java.util.function.Consumer;

public class ServerAPI implements NetworkAPI<AbstractMessage<?>> {
    private final TypesManager typesManager;
    private final MessagesManager messagesManager;
    private boolean compressionEnabled;

    public ServerAPI(MinecraftServer server, String namespace) {
        this.typesManager = new TypesManager(namespace);
        this.messagesManager = new MessagesManager(server, namespace);
        this.compressionEnabled = false;
        CommonAPI.setNetworkAPI(this);
        ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server1) -> {
            for (MessageType messageType : typesManager.getMessageTypes().values()) {
                if (!messageType.isConfigurationPhase() || messageType.isServerListener()) continue;
                AbstractMessage<?> message = messagesManager.getMessages().get(messageType);
                if (message == null) continue;
                if (!ServerConfigurationNetworking.canSend(handler, message.type())) continue;
                handler.addTask(new LibraryConfigTask(
                        new ConfigurationTask.Type(messageType.getChannelIdWithNamespace()),
                        message
                ));
            }
        });
    }

    private record LibraryConfigTask(ConfigurationTask.Type type, AbstractMessage<?> message)
            implements ConfigurationTask {
        @Override
        public void start(Consumer<Packet<?>> sender) {
            sender.accept(ServerConfigurationNetworking.createS2CPacket(message));
        }
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
