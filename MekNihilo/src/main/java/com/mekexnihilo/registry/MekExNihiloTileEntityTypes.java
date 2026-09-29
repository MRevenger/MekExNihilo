package com.mekexnihilo.registry;

import com.mekexnihilo.MekExNihilo;
import com.mekexnihilo.tile.TileEntitySieve;
import com.mekexnihilo.tile.TileEntitySieveFactory;
import mekanism.common.registration.impl.TileEntityTypeDeferredRegister;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.tier.FactoryTier;
import mekanism.common.tile.base.TileEntityMekanism;

public class MekExNihiloTileEntityTypes {

    public static final TileEntityTypeDeferredRegister TILE_ENTITY_TYPES = new TileEntityTypeDeferredRegister(MekExNihilo.MOD_ID);

    public static final TileEntityTypeRegistryObject<TileEntitySieve> ELECTRIC_SIEVE =
            TILE_ENTITY_TYPES.mekBuilder(MekExNihiloBlocks.ELECTRIC_SIEVE,
                            (pos, state) -> new TileEntitySieve(MekExNihiloBlocks.ELECTRIC_SIEVE, pos, state))
                    // These tickers are what actually drive onUpdateServer/onUpdateClient.
                    .clientTicker(TileEntityMekanism::tickClient)
                    .serverTicker(TileEntityMekanism::tickServer)
                    .build();

    public static final TileEntityTypeRegistryObject<TileEntitySieveFactory> BASIC_SIEVE_FACTORY =
            factory(MekExNihiloBlocks.BASIC_SIEVE_FACTORY, FactoryTier.BASIC);

    public static final TileEntityTypeRegistryObject<TileEntitySieveFactory> ADVANCED_SIEVE_FACTORY =
            factory(MekExNihiloBlocks.ADVANCED_SIEVE_FACTORY, FactoryTier.ADVANCED);

    public static final TileEntityTypeRegistryObject<TileEntitySieveFactory> ELITE_SIEVE_FACTORY =
            factory(MekExNihiloBlocks.ELITE_SIEVE_FACTORY, FactoryTier.ELITE);

    public static final TileEntityTypeRegistryObject<TileEntitySieveFactory> ULTIMATE_SIEVE_FACTORY =
            factory(MekExNihiloBlocks.ULTIMATE_SIEVE_FACTORY, FactoryTier.ULTIMATE);

    private static TileEntityTypeRegistryObject<TileEntitySieveFactory> factory(
            mekanism.common.registration.impl.BlockRegistryObject<?, ?> block, FactoryTier tier) {
        return TILE_ENTITY_TYPES.mekBuilder(block,
                        (pos, state) -> new TileEntitySieveFactory(block, pos, state, tier.processes))
                .clientTicker(TileEntityMekanism::tickClient)
                .serverTicker(TileEntityMekanism::tickServer)
                .build();
    }

    private MekExNihiloTileEntityTypes() {}
}
