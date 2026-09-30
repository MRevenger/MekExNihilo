package com.mekexnihilo.registry;

import com.mekexnihilo.MekExNihiloConfig;
import com.mekexnihilo.tile.TileEntitySieve;
import com.mekexnihilo.tile.TileEntitySieveFactory;
import java.util.function.Supplier;
import mekanism.common.block.attribute.AttributeSideConfig;
import mekanism.common.block.attribute.AttributeTier;
import mekanism.common.block.attribute.AttributeUpgradeable;
import mekanism.common.content.blocktype.Machine;
import mekanism.common.content.blocktype.Machine.MachineBuilder;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.tier.FactoryTier;

/**
 * Block type descriptors.
 *
 * <p>Mekanism has no registry for these: a block type is a plain object passed to the block's
 * constructor and read back through the block's attributes.
 *
 * <p>The sieve carries an {@link AttributeUpgradeable} pointing at the basic factory, which is what
 * the tier installer needs. It has no {@link AttributeTier} on purpose: the Basic Tier Installer
 * has a {@code null} "from" tier and only matches blocks without one, as the Enrichment Chamber does.
 */
public class MekExNihiloBlockTypes {

    public static final Machine<TileEntitySieve> ELECTRIC_SIEVE = MachineBuilder
            .createMachine(() -> MekExNihiloTileEntityTypes.ELECTRIC_SIEVE, MekExNihiloLang.DESCRIPTION_ELECTRIC_SIEVE)
            .withGui(() -> MekExNihiloContainerTypes.ELECTRIC_SIEVE)
            .withEnergyConfig(MekExNihiloConfig::energyPerTick, MekExNihiloConfig::energyCapacity)
            .with(AttributeSideConfig.ELECTRIC_MACHINE)
            .with(new AttributeUpgradeable(() -> MekExNihiloBlocks.BASIC_SIEVE_FACTORY))
            .build();

    public static final Machine<TileEntitySieveFactory> BASIC_SIEVE_FACTORY = createFactory(
            FactoryTier.BASIC, () -> MekExNihiloTileEntityTypes.BASIC_SIEVE_FACTORY, () -> MekExNihiloBlocks.ADVANCED_SIEVE_FACTORY);

    public static final Machine<TileEntitySieveFactory> ADVANCED_SIEVE_FACTORY = createFactory(
            FactoryTier.ADVANCED, () -> MekExNihiloTileEntityTypes.ADVANCED_SIEVE_FACTORY, () -> MekExNihiloBlocks.ELITE_SIEVE_FACTORY);

    public static final Machine<TileEntitySieveFactory> ELITE_SIEVE_FACTORY = createFactory(
            FactoryTier.ELITE, () -> MekExNihiloTileEntityTypes.ELITE_SIEVE_FACTORY, () -> MekExNihiloBlocks.ULTIMATE_SIEVE_FACTORY);

    /** Top tier: nothing to upgrade into, so it intentionally has no upgrade attribute. */
    public static final Machine<TileEntitySieveFactory> ULTIMATE_SIEVE_FACTORY = createFactory(
            FactoryTier.ULTIMATE, () -> MekExNihiloTileEntityTypes.ULTIMATE_SIEVE_FACTORY, null);

    private static Machine<TileEntitySieveFactory> createFactory(FactoryTier tier,
            Supplier<TileEntityTypeRegistryObject<TileEntitySieveFactory>> tile,
            Supplier<mekanism.common.registration.impl.BlockRegistryObject<?, ?>> upgradeTo) {
        MachineBuilder<Machine<TileEntitySieveFactory>, TileEntitySieveFactory, ?> builder = MachineBuilder
                .createMachine(tile, MekExNihiloLang.DESCRIPTION_SIEVE_FACTORY)
                .withGui(() -> MekExNihiloContainerTypes.ELECTRIC_SIEVE)
                .withEnergyConfig(MekExNihiloConfig::energyPerTick, MekExNihiloConfig::energyCapacity)
                .with(new AttributeTier<>(tier))
                .with(AttributeSideConfig.ELECTRIC_MACHINE);
        if (upgradeTo != null) {
            builder = builder.with(new AttributeUpgradeable(upgradeTo));
        }
        return builder.build();
    }

    private MekExNihiloBlockTypes() {}
}
