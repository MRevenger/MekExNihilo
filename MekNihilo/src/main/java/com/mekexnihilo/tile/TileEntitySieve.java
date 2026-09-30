package com.mekexnihilo.tile;

import com.mekexnihilo.AllTheCompressedCompat;
import com.mekexnihilo.ExDeorumCompat;
import com.mekexnihilo.MekExNihiloConfig;
import com.mekexnihilo.MekExNihiloTags;
import com.mekexnihilo.SieveLayout;
import com.mekexnihilo.api.SieveInputEvent;
import com.mekexnihilo.inventory.slot.OversizedOutputSlot;
import com.mekexnihilo.upgrade.SieveUpgradeData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.RelativeSide;
import mekanism.api.Upgrade;
import mekanism.api.IContentsListener;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.tier.BaseTier;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.ITileComponent;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.ConfigInfo;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.component.config.slot.InventorySlotInfo;
import mekanism.common.tile.base.WrenchResult;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootContext;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import thedarkcolour.exdeorum.recipe.sieve.SieveRecipe;

/**
 * An Ex Deorum sieve driven by Mekanism energy. The mesh goes in its own slot, siftable materials
 * in the input slots, drops in the output slots.
 *
 * <p>The mesh tier sets how many items one operation consumes (1, 2, 4, 16, 32, 64); the machine
 * level sets how many input slots there are. Efficiency shortens the processing time and Fortune
 * raises the yield, each by a configurable amount per level.
 *
 * <p>Drops come from Ex Deorum's recipes, so they match the equivalent hand sieve.
 */
public class TileEntitySieve extends TileEntityConfigurableMachine {

    private static final String NBT_OPERATING_TICKS = "operatingTicks";
    private static final String NBT_TICKS_REQUIRED = "ticksRequired";

    /**
     * Three minutes without managing to eject anything means nothing is draining the machine, so it
     * only tries once per second from then on instead of every tick. A successful eject resets it.
     */
    private static final int IDLE_EJECT_THRESHOLD = 3 * 60 * 20;
    private static final int IDLE_EJECT_INTERVAL = 20;

    /** Ticks since the last successful eject, and whether the idle throttle is currently engaged. */
    private int ticksSinceEject;
    private boolean ejectThrottled;

    // Slot counts are read from the config while the inventory is built, never cached in a static
    // field: a static initializer would freeze the values at class-load time, which is what made a
    // changed config appear to have no effect.
    private int machineLevel;
    private int inputSlotCount;
    private int outputSlotCount;

    // Assigned inside getInitialInventory: Java runs field initializers *after* the superclass
    // constructor, and Mekanism's TileEntityMekanism constructor already calls getInitialInventory.
    private List<IInventorySlot> inputSlots;
    private List<IInventorySlot> outputSlots;

    private BasicInventorySlot meshSlot;
    private EnergyInventorySlot energySlot;
    private MachineEnergyContainer<TileEntitySieve> energyContainer;

    private int operatingTicks;
    private int ticksRequired = MekExNihiloConfig.baseTicks();

    /**
     * Fortune level snapshotted when an operation starts, so swapping the mesh halfway through
     * cannot retroactively change the result.
     */
    private int fortuneLevel;

    /**
     * Cached answer to "is there anything to process". Recomputing it means asking Ex Deorum's
     * recipe cache, so it is only refreshed when the relevant slots actually change.
     */
    private boolean hasWork;
    private boolean workDirty = true;

    public TileEntitySieve(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state);

        // The mesh slot is deliberately NOT part of the item input group. It holds a tool rather
        // than a sieve input, so it gets its own EXTRA group: pipes only touch it on a side that is
        // explicitly configured as EXTRA, and the side config screen shows it as a separate slot.
        ConfigInfo itemConfig = configComponent.getConfig(TransmissionType.ITEM);
        if (itemConfig != null) {
            itemConfig.addSlotInfo(DataType.INPUT, new InventorySlotInfo(true, false, inputSlots));
            itemConfig.addSlotInfo(DataType.OUTPUT, new InventorySlotInfo(false, true, outputSlots));

            List<IInventorySlot> ioSlots = new ArrayList<>(inputSlots.size() + outputSlots.size());
            ioSlots.addAll(inputSlots);
            ioSlots.addAll(outputSlots);
            itemConfig.addSlotInfo(DataType.INPUT_OUTPUT, new InventorySlotInfo(true, true, ioSlots));

            itemConfig.addSlotInfo(DataType.EXTRA, new InventorySlotInfo(true, true, List.of(meshSlot)));
            itemConfig.addSlotInfo(DataType.ENERGY, new InventorySlotInfo(true, true, List.of(energySlot)));
        }
        configComponent.setupInputConfig(TransmissionType.ENERGY, energyContainer);

        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM);
        // Items are moved by tickEjection() instead. Mekanism's own item ejection waits ten ticks
        // after every attempt, so it can never move a full slot in a single tick. The component is
        // still what owns the side configuration, the GUI tab and serialisation.
        ejectorComponent.setCanEject(type -> type != TransmissionType.ITEM);
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        // The block is already known here (the superclass assigns it before calling this), so the
        // machine level can be derived from its tier attribute even though our own fields are not
        // initialised yet.
        machineLevel = machineLevelFromBlock();
        inputSlotCount = MekExNihiloConfig.inputSlotsForMachineLevel(machineLevel);
        outputSlotCount = MekExNihiloConfig.outputSlotsForMachineLevel(machineLevel);

        inputSlots = new ArrayList<>(inputSlotCount);
        outputSlots = new ArrayList<>(outputSlotCount);
        InventorySlotHelper builder = InventorySlotHelper.forSideWithConfig(this);

        // Any Ex Deorum mesh is accepted; its tier is read back from the item when the machine runs.
        IContentsListener meshListener = () -> {
            workDirty = true;
            listener.onContentsChanged();
        };
        builder.addSlot(meshSlot = BasicInventorySlot.at(
                ExDeorumCompat::isMesh, meshListener, SieveLayout.MESH_X, SieveLayout.MESH_Y, 1));

        IContentsListener inputListener = () -> {
            workDirty = true;
            listener.onContentsChanged();
        };
        for (int i = 0; i < inputSlotCount; i++) {
            InputInventorySlot slot = InputInventorySlot.at(
                    this::isAcceptableInput, inputListener, SieveLayout.inputSlotX(i), SieveLayout.INPUT_Y);
            inputSlots.add(slot);
            builder.addSlot(slot);
        }

        for (int i = 0; i < outputSlotCount; i++) {
            // Oversized so heavily multiplied compressed yields do not jam the machine.
            OversizedOutputSlot slot = OversizedOutputSlot.at(listener,
                    SieveLayout.outputSlotX(i, outputSlotCount), SieveLayout.outputSlotY(i, outputSlotCount),
                    MekExNihiloConfig.outputSlotLimit());
            outputSlots.add(slot);
            builder.addSlot(slot);
        }

        builder.addSlot(energySlot = EnergyInventorySlot.fillOrConvert(energyContainer, this::getLevel, listener,
                SieveLayout.ENERGY_X, SieveLayout.ENERGY_Y));

        return builder.build();
    }

    /**
     * Machine level derived from the block: 0 for the plain Sieve Machine (which deliberately has no
     * tier attribute so Mekanism's basic tier installer matches it), 1..4 for the factory tiers.
     */
    private int machineLevelFromBlock() {
        BaseTier tier = Attribute.getBaseTier(getBlockHolder());
        if (tier == null) {
            return MekExNihiloConfig.BASE_MACHINE_LEVEL;
        }
        return switch (tier) {
            case BASIC -> 1;
            case ADVANCED -> 2;
            case ELITE -> 3;
            case ULTIMATE -> 4;
            default -> MekExNihiloConfig.BASE_MACHINE_LEVEL;
        };
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSideWithConfig(this);
        builder.addContainer(energyContainer = MachineEnergyContainer.input(this, listener));
        return builder.build();
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdate = super.onUpdateServer();
        // Runs before the idle returns below, so outputs keep flowing out of a machine that is not
        // currently sifting anything.
        tickEjection();

        int tier = getMeshTier();
        if (tier <= 0) {
            if (operatingTicks != 0) {
                operatingTicks = 0;
                markForSave();
            }
            setActive(false);
            return sendUpdate;
        }

        int batchLimit = MekExNihiloConfig.batchSizeForLevel(tier) * getProcesses();

        if (operatingTicks <= 0) {
            if (workDirty) {
                workDirty = false;
                hasWork = !collectBatch(batchLimit).isEmpty();
            }
            if (!hasWork || !hasOutputRoom() || !hasEnergyForTick()) {
                setActive(false);
                return sendUpdate;
            }
            beginOperation();
        } else if (!hasEnergyForTick()) {
            setActive(false);
            return sendUpdate;
        }

        energyContainer.extract(energyPerTick(), Action.EXECUTE, AutomationType.INTERNAL);
        operatingTicks++;
        setActive(true);

        if (operatingTicks >= ticksRequired) {
            finishOperation(batchLimit);
        }
        return sendUpdate;
    }

    /** Snapshots the enchantment-derived values for the operation that is about to start. */
    private void beginOperation() {
        fortuneLevel = enchantmentLevel(Enchantments.FORTUNE);
        ticksRequired = computeTicksRequired();
        // The tick that starts the operation is counted by the caller's increment.
        operatingTicks = 0;
    }

    /**
     * Recomputes the processing time when the Speed upgrade changes.
     *
     * <p>Mekanism calls this when an upgrade is inserted or removed. Without it a new speed only
     * takes effect from the next operation on.
     */
    @Override
    public void recalculateUpgrades(Upgrade upgrade) {
        super.recalculateUpgrades(upgrade);
        if (upgrade == Upgrade.SPEED) {
            ticksRequired = computeTicksRequired();
        }
    }

    /**
     * Rolls and stores the drops for one batch.
     *
     * <p>The batch is handled one input item at a time: an item is only consumed once its own drops
     * have been proven to fit. A strong mesh can roll more drops than the output slots hold, so
     * requiring the whole batch to fit in one go would deadlock the machine forever.
     */
    private void finishOperation(int batchLimit) {
        if (!(getLevel() instanceof ServerLevel level)) {
            return;
        }
        ItemStack mesh = meshSlot.getStack();

        List<ItemStack> batch = collectBatch(batchLimit);
        if (batch.isEmpty()) {
            // Nothing usable (or everything was vetoed): go idle rather than burning energy.
            operatingTicks = 0;
            workDirty = true;
            markForSave();
            return;
        }

        int processed = 0;
        for (ItemStack input : batch) {
            List<ItemStack> drops = rollDrops(level, mesh, input);
            if (!canFitAll(drops)) {
                // Out of room: the remaining inputs wait for the next run.
                break;
            }
            for (ItemStack drop : drops) {
                storeInOutputs(drop);
            }
            consumeOne(input);
            processed++;
        }

        if (processed == 0) {
            // The output is full. Go idle rather than spending energy on further attempts.
            operatingTicks = 0;
            return;
        }

        damageMesh();
        operatingTicks = 0;
        workDirty = true;
        markForSave();
    }

    /**
     * Rolls Ex Deorum's sifting recipes for a single input item.
     *
     * <p>Ex Deorum encodes the chance of a drop in the recipe's {@code result_amount} number
     * provider (usually a 0/1 draw at a given probability), so the amount is simply sampled from it.
     * Fortune is applied as a multiplier on that amount using probabilistic rounding, which keeps
     * the long run average at exactly {@code base * multiplier}.
     */
    private List<ItemStack> rollDrops(ServerLevel level, ItemStack mesh, ItemStack input) {
        // A compressed material is sifted with the recipe of the material it is made of.
        AllTheCompressedCompat.Compressed compressed = AllTheCompressedCompat.resolve(input);
        ItemStack lookup = compressed == null ? input : compressed.base().getDefaultInstance();
        List<SieveRecipe> recipes = ExDeorumCompat.recipes(level, mesh, lookup);
        if (recipes.isEmpty()) {
            return List.of();
        }

        LootContext context = ExDeorumCompat.emptyLootContext(level);
        RandomSource random = level.getRandom();
        double multiplier = 1.0D + fortuneLevel * MekExNihiloConfig.fortuneBonusPerLevel();

        // A compressed material repeats the base recipe rather than scaling one roll. Multiplying the
        // amount would turn a rare drop into a guaranteed one (a 25% chance rolled once and multiplied
        // by 8 always yields 2), whereas running the same event eight times keeps the recipe's own
        // probabilities and averages the same.
        int repeats = compressed == null ? 1 : compressedRepeats(compressed.tier());

        List<ItemStack> produced = new ArrayList<>();
        for (int repeat = 0; repeat < repeats; repeat++) {
            rollOnce(recipes, context, random, multiplier, produced);
        }
        return produced;
    }

    /** One pass of every matching recipe: the "sifting event" for a single unit of input. */
    private static void rollOnce(List<SieveRecipe> recipes, LootContext context, RandomSource random,
            double multiplier, List<ItemStack> produced) {
        for (SieveRecipe recipe : recipes) {
            if (recipe.byHandOnly && !MekExNihiloConfig.siftByHandOnlyRecipes()) {
                // Ex Deorum's own mechanical sieve skips these as well.
                continue;
            }
            int base = recipe.resultAmount.getInt(context);
            if (base <= 0) {
                continue;
            }
            int amount = applyFortune(base, multiplier, random);
            ItemStack result = recipe.result;
            while (amount > 0) {
                int chunk = Math.min(amount, Math.max(1, result.getMaxStackSize()));
                produced.add(result.copyWithCount(chunk));
                amount -= chunk;
            }
        }
    }

    /**
     * How many times a compressed material repeats the sifting event.
     *
     * <p>The yield base is a double but an event runs a whole number of times, so it is rounded.
     */
    private static int compressedRepeats(int tier) {
        long configured = Math.round(MekExNihiloConfig.compressedYieldMultiplier(tier));
        // The count grows exponentially, so a high tier with a large base would roll millions of
        // times in one tick and stall the server. Clamp it; the cap only bites at tiers whose yield
        // could not possibly fit in the output slots anyway.
        long capped = Math.min(configured, MekExNihiloConfig.maxCompressedRepeats());
        return (int) Math.max(1L, capped);
    }

    private static int applyFortune(int base, double multiplier, RandomSource random) {
        double exact = base * multiplier;
        int whole = (int) exact;
        if (random.nextFloat() < (float) (exact - whole)) {
            whole++;
        }
        return whole;
    }

    /** Inserts a stack into the output slots. The caller has already checked that it fits. */
    private void storeInOutputs(ItemStack stack) {
        ItemStack remaining = stack;
        for (IInventorySlot slot : outputSlots) {
            remaining = slot.insertItem(remaining, Action.EXECUTE, AutomationType.INTERNAL);
            if (remaining.isEmpty()) {
                return;
            }
        }
    }

    // Ejecting

    /**
     * Pushes one whole output slot into the neighbouring inventories, once per tick.
     *
     * <p>Mekanism's own item ejection waits ten ticks between attempts, which a fast factory outruns.
     */
    private void tickEjection() {
        if (getLevel() == null || getLevel().isClientSide()) {
            return;
        }
        if (ejectThrottled && getLevel().getGameTime() % IDLE_EJECT_INTERVAL != 0) {
            return;
        }
        if (ejectOutputs() > 0) {
            ticksSinceEject = 0;
            ejectThrottled = false;
        } else if (ticksSinceEject < IDLE_EJECT_THRESHOLD && ++ticksSinceEject >= IDLE_EJECT_THRESHOLD) {
            // Nothing has taken anything for three minutes, so the machine counts as idle.
            ejectThrottled = true;
        }
    }

    /** True while the idle eject throttle is engaged, i.e. the machine only tries once per second. */
    public boolean isEjectThrottled() {
        return ejectThrottled;
    }

    /** The item side configuration, so integrations and tests can drive the output sides. */
    public ConfigInfo itemConfig() {
        return configComponent.getConfig(TransmissionType.ITEM);
    }

    /** Moves the first non-empty output slot out, returning how many items left the machine. */
    private int ejectOutputs() {        ConfigInfo itemConfig = configComponent.getConfig(TransmissionType.ITEM);
        if (itemConfig == null || !itemConfig.isEjecting() || !itemConfig.canEject()) {
            return 0;
        }
        IInventorySlot source = null;
        for (IInventorySlot slot : outputSlots) {
            if (!slot.isEmpty()) {
                source = slot;
                break;
            }
        }
        if (source == null) {
            return 0;
        }

        Direction facing = getDirection();
        int moved = 0;
        for (Map.Entry<RelativeSide, DataType> entry : itemConfig.getSideConfig()) {
            if (entry.getValue() != DataType.OUTPUT) {
                continue;
            }
            moved += pushTo(entry.getKey().getDirection(facing), source);
            if (source.isEmpty()) {
                break;
            }
        }
        return moved;
    }

    /** Offers the source slot's whole contents to the inventory on one side. */
    private int pushTo(Direction side, IInventorySlot source) {
        Level level = getLevel();
        if (level == null) {
            return 0;
        }
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK,
                getBlockPos().relative(side), side.getOpposite());
        if (handler == null) {
            return 0;
        }
        ItemStack stack = source.getStack();
        if (stack.isEmpty()) {
            return 0;
        }
        ItemStack remaining = stack.copy();
        for (int i = 0; i < handler.getSlots() && !remaining.isEmpty(); i++) {
            remaining = handler.insertItem(i, remaining, false);
        }
        int moved = stack.getCount() - remaining.getCount();
        if (moved > 0) {
            if (source instanceof BasicInventorySlot basic) {
                basic.setStackUnchecked(remaining);
            } else {
                source.setStack(remaining);
            }
        }
        return moved;
    }

    /** Removes a single matching item from the usable input slots. */
    private void consumeOne(ItemStack wanted) {
        int active = inputSlotCount;
        for (int i = 0; i < active; i++) {
            IInventorySlot slot = inputSlots.get(i);
            ItemStack current = slot.getStack();
            if (!current.isEmpty() && ItemStack.isSameItemSameComponents(current, wanted)) {
                slot.shrinkStack(1, Action.EXECUTE);
                return;
            }
        }
    }

    /**
     * Gathers up to {@code limit} single items that the installed mesh can actually sift.
     *
     * <p>Every candidate in the usable slots is offered to {@link SieveInputEvent} first and the
     * batch is only trimmed to {@code limit} afterwards. The order matters: if a vetoed item took
     * up one of the batch's places, a disabled input sitting in front of a usable one would starve
     * the machine forever.
     */
    private List<ItemStack> collectBatch(int limit) {
        ItemStack mesh = meshSlot.getStack();
        if (mesh.isEmpty()) {
            return new ArrayList<>();
        }
        List<ItemStack> candidates = new ArrayList<>();
        int active = inputSlotCount;
        for (int i = 0; i < active; i++) {
            ItemStack stack = inputSlots.get(i).getStack();
            if (stack.isEmpty() || !isSiftable(stack, mesh)) {
                continue;
            }
            for (int n = 0; n < stack.getCount(); n++) {
                candidates.add(stack.copyWithCount(1));
            }
        }
        if (candidates.isEmpty()) {
            return candidates;
        }

        // Ex Deorum has no sifting event of its own, so this addon fires one here. Listeners may
        // remove entries they do not want processed.
        NeoForge.EVENT_BUS.post(new SieveInputEvent(this, mesh, getMeshTier(), candidates));

        if (candidates.size() <= limit) {
            return candidates;
        }
        return new ArrayList<>(candidates.subList(0, limit));
    }

    /** Dry-run insertion of every produced stack into the output slots. */
    private boolean canFitAll(List<ItemStack> produced) {
        if (produced.isEmpty()) {
            return true;
        }
        List<ItemStack> simulated = new ArrayList<>(outputSlots.size());
        List<Integer> limits = new ArrayList<>(outputSlots.size());
        for (IInventorySlot slot : outputSlots) {
            simulated.add(slot.getStack().copy());
            // The slot's own limit, not the item's stack size: outputs may be oversized.
            limits.add(slot.getLimit(slot.getStack()));
        }
        for (ItemStack stack : produced) {
            if (!simulateInsert(simulated, limits, stack)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Mirrors {@link IInventorySlot#insertItem} against a detached list of stacks, so the outcome can
     * be checked before anything is committed.
     *
     * <p>{@code limits} carries each output slot's real capacity, which can exceed a vanilla stack.
     */
    private static boolean simulateInsert(List<ItemStack> slots, List<Integer> limits, ItemStack stack) {
        ItemStack remaining = stack;
        for (int i = 0; i < slots.size() && !remaining.isEmpty(); i++) {
            ItemStack current = slots.get(i);
            int limit = limits.get(i);
            if (current.isEmpty()) {
                int moved = Math.min(remaining.getCount(), limit);
                slots.set(i, remaining.copyWithCount(moved));
                remaining = remaining.copyWithCount(remaining.getCount() - moved);
            } else if (ItemStack.isSameItemSameComponents(current, remaining)) {
                int space = limit - current.getCount();
                if (space > 0) {
                    int moved = Math.min(remaining.getCount(), space);
                    slots.set(i, current.copyWithCount(current.getCount() + moved));
                    remaining = remaining.copyWithCount(remaining.getCount() - moved);
                }
            }
        }
        return remaining.isEmpty();
    }

    /** True when at least one output slot could accept another item. */
    private boolean hasOutputRoom() {
        for (IInventorySlot slot : outputSlots) {
            ItemStack stack = slot.getStack();
            // getLimit already accounts for the slot, which may be oversized; do not clamp it to the
            // item's stack size or a slot holding 64 items would wrongly look full.
            if (stack.isEmpty() || stack.getCount() < slot.getLimit(stack)) {
                return true;
            }
        }
        return false;
    }

    private void damageMesh() {
        if (!MekExNihiloConfig.damageMesh()) {
            return;
        }
        ItemStack mesh = meshSlot.getStack();
        if (mesh.isEmpty() || !mesh.isDamageableItem()) {
            return;
        }
        ItemStack damaged = mesh.copy();
        damaged.setDamageValue(damaged.getDamageValue() + 1);
        if (damaged.getDamageValue() >= damaged.getMaxDamage()) {
            meshSlot.setEmpty();
        } else {
            meshSlot.setStack(damaged);
        }
    }

    private int computeTicksRequired() {
        // Mekanism's Speed upgrade shortens the base time; Efficiency then shaves a further fraction
        // off. getTicks reads the installed upgrades, so it must be re-evaluated whenever they change
        // (see recalculateUpgrades) and whenever a new operation starts.
        int base = MekanismUtils.getTicks(this, MekExNihiloConfig.baseTicks());
        double reduction = enchantmentLevel(Enchantments.EFFICIENCY) * MekExNihiloConfig.efficiencyReductionPerLevel();
        // The processing time can never be reduced by more than 100%.
        reduction = Math.min(1.0D, Math.max(0.0D, reduction));
        // Compressed materials take proportionally longer.
        double compressed = MekExNihiloConfig.compressedTimeMultiplier(pendingCompressedTier());
        return Math.max(1, (int) Math.ceil(base * (1.0D - reduction) * compressed));
    }

    /**
     * Reads an enchantment level from the installed mesh.
     *
     * <p>The holder is resolved through the level's registry access; enchantments have no static
     * field any more.
     */
    private int enchantmentLevel(ResourceKey<Enchantment> key) {
        ItemStack mesh = meshSlot.getStack();
        if (mesh.isEmpty() || getLevel() == null) {
            return 0;
        }
        Optional<Holder.Reference<Enchantment>> holder = getLevel().registryAccess()
                .registry(Registries.ENCHANTMENT)
                .flatMap(registry -> registry.getHolder(key));
        if (holder.isEmpty()) {
            return 0;
        }
        int level = mesh.getEnchantmentLevel(holder.get());
        if (MekExNihiloConfig.respectEnchantmentLimits()) {
            level = Math.min(level, holder.get().value().getMaxLevel());
        }
        return Math.max(0, level);
    }

    private boolean hasEnergyForTick() {
        return energyContainer.getEnergy() >= energyPerTick();
    }

    /**
     * Energy drawn per tick for the batch currently in the input slots.
     *
     * <p>Derived from the most compressed item present rather than cached when an operation starts,
     * because the idle checks run before {@link #beginOperation} and would use a stale value.
     */
    private long energyPerTick() {
        double multiplier = MekExNihiloConfig.compressedEnergyMultiplier(pendingCompressedTier());
        return Math.max(1L, (long) Math.ceil(energyContainer.getEnergyPerTick() * multiplier));
    }

    /** Compression tier of the most compressed item in the input slots, or 0 when there is none. */
    private int pendingCompressedTier() {
        int highest = 0;
        for (IInventorySlot slot : inputSlots) {
            int tier = AllTheCompressedCompat.tierOf(slot.getStack());
            if (tier > highest) {
                highest = tier;
            }
        }
        return highest;
    }

    /**
     * Structural validity for the input slots.
     *
     * <p>This is asked on both sides — the GUI checks whether a slot accepts an item while the
     * player drags it — so it must never touch recipes, which live in the server's recipe manager.
     * Whether the installed mesh can actually sift an item is decided in {@link #collectBatch} when
     * the machine runs, so an item the current mesh cannot handle simply waits in the slot.
     */
    private boolean isAcceptableInput(ItemStack stack) {
        return !stack.isEmpty() && !isBlacklisted(stack);
    }

    /** Server-side check against Ex Deorum's actual sifting recipes. */
    private boolean isSiftable(ItemStack input, ItemStack mesh) {
        if (isBlacklisted(input)) {
            return false;
        }
        // Compressed materials are resolved to the material they are made of first.
        ItemStack lookup = input;
        Item base = AllTheCompressedCompat.baseOf(input);
        if (base != null) {
            lookup = base.getDefaultInstance();
        }
        return !ExDeorumCompat.recipes(getLevel(), mesh, lookup).isEmpty();
    }

    /**
     * Whether an item is excluded by the blacklist tag.
     *
     * <p>A compressed material inherits the entry of the material it holds, so blacklisting sand also
     * blocks {@code allthecompressed:sand_1x} and up.
     */
    private boolean isBlacklisted(ItemStack stack) {
        if (stack.is(MekExNihiloTags.SIEVE_BLACKLIST)) {
            return true;
        }
        Item base = AllTheCompressedCompat.baseOf(stack);
        return base != null && base.getDefaultInstance().is(MekExNihiloTags.SIEVE_BLACKLIST);
    }

    // ------------------------------------------------------------------
    // State exposed to the menu / screen
    // ------------------------------------------------------------------

    /** The tier of the installed mesh: 1 (string) through 6 (netherite), or 0 when none is present. */
    public int getMeshTier() {
        return ExDeorumCompat.tier(meshSlot == null ? ItemStack.EMPTY : meshSlot.getStack());
    }

    /** The machine level: 0 = Sieve Machine, 1..4 = the factory tiers. */
    public int getMachineLevel() {
        return machineLevel;
    }

    /** Number of input slots this machine was built with. Every one of them is usable. */
    public int getTotalInputSlotCount() {
        return inputSlotCount;
    }

    /** Number of output slots this machine was built with. */
    public int getOutputSlotCount() {
        return outputSlotCount;
    }

    /** Input items consumed per operation with the currently installed mesh. */
    public int getBatchSize() {
        return MekExNihiloConfig.batchSizeForLevel(Math.max(1, getMeshTier()));
    }

    /**
     * How many sifting operations run in parallel each cycle. The plain sieve runs one; the factory
     * variants override this, which is what multiplies their throughput.
     */
    protected int getProcesses() {
        return 1;
    }

    /** Items actually consumed per operation, including any parallel processes. */
    public int getEffectiveBatchSize() {
        return getBatchSize() * getProcesses();
    }

    public MachineEnergyContainer<TileEntitySieve> getEnergyContainer() {
        return energyContainer;
    }

    public int getOperatingTicks() {
        return operatingTicks;
    }

    public void setOperatingTicks(int ticks) {
        operatingTicks = ticks;
    }

    public int getTicksRequired() {
        return ticksRequired;
    }

    public void setTicksRequired(int ticks) {
        ticksRequired = ticks;
    }

    /** Progress in the range 0..1, used by the GUI progress bar. */
    public double getScaledProgress() {
        return ticksRequired <= 0 ? 0.0D : operatingTicks / (double) ticksRequired;
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableInt.create(this::getOperatingTicks, this::setOperatingTicks));
        container.track(SyncableInt.create(this::getTicksRequired, this::setTicksRequired));
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt(NBT_OPERATING_TICKS, operatingTicks);
        tag.putInt(NBT_TICKS_REQUIRED, ticksRequired);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        operatingTicks = tag.getInt(NBT_OPERATING_TICKS);
        ticksRequired = tag.contains(NBT_TICKS_REQUIRED) ? tag.getInt(NBT_TICKS_REQUIRED) : MekExNihiloConfig.baseTicks();
        workDirty = true;
    }

    // ------------------------------------------------------------------
    // Wrench support
    // ------------------------------------------------------------------

    /**
     * Lets any tool in the common wrench tag dismantle this machine, on top of what Mekanism does.
     *
     * <p>Mekanism only accepts its own configurator or items exposing its wrench abilities, so
     * another mod's wrench would otherwise do nothing here.
     */
    @Override
    public WrenchResult tryWrench(BlockState state, Player player, ItemStack stack) {
        WrenchResult result = super.tryWrench(state, player, stack);
        if (result != WrenchResult.PASS || stack.isEmpty() || getLevel() == null) {
            return result;
        }
        if (player.isShiftKeyDown() && stack.is(Tags.Items.TOOLS_WRENCH)) {
            WorldUtils.dismantleBlock(state, getLevel(), getBlockPos(), this, player, stack);
            return WrenchResult.DISMANTLED;
        }
        return result;
    }

    // Tier installer support. Mekanism's installer only converts a block that declares
    // AttributeUpgradeable and whose tile hands back upgrade data, which is what keeps the machine's
    // contents across the swap.

    @Nullable
    @Override
    public IUpgradeData getUpgradeData(HolderLookup.Provider provider) {
        CompoundTag components = new CompoundTag();
        for (ITileComponent component : getComponents()) {
            component.write(components, provider);
        }
        return new SieveUpgradeData(meshSlot.getStack().copy(), copyOf(inputSlots), copyOf(outputSlots),
                energySlot.getStack().copy(), energyContainer.getEnergy(), operatingTicks, ticksRequired, components);
    }

    @Override
    public void parseUpgradeData(HolderLookup.Provider provider, @NotNull IUpgradeData data) {
        if (!(data instanceof SieveUpgradeData sieveData)) {
            super.parseUpgradeData(provider, data);
            return;
        }
        // Restore per role rather than per index: the new machine may have a different number of
        // input and output slots (for example 1 input / 12 outputs becoming 3 / 16).
        restore(meshSlot, sieveData.mesh);
        restoreAll(inputSlots, sieveData.inputs);
        restoreAll(outputSlots, sieveData.outputs);
        restore(energySlot, sieveData.energyItem);
        energyContainer.setEnergy(sieveData.energy);
        operatingTicks = sieveData.operatingTicks;
        ticksRequired = sieveData.ticksRequired;
        workDirty = true;
        for (ITileComponent component : getComponents()) {
            component.read(sieveData.components, provider);
        }
    }

    private static List<ItemStack> copyOf(List<IInventorySlot> slots) {
        List<ItemStack> contents = new ArrayList<>(slots.size());
        for (IInventorySlot slot : slots) {
            contents.add(slot.getStack().copy());
        }
        return contents;
    }

    private static void restoreAll(List<IInventorySlot> slots, List<ItemStack> contents) {
        for (int i = 0; i < slots.size(); i++) {
            restore(slots.get(i), i < contents.size() ? contents.get(i) : ItemStack.EMPTY);
        }
    }

    private static void restore(IInventorySlot slot, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            slot.setEmpty();
            return;
        }
        if (slot instanceof BasicInventorySlot basic) {
            // Restore verbatim: the item may not pass the slot's validator (a mesh arriving in an
            // input slot, say), but it was legitimately inside the machine before the upgrade.
            basic.setStackUnchecked(stack.copy());
        } else {
            slot.setStack(stack.copy());
        }
    }
}
