package me.feeldev.networkingmessages.networking.common;

public class MessageType {
    private final String channelId;
    private final int packetId;
    private final boolean serverListener;
    private final boolean configurationPhase;
    private String namespace;

    public MessageType(String channelId, int packetId) {
        this(channelId, packetId, false);
    }

    public MessageType(String channelId, int packetId, boolean serverListener) {
        this(channelId, packetId, serverListener, false);
    }

    public MessageType(String channelId, int packetId, boolean serverListener, boolean configurationPhase) {
        this.channelId = channelId;
        this.packetId = packetId;
        this.serverListener = serverListener;
        this.configurationPhase = configurationPhase;
    }

    public int getPacketId() {
        return packetId;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getChannelIdWithNamespace() {
        if (namespace == null) return getChannelId();
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

    public boolean isConfigurationPhase() {
        return configurationPhase;
    }
}
