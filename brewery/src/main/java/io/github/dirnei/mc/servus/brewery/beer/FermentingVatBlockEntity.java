package io.github.dirnei.mc.servus.brewery.beer;

import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryBlockEntities;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryRecipes;
import io.github.dirnei.mc.servus.foundation.transfer.FluidSlot;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * Holds a Fermenting Vat's tank and its one Yeast, and times a batch in game time. A batch starts as soon as the tank
 * holds a wort and the yeast its recipe wants; it is checked by scheduled block ticks, never every tick, and turns
 * the whole tank into beer. The yeast is never used up.
 */
public class FermentingVatBlockEntity extends BlockEntity {

    /** Most ticks between two checks of a running batch. */
    static final int CHECK_TICKS = 100;

    private final VatYeast yeast = new VatYeast();
    private final FluidSlot tank;
    private final VatFluids fluids;
    private @Nullable Fermentation fermentation;

    public FermentingVatBlockEntity(BlockPos pos, BlockState state) {
        super(ServusBreweryBlockEntities.FERMENTING_VAT.get(), pos, state);
        this.tank = new FluidSlot(CellarStatsManager.INSTANCE.stats().vatCapacity(), this::isWort) {
            @Override
            protected void onContentsChanged() {
                contentsChanged();
            }
        };
        this.fluids = new VatFluids(tank, this::isFermenting, this::isWort);
    }

    /** The yeast slot, as the item capability on every side. */
    public VatYeast yeastSlot() {
        return yeast;
    }

    /** The tank, as the fluid capability on every side and for buckets. */
    public VatFluids fluids() {
        return fluids;
    }

    /** The yeast in the vat; do not change it. */
    public ItemStack yeast() {
        return yeast.getStackInSlot(0);
    }

    /** The tank's contents; do not change them. */
    public FluidStack contents() {
        return tank.getFluid();
    }

    /** The running batch, if any. */
    public Optional<Fermentation> fermentation() {
        return Optional.ofNullable(fermentation);
    }

    public boolean isFermenting() {
        return fermentation != null;
    }

    /** Puts one Yeast of {@code stack} into the vat and returns how many went in; the caller shrinks its stack. */
    public int insertYeast(ItemStack stack) {
        return stack.getCount() - yeast.insertItem(0, stack.copy(), false).getCount();
    }

    /** Takes the yeast back while no batch runs. */
    public ItemStack takeYeast() {
        return isFermenting() ? ItemStack.EMPTY : yeast.extractItem(0, 1, false);
    }

    /** What an empty hand is told about the vat. */
    public Component status() {
        FluidStack fluid = tank.getFluid();
        if (fluid.isEmpty()) {
            return Component.translatable("message.servus_brewery.vat.empty");
        }
        Optional<FermentingRecipe> recipe = recipe();
        if (fermentation != null && recipe.isPresent()) {
            return Component.translatable("message.servus_brewery.vat.fermenting", fluid.getAmount(), fluid.getHoverName(),
                    fermentation.percent(now(), recipe.get().time()));
        }
        if (isWort(fluid)) {
            return Component.translatable("message.servus_brewery.vat.wort", fluid.getAmount(), fluid.getHoverName());
        }
        return Component.translatable("message.servus_brewery.vat.beer", fluid.getAmount(), fluid.getHoverName());
    }

    /** Turns a finished batch into beer, or schedules the next check. Server side. */
    public void check() {
        Optional<FermentingRecipe> recipe = recipe();
        if (fermentation == null || recipe.isEmpty()) {
            return;
        }
        long time = recipe.get().time();
        if (fermentation.isDone(now(), time)) {
            fermentation = null;
            tank.setFluid(recipe.get().beer(tank.getFluid()));
        } else {
            scheduleCheck(fermentation.remaining(now(), time));
        }
    }

    private boolean isWort(FluidStack fluid) {
        return level == null || ServusBreweryRecipes.isWort(level, fluid);
    }

    private Optional<FermentingRecipe> recipe() {
        return level == null ? Optional.empty() : ServusBreweryRecipes.fermenting(level, yeast(), tank.getFluid());
    }

    private long now() {
        return level.getGameTime();
    }

    private void scheduleCheck(long remaining) {
        level.scheduleTick(worldPosition, getBlockState().getBlock(), (int) Math.max(1, Math.min(remaining, CHECK_TICKS)));
    }

    /** Starts a batch when wort and yeast are in, drops it when they no longer match, and shows the content. */
    private void contentsChanged() {
        setChanged();
        if (level == null || level.isClientSide()) {
            return;
        }
        Optional<FermentingRecipe> recipe = recipe();
        if (recipe.isEmpty()) {
            fermentation = null;
        } else if (fermentation == null) {
            fermentation = Fermentation.start(now());
            scheduleCheck(recipe.get().time());
        }
        BlockState state = getBlockState();
        VatContent content = look();
        if (state.getValue(FermentingVatBlock.CONTENT) != content) {
            level.setBlock(worldPosition, state.setValue(FermentingVatBlock.CONTENT, content), Block.UPDATE_CLIENTS);
        }
    }

    private VatContent look() {
        if (tank.isEmpty()) {
            return VatContent.EMPTY;
        }
        if (fermentation != null) {
            return VatContent.FERMENTING;
        }
        return isWort(tank.getFluid()) ? VatContent.WORT : VatContent.BEER;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        yeast.save(output, "Yeast");
        tank.save(output, "Tank");
        if (fermentation != null) {
            output.putLong("Started", fermentation.startedAt());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        yeast.load(input, "Yeast");
        tank.load(input, "Tank");
        fermentation = input.getLong("Started").map(Fermentation::new).orElse(null);
    }

    /** Drops the yeast when the vat is removed; the fluid is lost. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !yeast().isEmpty()) {
            Block.popResource(level, pos, yeast().copy());
        }
    }

    /** One Yeast: takes only what a fermenting recipe takes as yeast, and gives it out only while no batch runs. */
    public final class VatYeast extends ItemSlots {

        private VatYeast() {
            super(1);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return level != null && ServusBreweryRecipes.isYeast(level, stack);
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return isFermenting() ? 0 : super.extract(index, resource, amount, transaction);
        }

        @Override
        protected void onContentsChanged(int slot) {
            contentsChanged();
        }
    }
}
