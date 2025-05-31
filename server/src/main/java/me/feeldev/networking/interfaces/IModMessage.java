package me.feeldev.networking.interfaces;

import me.feeldev.networking.models.AbstractMessage;
import me.feeldev.networking.models.MessageType;

public interface IModMessage<T extends AbstractMessage<T>> {

    byte[] sendMessage(T message);

    byte[] deactivateMessage();

    MessageType getMessageType();
}
