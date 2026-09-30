package com.mekexnihilo.api;

import com.mekexnihilo.tile.TileEntitySieve;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Fired on the server on {@link NeoForge#EVENT_BUS} before a batch of items is sifted and consumed.
 *
 * <p>Ex Deorum has no sifting event of its own, so this is the addon's extension point. Use it for
 * conditional rules (per mesh, per tier, changing over time); to disable an input outright, put it
 * in the {@code mekexnihilo:sieve_blacklist} item tag instead.
 *
 * <p>Java: {@code NeoForge.EVENT_BUS.addListener(SieveInputEvent.class, event -> ...)}<br>
 * KubeJS: {@code NativeEvents.onEvent('com.mekexnihilo.api.SieveInputEvent', event => ...)}
 *
 * <p>Not cancelable. Remove entries from {@link #getInputs()} to drop them; the rest of the batch
 * still runs.
 */
public class SieveInputEvent extends Event {

    private final TileEntitySieve sieve;
    private final ItemStack mesh;
    private final int meshTier;
    private final List<ItemStack> inputs;

    public SieveInputEvent(TileEntitySieve sieve, ItemStack mesh, int meshTier, List<ItemStack> inputs) {
        this.sieve = sieve;
        this.mesh = mesh;
        this.meshTier = meshTier;
        this.inputs = inputs;
    }

    /** The machine that is about to sift. */
    public TileEntitySieve getSieve() {
        return sieve;
    }

    /** The installed mesh. */
    public ItemStack getMesh() {
        return mesh;
    }

    /** The tier of the installed mesh, 1 (string) through 6 (netherite). */
    public int getMeshTier() {
        return meshTier;
    }

    /**
     * Candidate inputs, one stack per item that would be consumed. Mutable: removing an entry stops
     * that item from being sifted this cycle. Removing all of them just makes the machine wait.
     */
    public List<ItemStack> getInputs() {
        return inputs;
    }
}
