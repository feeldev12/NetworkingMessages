package me.feeldev.networking.client;

import me.feeldev.networking.FabricTemplateCommon;
import net.fabricmc.api.ClientModInitializer;

public class FabricTemplateClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FabricTemplateCommon.LOGGER.info("Hello Fabric Client!");
    }
}
