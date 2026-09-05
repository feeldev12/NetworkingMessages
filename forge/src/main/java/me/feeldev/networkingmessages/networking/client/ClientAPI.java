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
    private boolean compressionEnabled;

    public ClientAPI(String namespace) {
        this.typesManager = new TypesManager(namespace);
        this.compressionEnabled = false;
        CommonAPI.setNetworkAPI(this);
    }

    public void sendMessageToServer(AbstractMessage<?> message) {
        MessagesManager.getInstance().sendMessageToServer(message);
    }

    @Override
    public TypesManager getTypesManager() {
        return typesManager;
    }

    @Override
    public MessagesManager getMessagesManager() {
        return MessagesManager.getInstance();
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
