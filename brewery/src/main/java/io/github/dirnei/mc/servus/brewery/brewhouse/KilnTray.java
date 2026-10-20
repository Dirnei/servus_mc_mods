package io.github.dirnei.mc.servus.brewery.brewhouse;

import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryRecipes;
import io.github.dirnei.mc.servus.delicacies.firebox.Firebox;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * One tray of the Malt Kiln: one stack that dries over a hot fire into the result of its kilning recipe, pale or dark
 * by the tray's mode. Adding to the tray starts the drying over; it pauses while the fire is out.
 */
public final class KilnTray {

    private final Supplier<@Nullable Level> level;
    private final Runnable changed;
    private final Consumer<TrayLook> show;
    private final TraySlot slot = new TraySlot();
    private int progress;
    private KilnMode mode = KilnMode.PALE;
    private TrayLook dried = TrayLook.PALE;

    /**
     * @param level   the level of the Brewhouse, null while it has none
     * @param changed called after any change that must be saved
     * @param show    shows the tray's look on its block
     */
    public KilnTray(Supplier<@Nullable Level> level, Runnable changed, Consumer<TrayLook> show) {
        this.level = level;
        this.changed = changed;
        this.show = show;
    }

    /** The tray's slot, as the item capability on every side. */
    public ItemSlots slot() {
        return slot;
    }

    /** The stack on the tray; do not change it. */
    public ItemStack stack() {
        return slot.getStackInSlot(0);
    }

    public int progress() {
        return progress;
    }

    public KilnMode mode() {
        return mode;
    }

    /** Switches between pale and dark; returns the new mode. */
    public KilnMode toggleMode() {
        mode = mode.next();
        changed.run();
        return mode;
    }

    /** What the tray shows now. */
    public TrayLook look() {
        if (stack().isEmpty()) {
            return TrayLook.EMPTY;
        }
        return recipe().isPresent() ? TrayLook.GREEN : dried;
    }

    /** Puts as much of {@code stack} on the tray as fits and returns how many went on; the caller shrinks its stack. */
    public int spread(ItemStack stack) {
        return stack.getCount() - slot.insertItem(0, stack.copy(), false).getCount();
    }

    /** Empties the tray and returns what lay on it. */
    public ItemStack takeAll() {
        ItemStack taken = stack().copy();
        slot.setStackInSlot(0, ItemStack.EMPTY);
        return taken;
    }

    /** Whether some kilning recipe takes {@code stack}. */
    public boolean accepts(ItemStack stack) {
        Level at = level.get();
        return at != null && ServusBreweryRecipes.kilning(at, stack).isPresent();
    }

    private Optional<KilningRecipe> recipe() {
        Level at = level.get();
        return at == null ? Optional.empty() : ServusBreweryRecipes.kilning(at, stack());
    }

    /** One server tick: dries the stack while the fire is hot and turns it into its result when done. */
    public void tick(Firebox firebox) {
        Optional<KilningRecipe> found = recipe();
        if (found.isEmpty()) {
            return;
        }
        firebox.demandHeat();
        int next = KilnClock.step(progress, firebox.isHot());
        if (next == progress) {
            return;
        }
        progress = next;
        KilningRecipe recipe = found.get();
        if (KilnClock.done(progress, KilnClock.target(recipe.time(), recipe.darkTime(), mode))) {
            boolean dark = KilnClock.makesDark(mode, recipe.darkTime());
            dried = dark ? TrayLook.DARK : TrayLook.PALE;
            slot.setStackInSlot(0, recipe.result(stack().getCount(), dark));
        }
        changed.run();
    }

    private void contentsChanged() {
        progress = 0;
        changed.run();
        show.accept(look());
    }

    public void dropAll(Level at, BlockPos pos) {
        slot.dropAll(at, pos);
    }

    public void save(ValueOutput output, String prefix) {
        slot.save(output, prefix + "Items");
        output.putInt(prefix + "Progress", progress);
        output.putBoolean(prefix + "Dark", mode == KilnMode.DARK);
        output.putBoolean(prefix + "DriedDark", dried == TrayLook.DARK);
    }

    public void load(ValueInput input, String prefix) {
        slot.load(input, prefix + "Items");
        progress = input.getIntOr(prefix + "Progress", 0);
        mode = input.getBooleanOr(prefix + "Dark", false) ? KilnMode.DARK : KilnMode.PALE;
        dried = input.getBooleanOr(prefix + "DriedDark", false) ? TrayLook.DARK : TrayLook.PALE;
    }

    /** One stack: takes only what a kilning recipe takes, and gives out only dried malt. */
    private final class TraySlot extends ItemSlots {

        private TraySlot() {
            super(1);
        }

        @Override
        public boolean isItemValid(int index, ItemStack stack) {
            return accepts(stack);
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return recipe().isPresent() ? 0 : super.extract(index, resource, amount, transaction);
        }

        @Override
        protected void onContentsChanged(int index) {
            contentsChanged();
        }
    }
}
