package com.mekexnihilo.registry;

import com.mekexnihilo.MekExNihilo;
import com.mekexnihilo.tile.TileEntitySieve;
import com.mekexnihilo.tile.TileEntitySieveFactory;
import mekanism.common.attachments.component.AttachedEjector;
import mekanism.common.attachments.component.AttachedSideConfig;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.Machine;
import mekanism.common.item.block.ItemBlockTooltip;
import mekanism.common.registration.impl.BlockDeferredRegister;
import mekanism.common.registration.impl.BlockRegistryObject;
import mekanism.common.registries.MekanismDataComponents;
import mekanism.common.resource.BlockResourceInfo;

public class MekExNihiloBlocks {

    public static final BlockDeferredRegister BLOCKS = new BlockDeferredRegister(MekExNihilo.MOD_ID);

    public static final BlockRegistryObject<BlockTile<TileEntitySieve, Machine<TileEntitySieve>>, ItemBlockTooltip<BlockTile<TileEntitySieve, Machine<TileEntitySieve>>>> ELECTRIC_SIEVE =
            BLOCKS.register("electric_sieve",
                    () -> new BlockTile<>(MekExNihiloBlockTypes.ELECTRIC_SIEVE,
                            properties -> properties.mapColor(BlockResourceInfo.STEEL.getMapColor())),
                    (block, properties) -> new ItemBlockTooltip<>(block, true, properties
                            .component(MekanismDataComponents.EJECTOR, AttachedEjector.DEFAULT)
                            .component(MekanismDataComponents.SIDE_CONFIG, AttachedSideConfig.ELECTRIC_MACHINE)));

    public static final BlockRegistryObject<BlockTile<TileEntitySieveFactory, Machine<TileEntitySieveFactory>>, ItemBlockTooltip<BlockTile<TileEntitySieveFactory, Machine<TileEntitySieveFactory>>>> BASIC_SIEVE_FACTORY =
            factory("basic_sieve_factory", MekExNihiloBlockTypes.BASIC_SIEVE_FACTORY);

    public static final BlockRegistryObject<BlockTile<TileEntitySieveFactory, Machine<TileEntitySieveFactory>>, ItemBlockTooltip<BlockTile<TileEntitySieveFactory, Machine<TileEntitySieveFactory>>>> ADVANCED_SIEVE_FACTORY =
            factory("advanced_sieve_factory", MekExNihiloBlockTypes.ADVANCED_SIEVE_FACTORY);

    public static final BlockRegistryObject<BlockTile<TileEntitySieveFactory, Machine<TileEntitySieveFactory>>, ItemBlockTooltip<BlockTile<TileEntitySieveFactory, Machine<TileEntitySieveFactory>>>> ELITE_SIEVE_FACTORY =
            factory("elite_sieve_factory", MekExNihiloBlockTypes.ELITE_SIEVE_FACTORY);

    public static final BlockRegistryObject<BlockTile<TileEntitySieveFactory, Machine<TileEntitySieveFactory>>, ItemBlockTooltip<BlockTile<TileEntitySieveFactory, Machine<TileEntitySieveFactory>> >> ULTIMATE_SIEVE_FACTORY =
            factory("ultimate_sieve_factory", MekExNihiloBlockTypes.ULTIMATE_SIEVE_FACTORY);

    private static BlockRegistryObject<BlockTile<TileEntitySieveFactory, Machine<TileEntitySieveFactory>>, ItemBlockTooltip<BlockTile<TileEntitySieveFactory, Machine<TileEntitySieveFactory>>>> factory(
            String name, Machine<TileEntitySieveFactory> type) {
        return BLOCKS.register(name,
                () -> new BlockTile<>(type, properties -> properties.mapColor(BlockResourceInfo.STEEL.getMapColor())),
                (block, properties) -> new ItemBlockTooltip<>(block, true, properties
                        .component(MekanismDataComponents.EJECTOR, AttachedEjector.DEFAULT)
                        .component(MekanismDataComponents.SIDE_CONFIG, AttachedSideConfig.ELECTRIC_MACHINE)));
    }

    private MekExNihiloBlocks() {}
}
