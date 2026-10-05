package io.github.dirnei.mc.servus.manufacturing.machine.assembler;

import io.github.dirnei.mc.servus.manufacturing.recipe.AssemblingRecipe;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * One recipe an assembler is set up to craft: the recipe id plus a concrete
 * template of the items it takes. For crafting recipes the template is the
 * 3 by 3 grid; for assembling recipes it is the list of counted ingredients.
 */
public final class ConfiguredRecipe {

    public static final int GRID = 9;

    private final ResourceLocation id;
    private final List<ItemStack> template;
    private RecipeHolder<?> cached;
    private boolean resolved;

    public ConfiguredRecipe(ResourceLocation id, List<ItemStack> template) {
        this.id = id;
        this.template = List.copyOf(template);
    }

    public ResourceLocation id() {
        return id;
    }

    /** Concrete stacks this recipe takes, empty cells allowed. */
    public List<ItemStack> template() {
        return template;
    }

    public Optional<RecipeHolder<?>> resolve(Level level) {
        if (!resolved) {
            cached = level.getRecipeManager().byKey(id).orElse(null);
            resolved = true;
        }
        return Optional.ofNullable(cached);
    }

    public boolean isCrafting(Level level) {
        return resolve(level).map(holder -> holder.value() instanceof CraftingRecipe).orElse(false);
    }

    public Optional<AssemblingRecipe> assembling(Level level) {
        return resolve(level).map(RecipeHolder::value)
                .filter(AssemblingRecipe.class::isInstance)
                .map(AssemblingRecipe.class::cast);
    }

    public Optional<CraftingRecipe> crafting(Level level) {
        return resolve(level).map(RecipeHolder::value)
                .filter(CraftingRecipe.class::isInstance)
                .map(CraftingRecipe.class::cast);
    }

    /** Result shown in the GUI, independent of inputs. */
    public ItemStack displayResult(Level level) {
        return resolve(level).map(holder -> holder.value().getResultItem(level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    /** Whether the stack is one of the template items. */
    public boolean uses(ItemStack stack) {
        for (ItemStack cell : template) {
            if (!cell.isEmpty() && ItemStack.isSameItemSameComponents(cell, stack)) {
                return true;
            }
        }
        return false;
    }

    // Factories

    /** Template from a crafting recipe's ingredients, shaped recipes keep their layout. */
    public static ConfiguredRecipe fromCrafting(RecipeHolder<CraftingRecipe> holder) {
        CraftingRecipe recipe = holder.value();
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        List<ItemStack> template = new ArrayList<>(NonNullList.withSize(GRID, ItemStack.EMPTY));
        if (recipe instanceof ShapedRecipe shaped) {
            int width = shaped.getWidth();
            int height = shaped.getHeight();
            for (int row = 0; row < height; row++) {
                for (int col = 0; col < width; col++) {
                    template.set(row * 3 + col, firstItem(ingredients.get(row * width + col)));
                }
            }
        } else {
            for (int i = 0; i < Math.min(GRID, ingredients.size()); i++) {
                template.set(i, firstItem(ingredients.get(i)));
            }
        }
        return new ConfiguredRecipe(holder.id(), template);
    }

    /** Template from an assembling recipe: one counted stack per ingredient. */
    public static ConfiguredRecipe fromAssembling(RecipeHolder<AssemblingRecipe> holder) {
        List<ItemStack> template = new ArrayList<>();
        for (SizedIngredient sized : holder.value().ingredients()) {
            ItemStack[] options = sized.getItems();
            template.add(options.length == 0 ? ItemStack.EMPTY : options[0].copyWithCount(sized.count()));
        }
        return new ConfiguredRecipe(holder.id(), template);
    }

    /** Template as laid out in the pattern grid, one item per cell. */
    public static ConfiguredRecipe fromPattern(ResourceLocation id, List<ItemStack> pattern) {
        List<ItemStack> template = new ArrayList<>(GRID);
        for (int i = 0; i < GRID; i++) {
            ItemStack cell = i < pattern.size() ? pattern.get(i) : ItemStack.EMPTY;
            template.add(cell.isEmpty() ? ItemStack.EMPTY : cell.copyWithCount(1));
        }
        return new ConfiguredRecipe(id, template);
    }

    private static ItemStack firstItem(Ingredient ingredient) {
        ItemStack[] options = ingredient.getItems();
        return options.length == 0 ? ItemStack.EMPTY : options[0].copyWithCount(1);
    }

    // NBT

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", id.toString());
        ListTag list = new ListTag();
        for (ItemStack stack : template) {
            list.add(stack.isEmpty() ? new CompoundTag() : stack.save(registries));
        }
        tag.put("Template", list);
        return tag;
    }

    public static Optional<ConfiguredRecipe> load(CompoundTag tag, HolderLookup.Provider registries) {
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("Id"));
        if (id == null) {
            return Optional.empty();
        }
        List<ItemStack> template = new ArrayList<>();
        for (Tag entry : tag.getList("Template", Tag.TAG_COMPOUND)) {
            CompoundTag stackTag = (CompoundTag) entry;
            template.add(stackTag.isEmpty() ? ItemStack.EMPTY : ItemStack.parseOptional(registries, stackTag));
        }
        return Optional.of(new ConfiguredRecipe(id, template));
    }
}
