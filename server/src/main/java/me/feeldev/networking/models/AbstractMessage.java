package me.feeldev.networking.models;

import me.feeldev.networking.interfaces.IModMessage;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class AbstractMessage<T extends AbstractMessage<T>> implements IModMessage<T> {
    private MessageType type;
    protected JavaPlugin plugin;

    public AbstractMessage(JavaPlugin plugin, MessageType type) {
        this.plugin = plugin;
        this.type = type;
    }

    public AbstractMessage() {

    }

    @Override
    public MessageType getMessageType() {
        return type;
    }

}
