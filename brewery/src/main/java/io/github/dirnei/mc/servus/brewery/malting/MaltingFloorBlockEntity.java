package io.github.dirnei.mc.servus.brewery.malting;

import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryBlockEntities;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryRecipes;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * Holds one stack of grain on a Malting Floor and times its germination in game time. Germination is checked by
 * scheduled block ticks, never every tick.
 */
public class MaltingFloorBlockEntity extends BlockEntity {

    /** Most ticks between two germination checks; a turn can make a batch finish earlier than a pending check. */
    static final int CHECK_TICKS = 100;

    private final FloorSlot slot = new FloorSlot();
    private @Nullable Germination germination;

    public MaltingFloorBlockEntity(BlockPos pos, BlockState state) {
        super(ServusBreweryBlockEntities.MALTING_FLOOR.get(), pos, state);
    }

    /** The floor's one slot, as the item capability on every side. */
    public FloorSlot slot() {
        return slot;
    }

    /** The grain on the floor; do not change it. */
    public ItemStack grain() {
        return slot.getStackInSlot(0);
    }

    /** The running batch, if soaked grain lies on the floor. */
    public Optional<Germination> germination() {
        return Optional.ofNullable(germination);
    }

    /** Puts as much of {@code stack} on the floor as fits and returns how many went on; the caller shrinks its stack. */
    public int spread(ItemStack stack) {
        return stack.getCount() - slot.insertItem(0, stack.copy(), false).getCount();
    }

    /** Empties the floor and returns what lay on it. */
    public ItemStack takeAll() {
        ItemStack taken = grain().copy();
        slot.setStackInSlot(0, ItemStack.EMPTY);
        return taken;
    }

    /** Turns the soaked grain: shortens the rest of the time unless the cooldown runs. Server side. */
    public boolean turn() {
        Optional<GerminatingRecipe> recipe = recipe();
        if (germination == null || recipe.isEmpty()) {
            return false;
        }
        Optional<Germination> turned = germination.turn(now(), recipe.get().turnCooldown(), recipe.get().turnBonus());
        turned.ifPresent(next -> {
            germination = next;
            setChanged();
            check();
        });
        return turned.isPresent();
    }

    /** Turns finished soaked grain into its result, or schedules the next check. Server side. */
    public void check() {
        Optional<GerminatingRecipe> recipe = recipe();
        if (germination == null || recipe.isEmpty()) {
            return;
        }
        long time = recipe.get().time();
        if (germination.isDone(now(), time)) {
            slot.setStackInSlot(0, recipe.get().result(grain().getCount()));
        } else {
            scheduleCheck(germination.remaining(now(), time));
        }
    }

    private Optional<GerminatingRecipe> recipe() {
        return level == null ? Optional.empty() : ServusBreweryRecipes.germinating(level, grain());
    }

    private long now() {
        return level.getGameTime();
    }

    private void scheduleCheck(long remaining) {
        level.scheduleTick(worldPosition, getBlockState().getBlock(), (int) Math.max(1, Math.min(remaining, CHECK_TICKS)));
    }

    /** Restarts the batch when soaked grain came on, clears it otherwise, and shows the content in the block state. */
    private void contentsChanged() {
        if (level == null || level.isClientSide()) {
            return;
        }
        FloorContent content;
        if (grain().isEmpty()) {
            germination = null;
            content = FloorContent.EMPTY;
        } else if (recipe().isPresent()) {
            germination = Germination.start(now());
            scheduleCheck(recipe().get().time());
            content = FloorContent.SOAKED;
        } else {
            germination = null;
            content = FloorContent.GREEN;
        }
        setChanged();
        BlockState state = getBlockState();
        if (state.getValue(MaltingFloorBlock.CONTENT) != content) {
            level.setBlock(worldPosition, state.setValue(MaltingFloorBlock.CONTENT, content), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        slot.save(output, "Grain");
        if (germination != null) {
            output.putLong("Started", germination.startedAt());
            output.putLong("Bonus", germination.bonus());
            output.putLong("LastTurn", germination.lastTurn());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        slot.load(input, "Grain");
        germination = input.getLong("Started")
                .map(started -> new Germination(started, input.getLongOr("Bonus", 0), input.getLongOr("LastTurn", Germination.NEVER)))
                .orElse(null);
    }

    /** Drops the grain when the floor is removed. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !grain().isEmpty()) {
            Block.popResource(level, pos, grain().copy());
        }
    }

    /** One stack of grain: takes only what a germinating recipe takes, and gives out only finished malt. */
    public final class FloorSlot extends ItemSlots {

        private FloorSlot() {
            super(1);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return level != null && ServusBreweryRecipes.germinating(level, stack).isPresent();
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return germination != null ? 0 : super.extract(index, resource, amount, transaction);
        }

        @Override
        protected void onContentsChanged(int slot) {
            contentsChanged();
        }
    }
}
