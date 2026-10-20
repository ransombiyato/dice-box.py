package io.github.ransombiyato.dicebox;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

/**
 * DiceBox Computers mod entrypoint.
 *
 * <p>Milestone A/B state: this class intentionally registers <b>no</b> blocks,
 * items, or other in-game content yet. The only implemented behavior at this
 * point is the pure-Java simulation core in
 * {@code io.github.ransombiyato.dicebox.sim}, which is covered by unit tests
 * and deliberately has no Minecraft/NeoForge dependencies. World integration
 * starts in later milestones (see docs/ROADMAP.md) — nothing here pretends
 * otherwise.
 */
// The value here must match the modId in META-INF/neoforge.mods.toml
// (generated from gradle.properties: mod_id=dicebox).
@Mod(DiceBoxMod.MODID)
public final class DiceBoxMod {
    public static final String MODID = "dicebox";

    private static final Logger LOGGER = LogUtils.getLogger();

    public DiceBoxMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info(
                "DiceBox Computers loaded (simulation-core foundation build; "
                        + "no in-game computer content is registered yet - see docs/ROADMAP.md)");
    }
}
