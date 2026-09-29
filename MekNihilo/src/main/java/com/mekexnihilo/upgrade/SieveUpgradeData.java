package com.mekexnihilo.upgrade;

import java.util.List;
import mekanism.common.upgrade.IUpgradeData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * Carries a machine's contents across a tier-installer conversion (sieve to factory, and factory
 * tier to factory tier).
 *
 * <p>The stacks are stored per slot <em>role</em> rather than by raw index, because the layouts
 * differ between tiers: the plain machine has one input slot and twelve outputs, while the basic
 * factory has three inputs and sixteen outputs. Copying by index would put output items into input
 * slots.
 *
 * <p>The stacks are detached copies rather than references to the live slots, so the data stays
 * valid even though the old block entity is replaced.
 */
public class SieveUpgradeData implements IUpgradeData {

    public final ItemStack mesh;
    public final List<ItemStack> inputs;
    public final List<ItemStack> outputs;
    public final ItemStack energyItem;
    public final long energy;
    public final int operatingTicks;
    public final int ticksRequired;
    public final CompoundTag components;

    public SieveUpgradeData(ItemStack mesh, List<ItemStack> inputs, List<ItemStack> outputs, ItemStack energyItem,
            long energy, int operatingTicks, int ticksRequired, CompoundTag components) {
        this.mesh = mesh;
        this.inputs = inputs;
        this.outputs = outputs;
        this.energyItem = energyItem;
        this.energy = energy;
        this.operatingTicks = operatingTicks;
        this.ticksRequired = ticksRequired;
        this.components = components;
    }
}
