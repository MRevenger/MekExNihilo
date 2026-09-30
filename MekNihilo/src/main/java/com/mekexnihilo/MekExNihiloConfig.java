package com.mekexnihilo;

import java.util.List;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * All tunables for the Sieve Machine and its factory tiers.
 *
 * <p>The config is {@link net.neoforged.neoforge.common.ModConfig.Type#COMMON} on purpose: the
 * number of input/output slots has to be identical on the client and the server because the menu
 * layout is derived from it.
 *
 * <p>Slot counts are read every time a machine is built, never cached in a static field, so a
 * changed config file takes effect on the next machine that is placed or loaded. Machines that are
 * already in the world keep the layout they were built with; break and replace them to pick up a
 * new slot count.
 *
 * <p>The {@code compressed} section only exists when AllTheCompressed is installed. That mod is a
 * purely optional companion, so without it the settings are neither written nor shown.
 */
public final class MekExNihiloConfig {

    public static final ModConfigSpec SPEC;

    /** Mod id of AllTheCompressed, the optional companion whose materials can be sifted. */
    public static final String ALL_THE_COMPRESSED_MOD_ID = "allthecompressed";

    /** Whether that companion is present; decided once, while the spec is being built. */
    private static final boolean COMPRESSED_AVAILABLE = ModList.get().isLoaded(ALL_THE_COMPRESSED_MOD_ID);

    // --- machine ---
    public static final ModConfigSpec.IntValue OUTPUT_SLOTS;
    public static final ModConfigSpec.IntValue OUTPUT_SLOT_LIMIT;
    public static final ModConfigSpec.IntValue OUTPUT_SLOTS_PER_TIER;
    public static final ModConfigSpec.IntValue BASE_TICKS;
    public static final ModConfigSpec.DoubleValue EFFICIENCY_REDUCTION_PER_LEVEL;
    public static final ModConfigSpec.DoubleValue FORTUNE_BONUS_PER_LEVEL;
    public static final ModConfigSpec.LongValue ENERGY_PER_TICK;
    public static final ModConfigSpec.LongValue ENERGY_CAPACITY;
    public static final ModConfigSpec.BooleanValue DAMAGE_MESH;
    public static final ModConfigSpec.BooleanValue SIFT_BY_HAND_ONLY_RECIPES;
    public static final ModConfigSpec.BooleanValue RESPECT_ENCHANTMENT_LIMITS;
    public static final ModConfigSpec.BooleanValue DYNAMIC_EJECT;

    // --- AllTheCompressed compatibility (absent when the mod is not installed) ---
    public static final ModConfigSpec.BooleanValue ENABLE_COMPRESSED_SIFTING;
    public static final ModConfigSpec.DoubleValue COMPRESSED_YIELD_BASE;
    public static final ModConfigSpec.DoubleValue COMPRESSED_TIME_BASE;
    public static final ModConfigSpec.DoubleValue COMPRESSED_ENERGY_BASE;
    public static final ModConfigSpec.IntValue MAX_COMPRESSED_REPEATS;

    /** Level of the plain Sieve Machine; the factory tiers are 1..4. */
    public static final int BASE_MACHINE_LEVEL = 0;
    /** Number of factory tiers above the plain machine. */
    public static final int FACTORY_TIERS = 4;

    /** Highest number of items one output slot can hold. */
    public static final int MAX_OUTPUT_SLOT_LIMIT = 8192;

    /**
     * Input slots per machine level: Sieve Machine, Basic, Advanced, Elite, Ultimate Sieve Factory.
     * Fixed rather than configurable, because the GUI layout and the menu are derived from it.
     */
    private static final List<Integer> MACHINE_INPUT_SLOTS = List.of(1, 3, 4, 5, 6);

    /**
     * Items consumed per operation, per mesh tier, weakest first (string .. netherite). Also fixed:
     * it is the single number that defines how strong each mesh is.
     */
    private static final List<Integer> MESH_BATCH_SIZES = List.of(1, 2, 4, 16, 32, 64);

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment(
                        "Settings for the Sieve Machine and its factory tiers.",
                        "Slot counts are shared by client and server, so both sides must use the same values.",
                        "Slot counts are applied when a machine is built: break and replace an existing machine",
                        "to pick up a changed value.")
                .translation("mekexnihilo.configuration.machine")
                .push("machine");

        OUTPUT_SLOTS = builder
                .comment("Output slots of the plain Sieve Machine.",
                        "Each factory tier adds outputSlotsPerTier more, so the Ultimate Sieve Factory has",
                        "outputSlots + 4 * outputSlotsPerTier.")
                .translation("mekexnihilo.configuration.machine.outputSlots")
                .defineInRange("outputSlots", 12, 5, 60);

        OUTPUT_SLOTS_PER_TIER = builder
                .comment("Extra output slots granted per factory tier.")
                .translation("mekexnihilo.configuration.machine.outputSlotsPerTier")
                .defineInRange("outputSlotsPerTier", 4, 0, 20);

        OUTPUT_SLOT_LIMIT = builder
                .comment("Maximum number of items a single output slot can hold.",
                        "Compressed sifting multiplies the yield heavily, so a slot needs room for far more",
                        "than a vanilla stack of 64. Mekanism stores such oversized stacks correctly and",
                        "NeoForge's hard ceiling is 99 per ItemStack, which this deliberately exceeds by",
                        "using Mekanism's oversized-stack serialisation.",
                        "Range: 64 - 8192.")
                .translation("mekexnihilo.configuration.machine.outputSlotLimit")
                .defineInRange("outputSlotLimit", MAX_OUTPUT_SLOT_LIMIT, 64, MAX_OUTPUT_SLOT_LIMIT);

        BASE_TICKS = builder
                .comment("Ticks required for one sieving operation with no Efficiency enchantment.")
                .translation("mekexnihilo.configuration.machine.baseTicks")
                .defineInRange("baseTicks", 100, 1, 72000);

        EFFICIENCY_REDUCTION_PER_LEVEL = builder
                .comment(
                        "Fraction of processing time removed per level of Efficiency on the mesh.",
                        "0.05 = -5% per level. The total reduction is capped at 100% (never below 1 tick).")
                .translation("mekexnihilo.configuration.machine.efficiencyReductionPerLevel")
                .defineInRange("efficiencyReductionPerLevel", 0.05D, 0.0D, 1.0D);

        FORTUNE_BONUS_PER_LEVEL = builder
                .comment(
                        "Relative increase of the drop chance per level of Fortune on the mesh.",
                        "0.20 = +20% yield per level. Range: 0.05 - 0.50.")
                .translation("mekexnihilo.configuration.machine.fortuneBonusPerLevel")
                .defineInRange("fortuneBonusPerLevel", 0.20D, 0.05D, 0.50D);

        ENERGY_PER_TICK = builder
                .comment("Energy consumed per tick while the machine is running.")
                .translation("mekexnihilo.configuration.machine.energyPerTick")
                .defineInRange("energyPerTick", 200L, 1L, Long.MAX_VALUE);

        ENERGY_CAPACITY = builder
                .comment("Internal energy buffer size.")
                .translation("mekexnihilo.configuration.machine.energyCapacity")
                .defineInRange("energyCapacity", 40000L, 1L, Long.MAX_VALUE);

        DAMAGE_MESH = builder
                .comment(
                        "Whether the installed mesh takes durability damage, like the vanilla sieve does.",
                        "Disabled by default so that meshes last forever in the machine.")
                .translation("mekexnihilo.configuration.machine.damageMesh")
                .define("damageMesh", false);

        SIFT_BY_HAND_ONLY_RECIPES = builder
                .comment(
                        "Ex Deorum marks some sifting recipes as 'by_hand_only'.",
                        "false - the machine skips them, exactly like Ex Deorum's own Mechanical Sieve.",
                        "true  - the machine also processes them.")
                .translation("mekexnihilo.configuration.machine.siftByHandOnlyRecipes")
                .define("siftByHandOnlyRecipes", false);

        RESPECT_ENCHANTMENT_LIMITS = builder
                .comment(
                        "true  - Efficiency and Fortune are capped at their vanilla maximum level.",
                        "false - Enchantment levels above the vanilla cap keep stacking. This is the default",
                        "        because Efficiency is meant to reach its 100% processing-time reduction cap;",
                        "        the reduction can still never exceed 100%.")
                .translation("mekexnihilo.configuration.machine.respectEnchantmentLimits")
                .define("respectEnchantmentLimits", false);

        DYNAMIC_EJECT = builder
                .comment(
                        "Push the output slots into adjacent inventories every tick.",
                        "Each tick one whole output slot is moved, starting at the first non-empty one.",
                        "With this on, a machine that has not managed to eject anything for three minutes is",
                        "treated as idle and only tries once per second, which keeps large farms from costing",
                        "a full eject attempt every tick. A successful eject resets that timer.",
                        "Turn it off to always attempt an eject every tick.")
                .translation("mekexnihilo.configuration.machine.dynamicEject")
                .define("dynamicEject", true);

        builder.pop();

        if (COMPRESSED_AVAILABLE) {
            builder.comment(
                            "AllTheCompressed compatibility.",
                            "AllTheCompressed adds items like allthecompressed:sand_1x .. sand_9x, each tier being",
                            "nine of the previous one. With this enabled the machine recognises such an item and",
                            "repeats the sifting event of its base material, scaling time and energy as well.",
                            "This section only exists while AllTheCompressed is installed.")
                    .translation("mekexnihilo.configuration.compressed")
                    .push("compressed");

            ENABLE_COMPRESSED_SIFTING = builder
                    .comment("Sift compressed materials, repeating the base sifting event by tier.")
                    .translation("mekexnihilo.configuration.compressed.enableCompressedSifting")
                    .define("enableCompressedSifting", true);

            COMPRESSED_YIELD_BASE = builder
                    .comment("Yield base. A tier N item repeats the base sifting event round(base^N) times.",
                            "9.0 means sand_1x repeats it nine times, sand_2x eighty-one and sand_3x 729,",
                            "matching how many base items a compressed block is worth.")
                    .translation("mekexnihilo.configuration.compressed.compressedYieldBase")
                    .defineInRange("compressedYieldBase", 9.0D, 1.0D, 64.0D);

            COMPRESSED_TIME_BASE = builder
                    .comment("Processing time multiplier base. A tier N item takes base^N times as long.")
                    .translation("mekexnihilo.configuration.compressed.compressedTimeBase")
                    .defineInRange("compressedTimeBase", 1.5D, 1.0D, 64.0D);

            COMPRESSED_ENERGY_BASE = builder
                    .comment("Energy multiplier base. A tier N item draws base^N times the energy per tick.")
                    .translation("mekexnihilo.configuration.compressed.compressedEnergyBase")
                    .defineInRange("compressedEnergyBase", 1.5D, 1.0D, 64.0D);

            MAX_COMPRESSED_REPEATS = builder
                    .comment("Safety cap on how many times one operation may repeat the sifting event.",
                            "The count grows exponentially, so a high tier with a large base would otherwise",
                            "roll millions of times in a single tick and stall the server. Anything above this",
                            "is clamped, which only matters for tiers whose yield could not fit in the output",
                            "slots anyway. Range: 1 - 65536.")
                    .translation("mekexnihilo.configuration.compressed.maxCompressedRepeats")
                    .defineInRange("maxCompressedRepeats", 4096, 1, 65536);

            builder.pop();
        } else {
            ENABLE_COMPRESSED_SIFTING = null;
            COMPRESSED_YIELD_BASE = null;
            COMPRESSED_TIME_BASE = null;
            COMPRESSED_ENERGY_BASE = null;
            MAX_COMPRESSED_REPEATS = null;
        }

        SPEC = builder.build();
    }

    private MekExNihiloConfig() {}

    private static int clamp(int level, int min, int max) {
        return Math.min(Math.max(level, min), max);
    }

    /** Whether the compressed-material settings exist at all, which requires AllTheCompressed. */
    public static boolean compressedAvailable() {
        return COMPRESSED_AVAILABLE;
    }

    /** Number of input slots for the given machine level (0 = plain machine, 4 = ultimate factory). */
    public static int inputSlotsForMachineLevel(int machineLevel) {
        return MACHINE_INPUT_SLOTS.get(clamp(machineLevel, 0, MACHINE_INPUT_SLOTS.size() - 1));
    }

    /** Number of output slots for the given machine level. */
    public static int outputSlotsForMachineLevel(int machineLevel) {
        int level = clamp(machineLevel, 0, FACTORY_TIERS);
        return OUTPUT_SLOTS.get() + level * OUTPUT_SLOTS_PER_TIER.get();
    }

    /** Number of input items one operation consumes at the given mesh tier. */
    public static int batchSizeForLevel(int level) {
        if (level < 1 || level > MESH_BATCH_SIZES.size()) {
            return MESH_BATCH_SIZES.get(0);
        }
        return MESH_BATCH_SIZES.get(level - 1);
    }

    public static int baseTicks() {
        return BASE_TICKS.get();
    }

    public static double efficiencyReductionPerLevel() {
        return EFFICIENCY_REDUCTION_PER_LEVEL.get();
    }

    public static double fortuneBonusPerLevel() {
        return FORTUNE_BONUS_PER_LEVEL.get();
    }

    public static long energyPerTick() {
        return ENERGY_PER_TICK.get();
    }

    public static long energyCapacity() {
        return ENERGY_CAPACITY.get();
    }

    public static boolean damageMesh() {
        return DAMAGE_MESH.get();
    }

    public static boolean siftByHandOnlyRecipes() {
        return SIFT_BY_HAND_ONLY_RECIPES.get();
    }

    public static boolean respectEnchantmentLimits() {
        return RESPECT_ENCHANTMENT_LIMITS.get();
    }

    /** Whether a machine that has been idle for a while throttles its eject attempts. */
    public static boolean dynamicEject() {
        return DYNAMIC_EJECT.get();
    }

    /** How many items one output slot may hold. */
    public static int outputSlotLimit() {
        return clamp(OUTPUT_SLOT_LIMIT.get(), 64, MAX_OUTPUT_SLOT_LIMIT);
    }

    public static boolean compressedSiftingEnabled() {
        return COMPRESSED_AVAILABLE && ENABLE_COMPRESSED_SIFTING.get();
    }

    /** base^tier; tiers below 1 mean "not compressed", and the mod may be absent entirely. */
    private static double compressedMultiplier(ModConfigSpec.DoubleValue base, int tier) {
        if (tier < 1 || !COMPRESSED_AVAILABLE || base == null) {
            return 1.0D;
        }
        return Math.pow(base.get(), tier);
    }

    public static double compressedYieldMultiplier(int tier) {
        return compressedMultiplier(COMPRESSED_YIELD_BASE, tier);
    }

    public static double compressedTimeMultiplier(int tier) {
        return compressedMultiplier(COMPRESSED_TIME_BASE, tier);
    }

    public static double compressedEnergyMultiplier(int tier) {
        return compressedMultiplier(COMPRESSED_ENERGY_BASE, tier);
    }

    /** Upper bound on the number of sifting events one compressed operation may run. */
    public static int maxCompressedRepeats() {
        if (!COMPRESSED_AVAILABLE || MAX_COMPRESSED_REPEATS == null) {
            return 1;
        }
        return clamp(MAX_COMPRESSED_REPEATS.get(), 1, 65536);
    }

    /**
     * Writes the values that were actually loaded to the log.
     *
     * <p>Called once the server is up, so a pack author can confirm from `latest.log` whether the
     * settings in {@code config/mekexnihilo-common.toml} were picked up instead of guessing.
     */
    public static void logLoadedValues() {
        MekExNihilo.LOGGER.info("Config file: {}", FMLPaths.CONFIGDIR.get().resolve(MekExNihilo.MOD_ID + "-common.toml"));
        MekExNihilo.LOGGER.info(
                "Config: outputSlots={} outputSlotsPerTier={} outputSlotLimit={} baseTicks={} efficiencyPerLevel={} fortunePerLevel={} energyPerTick={} dynamicEject={}",
                OUTPUT_SLOTS.get(), OUTPUT_SLOTS_PER_TIER.get(), outputSlotLimit(), BASE_TICKS.get(),
                EFFICIENCY_REDUCTION_PER_LEVEL.get(), FORTUNE_BONUS_PER_LEVEL.get(), ENERGY_PER_TICK.get(),
                dynamicEject());
        MekExNihilo.LOGGER.info("Config: fixed input slots per machine level 0..{}: {}",
                FACTORY_TIERS, MACHINE_INPUT_SLOTS);
        MekExNihilo.LOGGER.info("Config: fixed items per operation by mesh tier: {}", MESH_BATCH_SIZES);
        if (COMPRESSED_AVAILABLE) {
            MekExNihilo.LOGGER.info(
                    "Config: compressed sifting={} yieldBase={} timeBase={} energyBase={} maxRepeats={}",
                    compressedSiftingEnabled(), COMPRESSED_YIELD_BASE.get(), COMPRESSED_TIME_BASE.get(),
                    COMPRESSED_ENERGY_BASE.get(), maxCompressedRepeats());
        } else {
            MekExNihilo.LOGGER.info("Config: AllTheCompressed not installed, so compressed sifting is unavailable");
        }
    }
}
