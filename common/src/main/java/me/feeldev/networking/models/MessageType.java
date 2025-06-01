package me.feeldev.networking.models;

public class MessageType {
    private final String channelId;
    private final int packetId;
    private final boolean serverListener;

    private String namespace;

    public MessageType(String channelId, int packetId) {
        this(channelId, packetId, false);
    }

    public MessageType(String channelId, int packetId, boolean serverListener) {
        this.channelId = channelId;
        this.packetId = packetId;
        this.serverListener = serverListener;
    }

    public int getPacketId() {
        return packetId;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getChannelIdWithNamespace() {
        if(namespace == null) return getChannelId();
        return namespace + ":" + channelId;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getNamespace() {
        return namespace;
    }

    public boolean isServerListener() {
        return serverListener;
    }
}
