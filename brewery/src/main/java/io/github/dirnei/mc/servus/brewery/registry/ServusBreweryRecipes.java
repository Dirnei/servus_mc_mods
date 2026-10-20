package io.github.dirnei.mc.servus.brewery.registry;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import io.github.dirnei.mc.servus.brewery.beer.FermentingInput;
import io.github.dirnei.mc.servus.brewery.beer.FermentingRecipe;
import io.github.dirnei.mc.servus.brewery.beer.TappingInput;
import io.github.dirnei.mc.servus.brewery.beer.TappingRecipe;
import io.github.dirnei.mc.servus.brewery.brewhouse.BrewingRecipe;
import io.github.dirnei.mc.servus.brewery.brewhouse.KilningRecipe;
import io.github.dirnei.mc.servus.brewery.malting.GerminatingRecipe;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The recipe types of Servus: Brewery. */
public final class ServusBreweryRecipes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, ServusBreweryIds.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ServusBreweryIds.MOD_ID);

    public static final Identifier GERMINATING_ID = Identifier.fromNamespaceAndPath(ServusBreweryIds.MOD_ID, "germinating");
    public static final DeferredHolder<RecipeType<?>, RecipeType<GerminatingRecipe>> GERMINATING =
            RECIPE_TYPES.register("germinating", () -> RecipeType.simple(GERMINATING_ID));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<GerminatingRecipe>> GERMINATING_SERIALIZER =
            RECIPE_SERIALIZERS.register("germinating", () -> GerminatingRecipe.SERIALIZER);

    public static final Identifier BREWING_ID = Identifier.fromNamespaceAndPath(ServusBreweryIds.MOD_ID, "brewing");
    public static final DeferredHolder<RecipeType<?>, RecipeType<BrewingRecipe>> BREWING =
            RECIPE_TYPES.register("brewing", () -> RecipeType.simple(BREWING_ID));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BrewingRecipe>> BREWING_SERIALIZER =
            RECIPE_SERIALIZERS.register("brewing", () -> BrewingRecipe.SERIALIZER);

    public static final Identifier KILNING_ID = Identifier.fromNamespaceAndPath(ServusBreweryIds.MOD_ID, "kilning");
    public static final DeferredHolder<RecipeType<?>, RecipeType<KilningRecipe>> KILNING =
            RECIPE_TYPES.register("kilning", () -> RecipeType.simple(KILNING_ID));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<KilningRecipe>> KILNING_SERIALIZER =
            RECIPE_SERIALIZERS.register("kilning", () -> KilningRecipe.SERIALIZER);

    public static final Identifier FERMENTING_ID = Identifier.fromNamespaceAndPath(ServusBreweryIds.MOD_ID, "fermenting");
    public static final DeferredHolder<RecipeType<?>, RecipeType<FermentingRecipe>> FERMENTING =
            RECIPE_TYPES.register("fermenting", () -> RecipeType.simple(FERMENTING_ID));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FermentingRecipe>> FERMENTING_SERIALIZER =
            RECIPE_SERIALIZERS.register("fermenting", () -> FermentingRecipe.SERIALIZER);

    public static final Identifier TAPPING_ID = Identifier.fromNamespaceAndPath(ServusBreweryIds.MOD_ID, "tapping");
    public static final DeferredHolder<RecipeType<?>, RecipeType<TappingRecipe>> TAPPING =
            RECIPE_TYPES.register("tapping", () -> RecipeType.simple(TAPPING_ID));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TappingRecipe>> TAPPING_SERIALIZER =
            RECIPE_SERIALIZERS.register("tapping", () -> TappingRecipe.SERIALIZER);

    private ServusBreweryRecipes() {
    }

    /** Sends the Brewery recipes to clients, for JEI and the client's insertion checks. */
    public static void syncToClients() {
        ServusRecipes.syncToClients(GERMINATING);
        ServusRecipes.syncToClients(BREWING);
        ServusRecipes.syncToClients(KILNING);
        ServusRecipes.syncToClients(FERMENTING);
        ServusRecipes.syncToClients(TAPPING);
    }

    /** The kilning recipe that takes {@code stack}, if any; works on both sides since the recipes are synced. */
    public static Optional<KilningRecipe> kilning(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return ServusRecipes.first(level, KILNING.get(), new SingleRecipeInput(stack)).map(RecipeHolder::value);
    }

    /** Whether some brewing recipe takes {@code stack}, regardless of count. */
    public static boolean brews(Level level, ItemStack stack) {
        return !stack.isEmpty() && ServusRecipes.all(level, BREWING.get()).stream().anyMatch(holder -> holder.value().accepts(stack));
    }

    /** The germinating recipe that takes {@code stack}, if any; works on both sides since the recipes are synced. */
    public static Optional<GerminatingRecipe> germinating(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return ServusRecipes.first(level, GERMINATING.get(), new SingleRecipeInput(stack)).map(RecipeHolder::value);
    }

    /** The fermenting recipe for a vat holding {@code yeast} and {@code wort}, if any. */
    public static Optional<FermentingRecipe> fermenting(Level level, ItemStack yeast, FluidStack wort) {
        if (wort.isEmpty()) {
            return Optional.empty();
        }
        return ServusRecipes.first(level, FERMENTING.get(), new FermentingInput(yeast, wort)).map(RecipeHolder::value);
    }

    /** Whether some fermenting recipe ferments {@code fluid}. */
    public static boolean isWort(Level level, FluidStack fluid) {
        return ServusRecipes.all(level, FERMENTING.get()).stream().anyMatch(holder -> holder.value().fermentsFluid(fluid));
    }

    /** Whether some fermenting recipe takes {@code stack} as its yeast. */
    public static boolean isYeast(Level level, ItemStack stack) {
        return !stack.isEmpty() && ServusRecipes.all(level, FERMENTING.get()).stream().anyMatch(holder -> holder.value().yeast().test(stack));
    }

    /** Whether some fermenting recipe makes {@code fluid}. */
    public static boolean isBeer(Level level, FluidStack fluid) {
        return !fluid.isEmpty() && ServusRecipes.all(level, FERMENTING.get()).stream().anyMatch(holder -> fluid.is(holder.value().result()));
    }

    /** Whether some tapping recipe takes {@code stack} as its empty glass. */
    public static boolean isGlass(Level level, ItemStack stack) {
        return !stack.isEmpty() && ServusRecipes.all(level, TAPPING.get()).stream().anyMatch(holder -> holder.value().glass().test(stack));
    }

    /** The tapping recipe that pairs the glass with the beer, whether or not there is enough beer. */
    public static Optional<TappingRecipe> tapping(Level level, ItemStack glass, FluidStack beer) {
        TappingInput input = new TappingInput(glass, beer);
        return ServusRecipes.all(level, TAPPING.get()).stream().map(RecipeHolder::value).filter(recipe -> recipe.pairs(input)).findFirst();
    }
}
