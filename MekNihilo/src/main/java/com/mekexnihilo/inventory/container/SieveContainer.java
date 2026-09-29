package com.mekexnihilo.inventory.container;

import com.mekexnihilo.SieveLayout;
import com.mekexnihilo.registry.MekExNihiloContainerTypes;
import com.mekexnihilo.tile.TileEntitySieve;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.world.entity.player.Inventory;

/**
 * The machine's menu.
 *
 * <p>All machine slots are contributed by the block entity itself: {@link MekanismTileContainer}
 * walks the tile's inventory and creates a menu slot for each entry, so the slot layout lives in one
 * place ({@link SieveLayout}) and is shared by the server and the client.
 */
public class SieveContainer extends MekanismTileContainer<TileEntitySieve> {

    public SieveContainer(int id, Inventory inv, TileEntitySieve tile) {
        super(MekExNihiloContainerTypes.ELECTRIC_SIEVE, id, inv, tile);
    }

    @Override
    protected int getInventoryYOffset() {
        return SieveLayout.playerInventoryY(tile.getTotalInputSlotCount(), tile.getOutputSlotCount());
    }
}
