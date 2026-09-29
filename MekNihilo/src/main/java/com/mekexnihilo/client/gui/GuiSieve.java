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

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        // The status lines are drawn in the band between the machine area and the player inventory,
        // so they can never end up underneath a slot (which is drawn after the background).
        int y = SieveLayout.infoY(tile.getOutputSlotCount());
        Component summary = tile instanceof TileEntitySieveFactory factory
                ? Component.translatable("gui.mekexnihilo.batch_and_parallel", tile.getEffectiveBatchSize(),
                        factory.getProcessCount())
                : Component.translatable("gui.mekexnihilo.batch_size", tile.getEffectiveBatchSize());
        graphics.drawString(font, summary, leftPos + 8, topPos + y, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.mekexnihilo.input_slots",
                        tile.getTotalInputSlotCount(), tile.getOutputSlotCount()),
                leftPos + 8, topPos + y + SieveLayout.INFO_LINE, 0x404040, false);
    }
}
