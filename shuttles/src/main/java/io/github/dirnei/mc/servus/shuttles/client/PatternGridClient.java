package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.client.RecipeGridView;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.pattern.AnyGroup;
import io.github.dirnei.mc.servus.shuttles.pattern.EncodedPattern;
import io.github.dirnei.mc.servus.shuttles.pattern.PatternEditing;
import io.github.dirnei.mc.servus.shuttles.pattern.PatternVariants;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.Level;

/**
 * What the recipe grid view shows for a menu's {@link io.github.dirnei.mc.servus.shuttles.pattern.PatternGrid}
 * on the client: the result the cells make, and the Exact or Any button with
 * what each Any cell accepts. A material grid set to Any shows its variants'
 * results in turn, one per second, and says what the material follows. The
 * pattern and its variants are worked out again only when the cells or the
 * recipes change. Client only.
 */
final class PatternGridClient implements RecipeGridView.Source {

    private static final String MODE = "gui.servus_shuttles.pattern_grid.mode.";
    /** Ticks each variant's result shows. */
    private static final int CYCLE_TICKS = 20;

    private final Supplier<List<ItemStack>> cells;
    private final Supplier<EncodedPattern.Mode> mode;
    private final Runnable toggle;
    private List<ItemKey> cachedCells;
    private RecipeMap cachedRecipes;
    private Optional<EncodedPattern> pattern = Optional.empty();
    private PatternVariants variants = PatternVariants.NONE;
    private List<ItemStack> variantResults = List.of();

    /**
     * @param cells  the menu's ghost cells
     * @param mode   the mode the menu asks for
     * @param toggle sends the mode button's press to the menu
     */
    PatternGridClient(Supplier<List<ItemStack>> cells, Supplier<EncodedPattern.Mode> mode, Runnable toggle) {
        this.cells = cells;
        this.mode = mode;
        this.toggle = toggle;
    }

    /** What the cells themselves make, empty when they match nothing. */
    ItemStack ownResult() {
        refresh();
        return pattern.map(EncodedPattern::result).orElse(ItemStack.EMPTY);
    }

    /** The cells' result, or for a material grid set to Any each variant's result in turn. */
    @Override
    public ItemStack result() {
        Level level = Minecraft.getInstance().level;
        if (!materialAny() || variantResults.isEmpty() || level == null) {
            return ownResult();
        }
        return variantResults.get((int) (level.getGameTime() / CYCLE_TICKS % variantResults.size()));
    }

    @Override
    public List<Component> resultNotes() {
        return materialAny() ? List.of(followsLine(variants.follows())) : List.of();
    }

    private boolean materialAny() {
        return any() && variants.material();
    }

    /** What the grid's items allow. */
    PatternVariants variants() {
        refresh();
        return variants;
    }

    /** Whether the button shows Any: asked for and possible. */
    boolean any() {
        return variants().possible() && mode.get() == EncodedPattern.Mode.ANY;
    }

    @Override
    public RecipeGridView.ModeState mode() {
        boolean possible = variants().possible();
        return new RecipeGridView.ModeState(Component.translatable(MODE + (any() ? "any" : "exact")), possible, tooltip());
    }

    @Override
    public void toggleMode() {
        if (variants().possible()) {
            toggle.run();
        }
    }

    private List<Component> tooltip() {
        PatternVariants grid = variants();
        List<Component> tooltip = new ArrayList<>();
        if (!grid.possible()) {
            tooltip.add(Component.translatable(MODE + "exact"));
            String why = grid.verdict() == PatternVariants.Verdict.CHANGES_RESULT ? "changes_result" : "no_alternatives";
            tooltip.add(Component.translatable(MODE + why).withStyle(ChatFormatting.GRAY));
            return tooltip;
        }
        boolean any = any();
        tooltip.add(Component.translatable(MODE + (any ? "any" : "exact")));
        tooltip.add(Component.translatable(MODE + (any ? "any_hint" : "exact_hint")).withStyle(ChatFormatting.GRAY));
        if (any) {
            for (AnyGroup group : grid.groups()) {
                tooltip.add(anyLabel(group));
            }
        }
        tooltip.add(Component.translatable(MODE + "toggle_hint").withStyle(ChatFormatting.DARK_GRAY));
        return tooltip;
    }

    /** "Any planks" for a group of an Any pattern, as the mode tooltip and the Patterns tab show it. */
    static Component anyLabel(AnyGroup group) {
        return group.label(I18n.exists(group.tagKey())).copy().withStyle(ChatFormatting.AQUA);
    }

    /** "Material follows: Planks" for a material pattern, as the grid, the Patterns tab and the craftable entry show it. */
    static Component followsLine(AnyGroup follows) {
        Component name = I18n.exists(follows.tagKey()) ? Component.translatable(follows.tagKey())
                : follows.items().isEmpty() ? Component.empty() : follows.items().get(0).getHoverName();
        return Component.translatable("gui.servus_shuttles.pattern.follows", name).withStyle(ChatFormatting.AQUA);
    }

    private void refresh() {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            pattern = Optional.empty();
            variants = PatternVariants.NONE;
            variantResults = List.of();
            return;
        }
        List<ItemStack> grid = cells.get();
        List<ItemKey> keys = grid.stream().map(cell -> cell.isEmpty() ? null : ItemKey.of(cell)).toList();
        RecipeMap recipes = ServusRecipes.recipeMap(level);
        if (keys.equals(cachedCells) && recipes == cachedRecipes) {
            return;
        }
        cachedCells = keys;
        cachedRecipes = recipes;
        pattern = PatternEditing.encode(level, grid);
        variants = pattern.map(found -> PatternVariants.of(level, found)).orElse(PatternVariants.NONE);
        variantResults = pattern.filter(found -> variants.material())
                .map(found -> variants.variants(level, found.withMode(EncodedPattern.Mode.ANY), Map.of(), Set.of()).stream()
                        .map(EncodedPattern::result).toList())
                .orElse(List.of());
    }
}
