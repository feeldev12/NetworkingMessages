package me.feeldev.networkingmessages.networking.managers;

import me.feeldev.networkingmessages.networking.common.CommonAPI;
import me.feeldev.networkingmessages.networking.exceptions.RegistryMessageException;
import me.feeldev.networkingmessages.networking.models.AbstractMessage;
import me.feeldev.networkingmessages.networking.common.IMessagesManager;
import me.feeldev.networkingmessages.networking.common.MessageType;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class MessagesManager implements IMessagesManager<ServerPlayer, AbstractMessage<?>> {
    private final Map<MessageType, AbstractMessage<?>> messages = new HashMap<>();
    private final Map<Class<?>, MessageType> classTypes = new HashMap<>();
    private static MessagesManager instance;

    private MinecraftServer server;
    private final SimpleChannel channel;

    public MessagesManager(MinecraftServer server, String namespace) {
        this.server = server;
        this.channel = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(namespace, "main"))
            .networkProtocolVersion(1)
            .clientAcceptedVersions((status, i) -> true)
            .serverAcceptedVersions((status, i) -> true)
            .simpleChannel();
        instance = this;
    }

    public static MessagesManager getInstance() {
        return instance;
    }

    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    public SimpleChannel getChannel() {
        return channel;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void registerMessage(MessageType messageType, @NotNull AbstractMessage message) {
        if (classTypes.containsKey(message.getClass())) {
            throw new RegistryMessageException("Message " + messageType.getChannelIdWithNamespace() + " already registered");
        }
        messages.put(messageType, message);
        classTypes.put(message.getClass(), messageType);
        AbstractMessage.getClassInstances().put(message.getClass(), message);
        registerWithChannel(messageType, message);
    }

    @SuppressWarnings("unchecked")
    private <T extends AbstractMessage<T>> void registerWithChannel(MessageType messageType, T prototype) {
        if (messageType.isConfigurationPhase() && messageType.isServerListener()) {
            channel.messageBuilder((Class<T>) prototype.getClass(), NetworkDirection.CONFIGURATION_TO_SERVER)
                .encoder((msg, buf) -> prototype.encode(buf, msg))
                .decoder(buf -> prototype.decode(buf))
                .consumerMainThread((msg, ctx) -> {
                    ServerConfigurationPacketListenerImpl handler =
                        (ServerConfigurationPacketListenerImpl) ctx.getConnection().getPacketListener();
                    msg.handleOnConfigurationServer(handler);
                })
                .add();
        } else {
            channel.messageBuilder((Class<T>) prototype.getClass())
                .encoder((msg, buf) -> prototype.encode(buf, msg))
                .decoder(buf -> prototype.decode(buf))
                .consumerMainThread((msg, ctx) -> {
                    if (messageType.isServerListener()) {
                        msg.handleOnServer(ctx.getSender());
                    } else {
                        msg.handleOnClient();
                    }
                })
                .add();
        }
        CommonAPI.LOGGER.info("[NetworkingMessages] Registered message: {}", messageType.getChannelIdWithNamespace());
    }

    @Override
    public void unregister() {
        messages.clear();
        classTypes.clear();
        AbstractMessage.getClassInstances().clear();
    }

    public Map<MessageType, AbstractMessage<?>> getMessages() {
        return messages;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void sendConfigurationMessageToClient(ServerConfigurationPacketListenerImpl handler, AbstractMessage<?> message) {
        if (!classTypes.containsKey(message.getClass())) {
            throw new RegistryMessageException("Message " + message.getMessageType().getChannelIdWithNamespace() + " not registered");
        }
        MessageType messageType = getMessageTypeByClass(message);
        AbstractMessage abstractMessage = messages.get(messageType);
        message.updateProperties(messageType, abstractMessage.type());

        handler.send(new ClientboundCustomPayloadPacket(message));
    }

    public void sendMessageToClient(AbstractMessage<?> message) {
        sendMessageToClient(null, message);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void sendMessageToClient(ServerPlayer player, AbstractMessage<?> message) {
        if (!classTypes.containsKey(message.getClass())) {
            throw new RegistryMessageException("Message " + message.getMessageType().getChannelIdWithNamespace() + " not registered");
        }
        MessageType messageType = getMessageTypeByClass(message);
        AbstractMessage abstractMessage = messages.get(messageType);
        message.updateProperties(messageType, abstractMessage.type());

        if (player == null) {
            channel.send(message, PacketDistributor.ALL.noArg());
            return;
        }
        channel.send(message, PacketDistributor.PLAYER.with(player));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void sendMessageTrackerToClient(ServerPlayer player, AbstractMessage<?> message) {
        MessageType messageType = getMessageTypeByClass(message);
        if (messageType == null) {
            throw new RegistryMessageException("Message " + message.getMessageType().getChannelIdWithNamespace() + " not registered");
        }
        AbstractMessage abstractMessage = messages.get(messageType);
        message.updateProperties(messageType, abstractMessage.type());

        if (player == null) {
            channel.send(message, PacketDistributor.ALL.noArg());
            return;
        }
        player.serverLevel().getChunkSource().chunkMap
            .getPlayers(player.chunkPosition(), false)
            .forEach(p -> channel.send(message, PacketDistributor.PLAYER.with(p)));
    }

    @Override
    public MessageType getMessageTypeByClass(AbstractMessage<?> message) {
        return classTypes.get(message.getClass());
    }
}
