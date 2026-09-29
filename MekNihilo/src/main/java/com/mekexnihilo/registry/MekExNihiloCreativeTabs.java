package com.mekexnihilo.registry;

import com.mekexnihilo.MekExNihilo;
import mekanism.common.registration.impl.CreativeTabDeferredRegister;
import mekanism.common.registries.MekanismCreativeTabs;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/**
 * Puts the machines into Mekanism's own creative tab so they show up next to the other machines.
 */
public class MekExNihiloCreativeTabs {

    public static final CreativeTabDeferredRegister CREATIVE_TABS =
            new CreativeTabDeferredRegister(MekExNihilo.MOD_ID, MekExNihiloCreativeTabs::addToExistingTabs);

    private static void addToExistingTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == MekanismCreativeTabs.MEKANISM.getKey()) {
            CreativeTabDeferredRegister.addToDisplay(event,
                    MekExNihiloBlocks.ELECTRIC_SIEVE,
                    MekExNihiloBlocks.BASIC_SIEVE_FACTORY,
                    MekExNihiloBlocks.ADVANCED_SIEVE_FACTORY,
                    MekExNihiloBlocks.ELITE_SIEVE_FACTORY,
                    MekExNihiloBlocks.ULTIMATE_SIEVE_FACTORY);
        }
    }

    private MekExNihiloCreativeTabs() {}
}
