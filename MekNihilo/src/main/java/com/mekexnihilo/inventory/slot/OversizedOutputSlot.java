package com.mekexnihilo.inventory.slot;

import mekanism.api.IContentsListener;
import mekanism.api.functions.ConstantPredicates;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.slot.BasicInventorySlot;
import org.jetbrains.annotations.Nullable;

/**
 * An output slot that may hold far more than a vanilla stack.
 *
 * <p>{@link BasicInventorySlot#getLimit} normally clamps to the item's own max stack size, which
 * would cap a slot at 64 (or 99, NeoForge's absolute ceiling). Clearing {@code obeyStackLimit}
 * removes that clamp so the configured limit applies instead. Counts above 99 are safe here because
 * Mekanism serialises slots through {@code SerializerHelper.saveOversized}, which bypasses the
 * vanilla 99-item codec limit, and syncs them with a varint count that has no such limit.
 */
public class OversizedOutputSlot extends BasicInventorySlot {

    public static OversizedOutputSlot at(@Nullable IContentsListener listener, int x, int y, int limit) {
        return new OversizedOutputSlot(listener, x, y, limit);
    }

    private OversizedOutputSlot(@Nullable IContentsListener listener, int x, int y, int limit) {
        super(limit, ConstantPredicates.alwaysTrueBi(), ConstantPredicates.internalOnly(),
                ConstantPredicates.alwaysTrue(), listener, x, y);
        // Let a single slot exceed the item's own stack size.
        obeyStackLimit = false;
        setSlotType(ContainerSlotType.OUTPUT);
    }
}
