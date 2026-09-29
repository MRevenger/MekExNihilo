package com.mekexnihilo;

import com.mekexnihilo.client.MekExNihiloConfigScreen;
import com.mekexnihilo.registry.MekExNihiloBlocks;
import com.mekexnihilo.registry.MekExNihiloContainerTypes;
import com.mekexnihilo.registry.MekExNihiloCreativeTabs;
import com.mekexnihilo.registry.MekExNihiloTileEntityTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MekExNihilo: a Mekanism addon for Ex Deorum.
 *
 * <p>Adds the Sieve Machine: a Mekanism machine that runs Ex Deorum's sifting recipes using any
 * mesh tier, powered by Mekanism energy, plus four factory tiers.
 */
@Mod(MekExNihilo.MOD_ID)
public class MekExNihilo {

    public static final String MOD_ID = "mekexnihilo";
    public static final Logger LOGGER = LoggerFactory.getLogger("MekExNihilo");

    public MekExNihilo(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, MekExNihiloConfig.SPEC);

        // Give this mod a Config button in the in-game mod list, opening NeoForge's built-in editor
        // for the config registered above. Done from a client-only class so a dedicated server never
        // resolves the client-only config screen types.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            MekExNihiloConfigScreen.register(modContainer);
        }

        // Order matters: blocks must exist before the block entity types that reference them.
        MekExNihiloBlocks.BLOCKS.register(modEventBus);
        MekExNihiloContainerTypes.CONTAINER_TYPES.register(modEventBus);
        MekExNihiloTileEntityTypes.TILE_ENTITY_TYPES.register(modEventBus);
        MekExNihiloCreativeTabs.CREATIVE_TABS.register(modEventBus);

        // Recipes can change on a datapack (or KubeJS) reload, so drop the cached sifting recipes
        // and let the next lookup rebuild from the new recipe manager.
        NeoForge.EVENT_BUS.addListener(AddReloadListenerEvent.class, event -> ExDeorumCompat.invalidate());
        NeoForge.EVENT_BUS.addListener(ServerStoppingEvent.class, event -> ExDeorumCompat.invalidate());
        // Print the values that were actually loaded so a pack author can confirm from latest.log
        // that their config file was picked up.
        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, event -> MekExNihiloConfig.logLoadedValues());

        LOGGER.info("MekExNihilo loaded: Sieve Machine and factory tiers registered");
    }
}
