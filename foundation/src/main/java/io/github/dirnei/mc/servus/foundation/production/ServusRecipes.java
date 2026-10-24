package io.github.dirnei.mc.servus.foundation.production;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

/**
 * Recipe lookups that work on both sides. The server does not send recipes to
 * clients, so the recipe types registered with {@link #syncToClients} are sent
 * with NeoForge's datapack sync and kept here: a lookup on a client level reads
 * those, a lookup on a server level reads the recipe manager.
 */
@EventBusSubscriber(modid = ServusFoundationIds.MOD_ID)
public final class ServusRecipes {

    private static final List<Supplier<? extends RecipeType<?>>> SYNCED = new CopyOnWriteArrayList<>();
    private static volatile RecipeMap clientRecipes = RecipeMap.EMPTY;

    private ServusRecipes() {
    }

    /** Asks the server to send the recipes of a type to clients; call while the mod constructs. */
    public static void syncToClients(Supplier<? extends RecipeType<?>> type) {
        SYNCED.add(type);
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        for (Supplier<? extends RecipeType<?>> type : SYNCED) {
            event.sendRecipes(type.get());
        }
    }

    /** The recipes the client received; set by the client on {@code RecipesReceivedEvent}. */
    public static void setClientRecipes(RecipeMap recipes) {
        clientRecipes = recipes;
    }

    /** The recipes a level sees: the recipe manager's on a server, the synced ones on a client. A reload makes a new map. */
    public static RecipeMap recipeMap(Level level) {
        if (level instanceof ServerLevel server) {
            return server.recipeAccess().recipeMap();
        }
        return clientRecipes;
    }

    /** The registry key of a recipe id. */
    public static ResourceKey<Recipe<?>> key(Identifier id) {
        return ResourceKey.create(Registries.RECIPE, id);
    }

    /** The recipe with that id; on a client only recipes of synced types. */
    public static Optional<RecipeHolder<?>> byId(Level level, Identifier id) {
        return Optional.ofNullable(recipeMap(level).byKey(key(id)));
    }

    /** The crafting recipe with that id, empty for other recipe types. */
    @SuppressWarnings("unchecked")
    public static Optional<RecipeHolder<CraftingRecipe>> crafting(Level level, Identifier id) {
        return byId(level, id).filter(holder -> holder.value() instanceof CraftingRecipe)
                .map(holder -> (RecipeHolder<CraftingRecipe>) holder);
    }

    /** The crafting recipe a grid matches. */
    public static Optional<RecipeHolder<CraftingRecipe>> craftingFor(Level level, CraftingInput input) {
        return first(level, RecipeType.CRAFTING, input);
    }

    /** The first recipe of a type that matches the input. */
    public static <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> first(Level level, RecipeType<T> type, I input) {
        return recipeMap(level).getRecipesFor(type, input, level).findFirst();
    }

    /** Every recipe of a type. */
    public static <I extends RecipeInput, T extends Recipe<I>> List<RecipeHolder<T>> all(Level level, RecipeType<T> type) {
        return List.copyOf(recipeMap(level).byType(type));
    }

    /** The result a recipe shows (its first display), empty when it shows none. */
    public static ItemStack displayResult(Level level, Recipe<?> recipe) {
        List<RecipeDisplay> displays = recipe.display();
        if (displays.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return displays.getFirst().result().resolveForFirstStack(SlotDisplayContext.fromLevel(level));
    }

    /**
     * The ingredients of a crafting recipe as laid out in the grid: shaped
     * recipes keep their pattern positions (an empty cell is an empty
     * ingredient), shapeless ones list their ingredients in order.
     */
    public static List<Optional<Ingredient>> gridIngredients(CraftingRecipe recipe) {
        if (recipe instanceof ShapedRecipe shaped) {
            return shaped.getIngredients();
        }
        List<Optional<Ingredient>> ingredients = new ArrayList<>();
        if (!recipe.placementInfo().isImpossibleToPlace()) {
            recipe.placementInfo().ingredients().forEach(ingredient -> ingredients.add(Optional.of(ingredient)));
        }
        return ingredients;
    }

    /** One item of an ingredient for display, empty when it lists none. */
    public static ItemStack firstItem(Ingredient ingredient) {
        return ingredient.items().findFirst().map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    /** Every item an ingredient accepts, one of each. */
    public static List<ItemStack> items(Ingredient ingredient) {
        return ingredient.items().map(ItemStack::new).toList();
    }

    /** Every item a sized ingredient accepts, each with the ingredient's count. */
    public static List<ItemStack> items(SizedIngredient ingredient) {
        return ingredient.ingredient().items().map(item -> new ItemStack(item, ingredient.count())).toList();
    }
}
