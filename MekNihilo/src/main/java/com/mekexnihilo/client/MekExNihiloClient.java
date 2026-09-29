package com.mekexnihilo.client;

import com.mekexnihilo.MekExNihilo;
import com.mekexnihilo.client.gui.GuiSieve;
import com.mekexnihilo.registry.MekExNihiloContainerTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-side wiring: binds the menu types to their screen. */
@EventBusSubscriber(modid = MekExNihilo.MOD_ID, value = Dist.CLIENT)
public class MekExNihiloClient {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        // The factory tiers share the sieve's menu and screen; both adapt automatically.
        event.register(MekExNihiloContainerTypes.ELECTRIC_SIEVE.get(), GuiSieve::new);
    }
}
