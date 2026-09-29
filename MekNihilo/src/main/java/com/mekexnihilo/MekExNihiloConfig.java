package com.mekexnihilo;

import java.util.ArrayList;
import java.util.List;
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
 */
public final class MekExNihiloConfig {

    public static final ModConfigSpec SPEC;

    // --- machine ---
    public static final ModConfigSpec.IntValue OUTPUT_SLOTS;
    public static final ModConfigSpec.IntValue OUTPUT_SLOTS_PER_TIER;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> INPUT_SLOTS;
    public static final ModConfigSpec.IntValue BASE_TICKS;
    public static final ModConfigSpec.DoubleValue EFFICIENCY_REDUCTION_PER_LEVEL;
    public static final ModConfigSpec.DoubleValue FORTUNE_BONUS_PER_LEVEL;
    public static final ModConfigSpec.LongValue ENERGY_PER_TICK;
    public static final ModConfigSpec.LongValue ENERGY_CAPACITY;
    public static final ModConfigSpec.BooleanValue DAMAGE_MESH;
    public static final ModConfigSpec.BooleanValue SIFT_BY_HAND_ONLY_RECIPES;
    public static final ModConfigSpec.BooleanValue RESPECT_ENCHANTMENT_LIMITS;

    // --- per mesh tier ---
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> BATCH_SIZES;

    /** Level of the plain Sieve Machine; the factory tiers are 1..4. */
    public static final int BASE_MACHINE_LEVEL = 0;
    /** Number of factory tiers above the plain machine. */
    public static final int FACTORY_TIERS = 4;

    private static final int FALLBACK_INPUT_SLOTS = 1;
    private static final int FALLBACK_BATCH = 1;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment(
                        "Settings for the Sieve Machine and its factory tiers.",
                        "Slot counts are shared by client and server, so both sides must use the same values.",
                        "Slot counts are applied when a machine is built: break and replace an existing machine",
                        "to pick up a changed value.")
                .push("machine");

        OUTPUT_SLOTS = builder
                .comment("Output slots of the plain Sieve Machine.",
                        "Each factory tier adds outputSlotsPerTier more, so the Ultimate Sieve Factory has",
                        "outputSlots + 4 * outputSlotsPerTier.")
                .defineInRange("outputSlots", 12, 5, 60);

        OUTPUT_SLOTS_PER_TIER = builder
                .comment("Extra output slots granted per factory tier.")
                .defineInRange("outputSlotsPerTier", 4, 0, 20);

        INPUT_SLOTS = builder
                .comment("Input slots for each machine level, weakest first:",
                        "Sieve Machine, Basic Sieve Factory, Advanced Sieve Factory,",
                        "Elite Sieve Factory, Ultimate Sieve Factory.",
                        "Defaults: 1, 3, 4, 5, 8. Range per entry: 1 - 12.")
                .defineList(
                        "inputSlots",
                        List.of(1, 3, 4, 5, 8),
                        () -> 1,
                        o -> o instanceof Integer i && i >= 1 && i <= 12);

        BASE_TICKS = builder
                .comment("Ticks required for one sieving operation with no Efficiency enchantment.")
                .defineInRange("baseTicks", 100, 1, 72000);

        EFFICIENCY_REDUCTION_PER_LEVEL = builder
                .comment(
                        "Fraction of processing time removed per level of Efficiency on the mesh.",
                        "0.05 = -5% per level. The total reduction is capped at 100% (never below 1 tick).")
                .defineInRange("efficiencyReductionPerLevel", 0.05D, 0.0D, 1.0D);

        FORTUNE_BONUS_PER_LEVEL = builder
                .comment(
                        "Relative increase of the drop chance per level of Fortune on the mesh.",
                        "0.20 = +20% yield per level. Range: 0.05 - 0.50.")
                .defineInRange("fortuneBonusPerLevel", 0.20D, 0.05D, 0.50D);

        ENERGY_PER_TICK = builder
                .comment("Energy consumed per tick while the machine is running.")
                .defineInRange("energyPerTick", 200L, 1L, Long.MAX_VALUE);

        ENERGY_CAPACITY = builder
                .comment("Internal energy buffer size.")
                .defineInRange("energyCapacity", 40000L, 1L, Long.MAX_VALUE);

        DAMAGE_MESH = builder
                .comment(
                        "Whether the installed mesh takes durability damage, like the vanilla sieve does.",
                        "Disabled by default so that meshes last forever in the machine.")
                .define("damageMesh", false);

        SIFT_BY_HAND_ONLY_RECIPES = builder
                .comment(
                        "Ex Deorum marks some sifting recipes as 'by_hand_only'.",
                        "false - the machine skips them, exactly like Ex Deorum's own Mechanical Sieve.",
                        "true  - the machine also processes them.")
                .define("siftByHandOnlyRecipes", false);

        RESPECT_ENCHANTMENT_LIMITS = builder
                .comment(
                        "true  - Efficiency and Fortune are capped at their vanilla maximum level.",
                        "false - Enchantment levels above the vanilla cap keep stacking. This is the default",
                        "        because Efficiency is meant to reach its 100% processing-time reduction cap;",
                        "        the reduction can still never exceed 100%.")
                .define("respectEnchantmentLimits", false);

        builder.pop();

        builder.comment(
                        "Per mesh tier settings.",
                        "Index 1 = the weakest mesh (String), index 6 = the strongest (Netherite).",
                        "The tier is taken from Ex Deorum's own mesh ordering, so the list always lines up",
                        "with the meshes Ex Deorum actually registers.")
                .push("tiers");

        BATCH_SIZES = builder
                .comment(
                        "How many input items a single operation may consume, per mesh tier.",
                        "Defaults: 1, 4, 8, 16, 32, 64.")
                .defineList(
                        "batchSizes",
                        List.of(1, 4, 8, 16, 32, 64),
                        () -> 1,
                        o -> o instanceof Integer i && i >= 1 && i <= 64);

        builder.pop();

        SPEC = builder.build();
    }

    private MekExNihiloConfig() {}

    private static int clamp(int level, int min, int max) {
        return Math.min(Math.max(level, min), max);
    }

    /** Number of input slots for the given machine level (0 = plain machine, 4 = ultimate factory). */
    public static int inputSlotsForMachineLevel(int machineLevel) {
        List<? extends Integer> list = INPUT_SLOTS.get();
        int index = clamp(machineLevel, 0, list.size() - 1);
        Integer value = list.get(index);
        return value == null ? FALLBACK_INPUT_SLOTS : clamp(value, 1, 12);
    }

    /** Number of output slots for the given machine level. */
    public static int outputSlotsForMachineLevel(int machineLevel) {
        int level = clamp(machineLevel, 0, FACTORY_TIERS);
        return OUTPUT_SLOTS.get() + level * OUTPUT_SLOTS_PER_TIER.get();
    }

    /** Number of input items one operation consumes at the given mesh tier. */
    public static int batchSizeForLevel(int level) {
        List<? extends Integer> list = BATCH_SIZES.get();
        if (level < 1 || level > list.size()) {
            return FALLBACK_BATCH;
        }
        Integer value = list.get(level - 1);
        return value == null ? FALLBACK_BATCH : clamp(value, 1, 64);
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

    /**
     * Writes the values that were actually loaded to the log.
     *
     * <p>Called once the server is up, so a pack author can confirm from `latest.log` whether the
     * settings in {@code config/mekexnihilo-common.toml} were picked up instead of guessing.
     */
    public static void logLoadedValues() {
        MekExNihilo.LOGGER.info("Config file: {}", FMLPaths.CONFIGDIR.get().resolve(MekExNihilo.MOD_ID + "-common.toml"));
        MekExNihilo.LOGGER.info(
                "Config: outputSlots={} outputSlotsPerTier={} inputSlots={} baseTicks={} efficiencyPerLevel={} fortunePerLevel={} energyPerTick={} batchSizes={}",
                OUTPUT_SLOTS.get(), OUTPUT_SLOTS_PER_TIER.get(), INPUT_SLOTS.get(), BASE_TICKS.get(),
                EFFICIENCY_REDUCTION_PER_LEVEL.get(), FORTUNE_BONUS_PER_LEVEL.get(), ENERGY_PER_TICK.get(),
                BATCH_SIZES.get());
        List<Integer> inputs = new ArrayList<>();
        List<Integer> outputs = new ArrayList<>();
        for (int level = 0; level <= FACTORY_TIERS; level++) {
            inputs.add(inputSlotsForMachineLevel(level));
            outputs.add(outputSlotsForMachineLevel(level));
        }
        MekExNihilo.LOGGER.info("Config: resolved slot counts for machine levels 0..{}: inputs={} outputs={}",
                FACTORY_TIERS, inputs, outputs);
    }
}
