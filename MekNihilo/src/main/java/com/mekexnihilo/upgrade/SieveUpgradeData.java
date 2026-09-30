package com.mekexnihilo.upgrade;

import java.util.List;
import mekanism.common.upgrade.IUpgradeData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * Carries a machine's contents across a tier-installer conversion (sieve to factory, and factory
 * tier to factory tier).
 *
 * <p>Stacks are keyed by slot role, not raw index, because the layouts differ per tier: the plain
 * machine has one input and twelve outputs, the basic factory three and sixteen. Copying by index
 * would drop output items into input slots.
 *
 * <p>They are detached copies, not references, so the data outlives the replaced block entity.
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
