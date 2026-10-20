package dev.minetech;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entrypoint.
 *
 * <p>Registration of blocks, items, block entities and the bundled blueprints
 * happens here in later milestones; the circuit engine itself lives in
 * {@code dev.minetech.core} and never touches Minecraft classes.
 */
public final class Minetech implements ModInitializer {

    public static final String MOD_ID = "minetech";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Minetech loading: micro-electronics for Minecraft");
    }
}
