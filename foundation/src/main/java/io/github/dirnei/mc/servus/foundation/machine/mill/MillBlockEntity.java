package io.github.dirnei.mc.servus.foundation.machine.mill;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.drive.Drivable;
import io.github.dirnei.mc.servus.foundation.drive.Flywheel;
import io.github.dirnei.mc.servus.foundation.drive.WorkProgress;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Grinds what a milling recipe takes, but only with the work a crank or motor
 * hands it through {@link #drive}. Without a driver the progress stays where it
 * is. The front, where the chute is, only pushes the result out.
 */
public class MillBlockEntity extends MachineBlockEntity {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final Identifier ID = Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "mill");
    /** The Mill has no balance numbers of its own: each recipe's work decides. */
    private static final MachineStats DEFAULTS = MachineStats.crafter(1, 0, 0, 1);
    /** Ticks after the last turn in which the Mill still counts as turning; bridges a tick without drive. */
    private static final int TURNING_WINDOW = 5;
    private static final float WHEEL_DEGREES_PER_TICK = 9;

    private final WorkProgress work = new WorkProgress();
    private final Flywheel wheel = new Flywheel(WHEEL_DEGREES_PER_TICK);
    private long lastTurned = Long.MIN_VALUE / 2;
    private final Drivable drive = new Drivable() {
        @Override
        public boolean wantsWork() {
            return runnableRecipe().isPresent();
        }

        @Override
        public void addWork(int amount) {
            turn();
            runnableRecipe().ifPresent(recipe -> grind(recipe, amount));
        }

        @Override
        public void turn() {
            lastTurned = level.getGameTime();
        }
    };

    public MillBlockEntity(BlockPos pos, BlockState state) {
        super(MachineContent.MILL_ENTITY.get(), pos, state, 2);
        sideConfig().lock(RelativeSide.FRONT, SideMode.PUSH);
        for (RelativeSide side : new RelativeSide[] {RelativeSide.TOP, RelativeSide.BACK, RelativeSide.LEFT, RelativeSide.RIGHT}) {
            sideConfig().set(side, SideMode.INPUT);
        }
    }

    /** The drive link on {@code side}: the back and both sides take drive, the top, bottom and chute front do not. */
    public @Nullable Drivable drive(@Nullable Direction side) {
        if (side == null) {
            return drive;
        }
        RelativeSide relative = relativeSide(side);
        return relative == RelativeSide.BACK || relative == RelativeSide.LEFT || relative == RelativeSide.RIGHT ? drive : null;
    }

    /** The wheel's flywheel on the client. */
    public Flywheel wheel() {
        return wheel;
    }

    @Override
    protected Identifier machineId() {
        return ID;
    }

    @Override
    protected MachineStats defaultStats() {
        return DEFAULTS;
    }

    @Override
    protected boolean isInputSlot(int slot) {
        return slot == SLOT_INPUT;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return slot == SLOT_OUTPUT;
    }

    /** What some milling recipe takes; the recipes are synced, so the client's slot agrees with the server. */
    @Override
    protected boolean acceptsInput(int slot, ItemStack stack) {
        return level != null && ServusRecipes.all(level, MachineContent.MILLING.get()).stream()
                .anyMatch(holder -> holder.value().accepts(stack));
    }

    /** The milling recipe the input holds enough for, if any. */
    private Optional<RecipeHolder<MillingRecipe>> currentRecipe() {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        if (level == null || input.isEmpty()) {
            return Optional.empty();
        }
        return ServusRecipes.first(level, MachineContent.MILLING.get(), new SingleRecipeInput(input));
    }

    /** The current recipe while its result fits into the output. */
    private Optional<MillingRecipe> runnableRecipe() {
        return currentRecipe().map(RecipeHolder::value).filter(recipe -> canInsertOutput(SLOT_OUTPUT, recipe.result()));
    }

    private void grind(MillingRecipe recipe, int amount) {
        if (work.add(amount, recipe.work())) {
            inventory.extractItem(SLOT_INPUT, recipe.input().count(), false);
            insertOutput(SLOT_OUTPUT, recipe.result());
        }
        setChanged();
    }

    /** Forgets the progress when nothing can be ground; reports working while a driver turned it just now. */
    @Override
    protected boolean tick() {
        if (work.value() > 0 && currentRecipe().isEmpty()) {
            work.reset();
            setChanged();
        }
        return level.getGameTime() - lastTurned < TURNING_WINDOW;
    }

    /** The arrow shows the work collected toward the recipe's work. */
    @Override
    protected int getData(int index) {
        return switch (index) {
            case DATA_PROGRESS -> work.value();
            case DATA_WORK_TICKS -> currentRecipe().map(holder -> holder.value().work()).orElse(1);
            default -> super.getData(index);
        };
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Work", work.value());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        work.set(input.getIntOr("Work", 0));
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MillMenu(containerId, playerInventory, this);
    }
}
