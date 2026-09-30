package com.mekexnihilo.client.gui;

import com.mekexnihilo.SieveLayout;
import com.mekexnihilo.inventory.container.SieveContainer;
import com.mekexnihilo.tile.TileEntitySieve;
import com.mekexnihilo.tile.TileEntitySieveFactory;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for the Electric Sieve.
 *
 * <p>The window is sized from the actual slot counts so it always fits the configured layout. The
 * background comes from Mekanism's nine-sliced {@code base.png}, which scales to any size.
 */
public class GuiSieve extends GuiConfigurableTile<TileEntitySieve, SieveContainer> {

    public GuiSieve(SieveContainer container, Inventory inv, Component title) {
        super(container, inv, title);
        int inputSlots = tile.getTotalInputSlotCount();
        int outputSlots = tile.getOutputSlotCount();
        // The window grows with the machine so every configured slot stays visible.
        imageWidth = SieveLayout.guiWidth(inputSlots, outputSlots);
        imageHeight = SieveLayout.guiHeight(inputSlots, outputSlots);
        // Must be recomputed whenever the height changes, otherwise the label is misplaced.
        inventoryLabelY = imageHeight - 94;
        // Lets Mekanism draw the container's slots automatically.
        dynamicSlots = true;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addRenderableWidget(new GuiProgress(tile::getScaledProgress, ProgressType.BAR, this,
                SieveLayout.PROGRESS_X, SieveLayout.PROGRESS_Y));
        // Vertical energy bar down the right hand edge, the way Mekanism's own machines show it.
        addRenderableWidget(new GuiVerticalPowerBar(this, tile.getEnergyContainer(),
                SieveLayout.powerBarX(tile.getTotalInputSlotCount(), tile.getOutputSlotCount()),
                SieveLayout.POWER_BAR_Y));
        addRenderableWidget(new GuiEnergyTab(this, tile.getEnergyContainer(), tile::getActive));
    }
}
