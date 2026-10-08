package me.feeldev.networkingmessages.networking.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;

/** Client-only connection access, kept out of the shared manager so dedicated servers never load it. */
public final class ClientSender {
    private ClientSender() {}

    public static void send(Packet<?> packet) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            throw new IllegalStateException("Cannot send a message to the server: not connected");
        }
        connection.send(packet);
    }
}
