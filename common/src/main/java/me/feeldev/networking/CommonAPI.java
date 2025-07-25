package me.feeldev.networking;

import me.feeldev.networking.models.NetworkAPI;
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
}
