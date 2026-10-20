package dev.minetech.client;

import net.fabricmc.api.ClientModInitializer;

/**
 * Client entrypoint.
 *
 * <p>Renderers for the micro-parts and the wall editor screen are registered
 * here once the blocks land.
 */
public final class MinetechClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // renderer and screen registration comes with the first block
    }
}
