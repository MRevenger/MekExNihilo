package com.mekexnihilo;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Tags owned by this mod. */
public final class MekExNihiloTags {

    /**
     * Items in this tag are never sifted and cannot be inserted into the input slots. It is a plain
     * item tag, so a datapack or a KubeJS script can fill it:
     *
     * <pre>{@code
     * ServerEvents.tags('item', event => {
     *     event.add('mekexnihilo:sieve_blacklist', 'minecraft:gravel')
     *     event.add('mekexnihilo:sieve_blacklist', '#minecraft:sand')
     * })
     * }</pre>
     */
    public static final TagKey<Item> SIEVE_BLACKLIST =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MekExNihilo.MOD_ID, "sieve_blacklist"));

    private MekExNihiloTags() {}
}
