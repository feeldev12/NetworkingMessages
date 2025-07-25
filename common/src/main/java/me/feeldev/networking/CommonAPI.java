package me.feeldev.networking;

import me.feeldev.networking.models.NetworkAPI;
import me.feeldev.networking.serialization.FriendlyByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonAPI {
    private static final String MOD_ID = "networking_messages";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static NetworkAPI<?> networkAPI;

    public static void setNetworkAPI(NetworkAPI<?> networkAPI) {
        CommonAPI.networkAPI = networkAPI;
    }

    public static NetworkAPI<?> getNetworkAPI() {
        return networkAPI;
    }

    public static void printPacketStats(FriendlyByteBuf friendlyByteBuf) {
        boolean realCompressed = friendlyByteBuf.isCompressed();
        friendlyByteBuf.enableCompression();
        byte[] uncompressed = friendlyByteBuf.copy(0, friendlyByteBuf.readableBytes()).array();

        long start = System.nanoTime();
        byte[] compressed = friendlyByteBuf.readOnlyNecessaryBytes();
        long end = System.nanoTime();

        int uncompressedSize = uncompressed.length;
        int compressedSize = compressed.length;
        double reduction = ((uncompressedSize - compressedSize) * 100.0) / uncompressedSize;

        LOGGER.info("Uncompressed serialized packet size: {} bytes | ({} KB)", uncompressed.length, uncompressed.length / 1024.0);
        LOGGER.info("Compressed serialized packet size: {} bytes | ({} KB)", compressed.length, compressed.length / 1024.0);
        LOGGER.info("Compression time: {} ms", (end - start) / 1_000_000.0);

        LOGGER.info("Compression effectiveness: {}%", String.format("%.2f", reduction));

        if(!realCompressed) {
            LOGGER.warn("Packet was not originally compressed.");
            friendlyByteBuf.disableCompression();
        }
    }
}
