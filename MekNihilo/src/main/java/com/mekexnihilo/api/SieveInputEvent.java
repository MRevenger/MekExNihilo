package com.mekexnihilo.api;

import com.mekexnihilo.tile.TileEntitySieve;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Fired on {@link NeoForge#EVENT_BUS} on the server whenever the Electric Sieve is about to sift a
 * batch of items, before any of them are consumed.
 *
 * <p>Ex Deorum itself does not expose a sifting event, so this is the addon's own extension point.
 * It exists so that packs and other mods can veto individual inputs without having to edit Ex
 * Deorum's recipes:
 *
 * <ul>
 *   <li>Java: {@code NeoForge.EVENT_BUS.addListener(SieveInputEvent.class, event -> ...)}
 *   <li>KubeJS: {@code NativeEvents.onEvent('com.mekexnihilo.api.SieveInputEvent', event => ...)}
 * </ul>
 *
 * <p>The simplest way to disable an input globally is the {@code mekexnihilo:sieve_blacklist} item
 * tag instead; this event is for conditional rules (per mesh, per tier, changing over time, ...).
 *
 * <p>The event is not cancelable: removing entries from {@link #getInputs()} lets a listener drop
 * only the inputs it cares about while the rest of the batch still runs.
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
     * The candidate inputs, one stack per item that would be consumed. Mutable: remove an entry (or
     * use {@code removeIf}) to stop that item from being sifted in this operation. Removing every
     * entry simply makes the machine wait for the next cycle.
     */
    public List<ItemStack> getInputs() {
        return inputs;
    }
}
