package me.feeldev.networkingmessages.networking.interfaces;

import me.feeldev.networkingmessages.networking.models.AbstractMessage;
import me.feeldev.networkingmessages.networking.common.MessageType;

public interface IModMessage<T extends AbstractMessage<T>> {

    byte[] sendMessage(T message);

    MessageType getMessageType();
}
