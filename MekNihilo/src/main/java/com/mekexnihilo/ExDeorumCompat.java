package com.mekexnihilo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import org.jetbrains.annotations.Nullable;
import thedarkcolour.exdeorum.recipe.sieve.SieveRecipe;
import thedarkcolour.exdeorum.registry.EItems;
import thedarkcolour.exdeorum.registry.ERecipeTypes;
import thedarkcolour.exdeorum.tag.EItemTags;

/**
 * Everything that touches Ex Deorum lives here.
 *
 * <p>A mesh is a plain {@link Item}, not an enum, so its tier is its position in Ex Deorum's own
 * ordering: string, flint, iron, golden, diamond, netherite.
 *
 * <p>Recipes are read from the vanilla {@link RecipeManager} through Ex Deorum's {@code
 * exdeorum:sieve} recipe type rather than through {@code RecipeUtil}. That class changed shape
 * between releases: 3.x has {@code RecipeUtil.getSieveRecipes(mesh, stack)}, 3.12 replaced it with
 * {@code RecipeUtil.getCaches(level).getSieveRecipes(...)}. Calling either one directly crashes with
 * {@link NoSuchMethodError} on the other version. Going through the recipe type also picks up
 * recipes added or removed by a datapack or a KubeJS script, which Ex Deorum's own cache does not.
 */
public final class ExDeorumCompat {

    /**
     * Lazily resolved because the deferred holders must not be dereferenced before Ex Deorum has
     * finished registering its items.
     */
    @Nullable
    private static List<Item> meshOrder;

    @Nullable
    private static RecipeManager cachedManager;
    private static Map<Item, Map<Item, List<SieveRecipe>>> recipesByMesh = Map.of();

    private ExDeorumCompat() {}

    /** Ex Deorum's meshes, weakest first. */
    public static List<Item> meshes() {
        List<Item> order = meshOrder;
        if (order == null) {
            order = List.of(
                    EItems.STRING_MESH.get(),
                    EItems.FLINT_MESH.get(),
                    EItems.IRON_MESH.get(),
                    EItems.GOLDEN_MESH.get(),
                    EItems.DIAMOND_MESH.get(),
                    EItems.NETHERITE_MESH.get());
            meshOrder = order;
        }
        return order;
    }

    /** True for anything in Ex Deorum's {@code exdeorum:sieve_meshes} tag. */
    public static boolean isMesh(ItemStack stack) {
        return !stack.isEmpty() && stack.is(EItemTags.SIEVE_MESHES);
    }

    /**
     * The mesh's tier, 1 (string) through 6 (netherite), or 0 if the stack is not a known mesh.
     * Meshes added by datapacks or addons fall back to tier 1.
     */
    public static int tier(ItemStack stack) {
        if (!isMesh(stack)) {
            return 0;
        }
        int index = meshes().indexOf(stack.getItem());
        return index < 0 ? 1 : index + 1;
    }

    /**
     * The sifting recipes for one input item with one mesh, or an empty list when the mesh cannot
     * sift it.
     *
     * <p>Server side only: recipes live in the server's recipe manager. The client deliberately gets
     * an empty list so that nothing on the render thread ever touches server state — the slot
     * validator and the GUI do not consult recipes at all.
     */
    public static List<SieveRecipe> recipes(Level level, ItemStack mesh, ItemStack input) {
        if (level == null || level.isClientSide() || mesh.isEmpty() || input.isEmpty()) {
            return List.of();
        }
        MinecraftServer server = level.getServer();
        if (server == null) {
            return List.of();
        }
        RecipeManager manager = server.getRecipeManager();
        if (manager != cachedManager) {
            rebuild(manager);
        }
        Map<Item, List<SieveRecipe>> byInput = recipesByMesh.get(mesh.getItem());
        if (byInput == null) {
            return List.of();
        }
        List<SieveRecipe> found = byInput.get(input.getItem());
        return found == null ? List.of() : found;
    }

    /** Drops the cache so the next lookup rebuilds from the current recipe manager. */
    public static void invalidate() {
        cachedManager = null;
        recipesByMesh = Map.of();
    }

    /**
     * The empty loot context Ex Deorum uses when rolling a sieve recipe.
     *
     * <p>Built here rather than delegating to Ex Deorum's helper so that this addon depends on
     * nothing but Ex Deorum's registries and recipe classes, which are the parts that have stayed
     * stable across releases.
     */
    public static LootContext emptyLootContext(ServerLevel level) {
        return new LootContext.Builder(new LootParams(level, Map.of(), Map.of(), 0)).create(Optional.empty());
    }

    private static void rebuild(RecipeManager manager) {
        Map<Item, Map<Item, List<SieveRecipe>>> built = new HashMap<>();
        for (RecipeHolder<SieveRecipe> holder : manager.getAllRecipesFor(ERecipeTypes.SIEVE.get())) {
            SieveRecipe recipe = holder.value();
            for (ItemStack meshStack : recipe.mesh.getItems()) {
                Map<Item, List<SieveRecipe>> byInput = built.computeIfAbsent(meshStack.getItem(), key -> new HashMap<>());
                for (ItemStack inputStack : recipe.ingredient.getItems()) {
                    byInput.computeIfAbsent(inputStack.getItem(), key -> new ArrayList<>()).add(recipe);
                }
            }
        }
        recipesByMesh = built;
        cachedManager = manager;
    }
}
