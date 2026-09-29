package com.mekexnihilo.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Adds the Config button to this mod's entry in the in-game mod list.
 *
 * <p>Kept in its own client-only class on purpose: {@code IConfigScreenFactory} lives in a
 * client-only package, so the mod constructor must not reference it directly or a dedicated
 * server would fail to load the class.
 */
public final class MekExNihiloConfigScreen {

    public static void register(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (IConfigScreenFactory) (mod, parent) -> new ConfigurationScreen(mod, parent));
    }

    private MekExNihiloConfigScreen() {}
}
