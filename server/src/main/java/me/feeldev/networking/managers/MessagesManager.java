package me.feeldev.networking.managers;

import me.feeldev.networking.ServerAPI;
import me.feeldev.networking.exceptions.MessageNullException;
import me.feeldev.networking.exceptions.RegistryMessageException;
import me.feeldev.networking.models.AbstractMessage;
import me.feeldev.networking.models.MessageType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.HashMap;
import java.util.Map;

public class MessagesManager {
    private final Map<MessageType, AbstractMessage> messages;
    private final Map<Class<?>, MessageType> classTypes;

    private final JavaPlugin plugin;
    private final String namespace;

    private ServerAPI serverAPI;

    public MessagesManager(JavaPlugin plugin, ServerAPI serverAPI, String namespace) {
        this.plugin = plugin;
        this.messages = new HashMap<>();
        this.classTypes = new HashMap<>();
        this.serverAPI = serverAPI;
        this.namespace = namespace.endsWith(":") ? namespace : namespace + ":";
    }

    public void registerMessage(MessageType messageType, AbstractMessage<?> message) {
        if(classTypes.containsKey(message.getClass())) {
            throw new RegistryMessageException("Message already registered");
        }

        messages.put(messageType, message);
        classTypes.put(message.getClass(), messageType);

        if(messageType.isServerListener()) {
            plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, namespace + messageType.getChannelId(), (PluginMessageListener) messages.get(messageType));
        }
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, namespace + messageType.getChannelId());
    }

    public void unregister() {
        messages.clear();
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin);
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin);
    }

    public void sendMessageToClient(AbstractMessage<?> message) {
        sendMessageToClient(null, message);
    }

    public void sendMessageToClient(Player player, AbstractMessage<?> message) {
        MessageType messageType = getMessageTypeByClass(message);
        if(messageType == null) {
            throw new RegistryMessageException("Message " + message.getClass().getSimpleName() + " not registered");
        }
        byte[] messageBytes = messages.get(messageType).sendMessage(message);

        if(messageBytes == null) {
            throw new MessageNullException(" message bytes in " + message.getClass().getSimpleName() + " is null");
        }
        if(player == null) {
            plugin.getServer().sendPluginMessage(plugin, namespace + messageType.getChannelId(), messageBytes);
            return;
        }
        player.sendPluginMessage(plugin, namespace + messageType.getChannelId(), messageBytes);
    }

    public void deactivateMessage(MessageType messageType) {
        deactivateMessage(null, messageType);
    }

    public void deactivateMessage(Player player, MessageType messageType) {
        byte[] messageBytes = messages.get(messageType).deactivateMessage();
        if(messageBytes == null) {
            throw new MessageNullException(" message bytes in " + messageType.getChannelId() + " is null");
        }
        if(player == null) {
            plugin.getServer().sendPluginMessage(plugin, namespace + messageType.getChannelId(), messageBytes);
            return;
        }
        player.sendPluginMessage(plugin, namespace + messageType.getChannelId(), messageBytes);
    }

    public MessageType getMessageTypeByClass(AbstractMessage<?> message) {
        return classTypes.get(message.getClass());
    }
}
