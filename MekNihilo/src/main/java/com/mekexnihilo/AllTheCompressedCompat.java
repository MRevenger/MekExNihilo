package com.mekexnihilo;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * Recognition of AllTheCompressed's compressed materials.
 *
 * <p>That mod registers one item per material and tier, named
 * {@code allthecompressed:<material>_<tier>x} with each tier worth nine of the previous one, so the
 * registry name already carries everything needed. AllTheCompressed is optional in the strict
 * sense: nothing is compiled against it and nothing breaks without it.
 *
 * <p>Base materials are usually vanilla ({@code sand_1x} to {@code minecraft:sand}); other
 * namespaces are covered by a fallback search.
 */
public final class AllTheCompressedCompat {

    private static final String NAMESPACE = MekExNihiloConfig.ALL_THE_COMPRESSED_MOD_ID;
    private static final String SUFFIX = "x";

    /** A compressed item resolved to the material it is made of and how many times it is compressed. */
    public record Compressed(Item base, int tier) {}

    /** Cache keyed by the compressed item, since an item's id never changes once registered. */
    private static final Map<Item, Compressed> CACHE = new HashMap<>();

    /** Lazily built index of every item whose path could be a base material. */
    @Nullable
    private static Map<String, Item> itemsByPath;

    private AllTheCompressedCompat() {}

    /**
     * Resolves a compressed stack, or returns {@code null} when the stack is not one, when
     * compressed sifting is disabled, or when the base material is unknown.
     */
    @Nullable
    public static Compressed resolve(ItemStack stack) {
        if (stack.isEmpty() || !MekExNihiloConfig.compressedSiftingEnabled()) {
            return null;
        }
        Item item = stack.getItem();
        if (CACHE.containsKey(item)) {
            return CACHE.get(item);
        }
        Compressed resolved = parse(item);
        CACHE.put(item, resolved);
        return resolved;
    }

    /** The compression tier of a stack, or 0 when it is not a compressed material. */
    public static int tierOf(ItemStack stack) {
        Compressed compressed = resolve(stack);
        return compressed == null ? 0 : compressed.tier();
    }

    /** The material a compressed stack is made of, or {@code null}. */
    @Nullable
    public static Item baseOf(ItemStack stack) {
        Compressed compressed = resolve(stack);
        return compressed == null ? null : compressed.base();
    }

    /** Drops the cache; only needed if the config toggles at runtime. */
    public static void invalidate() {
        CACHE.clear();
        itemsByPath = null;
    }

    @Nullable
    private static Compressed parse(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (!NAMESPACE.equals(id.getNamespace())) {
            return null;
        }
        String path = id.getPath();
        if (!path.endsWith(SUFFIX)) {
            return null;
        }
        // Walk back over the tier digits, which must sit between an underscore and the trailing "x".
        int lastDigit = path.length() - SUFFIX.length() - 1;
        int start = lastDigit;
        while (start >= 0 && Character.isDigit(path.charAt(start))) {
            start--;
        }
        if (start == lastDigit || start < 0 || path.charAt(start) != '_') {
            return null;
        }
        int tier;
        try {
            tier = Integer.parseInt(path.substring(start + 1, lastDigit + 1));
        } catch (NumberFormatException e) {
            return null;
        }
        if (tier < 1) {
            return null;
        }
        String material = path.substring(0, start);
        if (material.isEmpty()) {
            return null;
        }
        Item base = findBase(material);
        return base == null ? null : new Compressed(base, tier);
    }

    /** The item a material name refers to: vanilla first, then any other namespace. */
    @Nullable
    private static Item findBase(String material) {
        Item vanilla = BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(material));
        if (vanilla != Items.AIR) {
            return vanilla;
        }
        Map<String, Item> index = itemsByPath;
        if (index == null) {
            index = new HashMap<>();
            for (Item item : BuiltInRegistries.ITEM) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                if (item == Items.AIR || NAMESPACE.equals(id.getNamespace())) {
                    continue;
                }
                // First registration wins, which keeps the result stable across runs.
                index.putIfAbsent(id.getPath(), item);
            }
            itemsByPath = index;
        }
        return index.get(material);
    }
}
