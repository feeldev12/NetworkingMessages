package me.feeldev.networking.managers;

import me.feeldev.networking.CommonAPI;
import me.feeldev.networking.exceptions.MessageNullException;
import me.feeldev.networking.exceptions.RegistryMessageException;
import me.feeldev.networking.interfaces.IModMessage;
import me.feeldev.networking.models.AbstractMessage;
import me.feeldev.networking.models.IMessagesManager;
import me.feeldev.networking.models.MessageType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.impl.networking.PayloadTypeRegistryImpl;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.TypeFilter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class MessagesManager implements IMessagesManager<AbstractMessage<?>> {
    private final Map<MessageType, AbstractMessage> messages;
    private final Map<Class<?>, MessageType> classTypes;

    private final String namespace;
    private final MinecraftServer server;

    public MessagesManager(MinecraftServer server, String namespace) {
        this.server = server;
        this.messages = new HashMap<>();
        this.namespace = namespace;
        this.classTypes = new HashMap<>();
    }

    public void registerMessage(MessageType messageType, @NotNull AbstractMessage message) {
        if(classTypes.containsKey(message.getClass())) {
            throw new RegistryMessageException("Message " + messageType.getChannelIdWithNamespace() + " already registered");
        }

        messages.put(messageType, message);
        classTypes.put(message.getClass(), messageType);

        CustomPayload.Id<? extends AbstractMessage<?>> id = message.getId();
        PayloadTypeRegistry.playS2C().register(id, message);
        if(messageType.isServerListener()) {
            PayloadTypeRegistry.playC2S().register(id, message);
            ServerPlayNetworking.registerGlobalReceiver(id, (payload, context) -> {
                try {
                    payload.handler(context);
                } catch (Exception e) {
                    CommonAPI.LOGGER.error("[NetworkingMessages] Exception in handler for message: {}", messageType.getChannelIdWithNamespace(), e);
                    throw e;
                }
            });
        }
    }

    @Override
    public void unregister() {
        messages.forEach((messageType, abstractMessage) -> {
            CustomPayload.Id<? extends AbstractMessage<?>> id = abstractMessage.getId();
//            if(messageType.isServerListener()) {
//                PayloadTypeRegistry.playC2S().unregister(id);
//            }
//            PayloadTypeRegistry.playS2C().unregister(id);
            if(messageType.isServerListener()) {
                ServerPlayNetworking.unregisterGlobalReceiver(id.id());
            }
        });
        messages.clear();
        classTypes.clear();
    }

    public void sendMessageToClient(AbstractMessage<?> message) {
        sendMessageToClient(null, message);
    }

    public void sendMessageToClient(ServerPlayerEntity player, AbstractMessage<?> message) {
        if(!classTypes.containsKey(message.getClass())) {
            throw new RegistryMessageException("Message " + message.getMessageType().getChannelIdWithNamespace() + " not registered");
        }

        MessageType messageType = getMessageTypeByClass(message);
        if(messageType == null) {
            throw new RegistryMessageException("Message " + message.getMessageType().getChannelIdWithNamespace() + " not registered");
        }

        AbstractMessage abstractMessage = messages.get(messageType);
        message.updateProperties(abstractMessage.getModInitializer(), messageType, abstractMessage.getId());

        if(player == null) {
            server.getPlayerManager().getPlayerList().forEach(player1 -> {
                ServerPlayNetworking.send(player1, message);
            });
            return;
        }
        ServerPlayNetworking.send(player, message);
    }

    public void sendMessageTrackerToClient(ServerPlayerEntity player, AbstractMessage<?> message) {
        MessageType messageType = getMessageTypeByClass(message);
        if(messageType == null) {
            throw new RegistryMessageException("Message " + message.getMessageType().getChannelIdWithNamespace() + " not registered");
        }

        AbstractMessage abstractMessage = messages.get(messageType);
        message.updateProperties(abstractMessage.getModInitializer(), messageType, abstractMessage.getId());

        if(player == null) {
            server.getPlayerManager().getPlayerList().forEach(player1 -> {
                ServerPlayNetworking.send(player1, message);
            });
            return;
        }
        PlayerLookup.tracking(player).forEach(player1 -> {
            ServerPlayNetworking.send(player1, message);
        });
    }

    @Override
    public MessageType getMessageTypeByClass(AbstractMessage<?> message) {
        return classTypes.get(message.getClass());
    }
}
