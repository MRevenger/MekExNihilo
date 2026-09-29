package com.mekexnihilo.registry;

import com.mekexnihilo.MekExNihilo;
import com.mekexnihilo.inventory.container.SieveContainer;
import com.mekexnihilo.tile.TileEntitySieve;
import mekanism.common.registration.impl.ContainerTypeDeferredRegister;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;

public class MekExNihiloContainerTypes {

    public static final ContainerTypeDeferredRegister CONTAINER_TYPES = new ContainerTypeDeferredRegister(MekExNihilo.MOD_ID);

    /**
     * One menu serves the sieve and every factory tier.
     *
     * <p>The tile class is only used to locate the block entity on the client, and Mekanism's
     * {@code WorldUtils.getTileEntity} matches with {@code isInstance}, so registering the base
     * {@link TileEntitySieve} also resolves the factory subclasses.
     */
    public static final ContainerTypeRegistryObject<SieveContainer> ELECTRIC_SIEVE =
            CONTAINER_TYPES.register("electric_sieve", TileEntitySieve.class, SieveContainer::new);

    private MekExNihiloContainerTypes() {}
}
