package com.mekexnihilo.tile;

import com.mekexnihilo.MekExNihiloConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The factory version of the Electric Sieve, produced by using a Mekanism tier installer on a plain
 * sieve (and then upgraded tier by tier, exactly like Mekanism's own factories).
 *
 * <p>It behaves like the sieve but runs several operations in parallel per cycle, which multiplies
 * how many input items a single operation may consume.
 */
public class TileEntitySieveFactory extends TileEntitySieve {

    /** Parallel operations per cycle, matching Mekanism's factory tiers. */
    private final int processes;

    public TileEntitySieveFactory(Holder<Block> blockProvider, BlockPos pos, BlockState state, int processes) {
        super(blockProvider, pos, state);
        this.processes = processes;
    }

    @Override
    protected int getProcesses() {
        return processes;
    }

    /** Number of operations this factory runs at once. */
    public int getProcessCount() {
        return processes;
    }

    /** Items consumed per operation including the parallel processes; shown in the GUI. */
    public int getEffectiveBatchSize() {
        return MekExNihiloConfig.batchSizeForLevel(Math.max(1, getMeshTier())) * processes;
    }
}
