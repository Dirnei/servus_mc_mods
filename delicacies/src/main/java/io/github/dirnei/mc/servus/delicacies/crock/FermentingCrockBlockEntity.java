package io.github.dirnei.mc.servus.delicacies.crock;

import io.github.dirnei.mc.servus.delicacies.cooking.TankVesselBlockEntity;
import io.github.dirnei.mc.servus.delicacies.cooking.VesselInput;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlockEntities;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesRecipes;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The Fermenting Crock's contents and clock. When its contents match a fermenting recipe it seals and ferments every
 * full batch for the recipe's time in game ticks; nothing goes in or out but the result while it is sealed. It looks
 * at itself only after a change and through scheduled block ticks, never every tick.
 */
public class FermentingCrockBlockEntity extends TankVesselBlockEntity<FermentingRecipe> {

    public static final int CAPACITY = 4000;
    /** The longest a sealed crock waits between two looks at the clock. */
    public static final int CHECK_INTERVAL = 100;

    private final Fermentation fermentation = new Fermentation();

    public FermentingCrockBlockEntity(BlockPos pos, BlockState state) {
        super(ServusDelicaciesBlockEntities.FERMENTING_CROCK.get(), pos, state, CAPACITY);
    }

    @Override
    protected RecipeType<FermentingRecipe> recipeType() {
        return ServusDelicaciesRecipes.FERMENTING.get();
    }

    /** Whether the lid is on and it ferments. */
    public boolean sealed() {
        return fermentation.active();
    }

    @Override
    protected boolean canHoldInput(int slot, ItemStack stack) {
        return !sealed() && super.canHoldInput(slot, stack);
    }

    @Override
    protected boolean tankTakes(FluidStack fluid) {
        return !sealed() && super.tankTakes(fluid);
    }

    @Override
    protected boolean tankGives() {
        return !sealed();
    }

    @Override
    protected void contentsChanged() {
        super.contentsChanged();
        scheduleCheck(1);
    }

    private void scheduleCheck(int delay) {
        if (level instanceof ServerLevel server) {
            server.scheduleTick(worldPosition, getBlockState().getBlock(), delay);
        }
    }

    /** Looks at the clock and the contents: finishes a done fermentation, seals when a recipe matches. */
    void check() {
        if (level == null || level.isClientSide()) {
            return;
        }
        long now = level.getGameTime();
        if (sealed()) {
            if (!fermentation.done(now)) {
                scheduleCheck(fermentation.nextCheck(now, CHECK_INTERVAL));
                return;
            }
            finish();
        }
        startIfReady(now);
    }

    private void startIfReady(long now) {
        Optional<FermentingRecipe> recipe = matchingRecipe();
        if (recipe.isEmpty() || batches(recipe.get()) < 1) {
            return;
        }
        fermentation.start(now, recipe.get().time());
        showSealed(true);
        setChanged();
        scheduleCheck(fermentation.nextCheck(now, CHECK_INTERVAL));
    }

    private void finish() {
        Optional<FermentingRecipe> recipe = matchingRecipe();
        fermentation.stop();
        recipe.ifPresent(found -> ferment(found, batches(found)));
        showSealed(false);
        setChanged();
    }

    private void ferment(FermentingRecipe recipe, int batches) {
        int[] assignment = recipe.slotsFor(input()).orElse(null);
        if (assignment == null || batches < 1) {
            return;
        }
        List<SizedIngredient> ingredients = recipe.ingredients();
        for (int i = 0; i < ingredients.size(); i++) {
            slots.extractItem(assignment[i], ingredients.get(i).count() * batches, false);
        }
        recipe.fluid().ifPresent(fluid -> drainFluid(fluid.amount() * batches));
        ItemStack result = recipe.result();
        slots.insertItem(OUTPUT, result.copyWithCount(result.getCount() * batches), false);
    }

    /** The full batches of {@code recipe} the ingredients, the fluid and the room in the output allow. */
    private int batches(FermentingRecipe recipe) {
        VesselInput input = input();
        int[] assignment = recipe.slotsFor(input).orElse(null);
        if (assignment == null) {
            return 0;
        }
        List<SizedIngredient> ingredients = recipe.ingredients();
        int[] have = new int[ingredients.size()];
        int[] need = new int[ingredients.size()];
        for (int i = 0; i < ingredients.size(); i++) {
            have[i] = input.items().get(assignment[i]).getCount();
            need[i] = ingredients.get(i).count();
        }
        int fluidNeed = recipe.fluid().map(fluid -> fluid.amount()).orElse(0);
        ItemStack result = recipe.result();
        return CrockBatch.count(have, need, fluid().getAmount(), fluidNeed, outputRoom(result), result.getCount());
    }

    private int outputRoom(ItemStack result) {
        ItemStack full = result.copyWithCount(result.getMaxStackSize());
        return full.getCount() - slots.insertItem(OUTPUT, full, true).getCount();
    }

    private VesselInput input() {
        return new VesselInput(inputs(), fluid());
    }

    private Optional<FermentingRecipe> matchingRecipe() {
        return level == null ? Optional.empty()
                : ServusRecipes.first(level, recipeType(), input()).map(RecipeHolder::value);
    }

    private void showSealed(boolean sealed) {
        BlockState state = getBlockState();
        if (level != null && state.hasProperty(FermentingCrockBlock.SEALED) && state.getValue(FermentingCrockBlock.SEALED) != sealed) {
            level.setBlock(worldPosition, state.setValue(FermentingCrockBlock.SEALED, sealed), Block.UPDATE_CLIENTS);
        }
    }

    /** What a sealed crock tells the player: what it makes and about how many in-game hours are left. */
    public Component status() {
        if (!sealed() || level == null) {
            return Component.translatable("message.servus_delicacies.crock.empty");
        }
        Component result = matchingRecipe().map(recipe -> recipe.result().getHoverName())
                .orElse(Component.translatable("message.servus_delicacies.crock.unknown"));
        return Component.translatable("message.servus_delicacies.crock.fermenting", result, fermentation.hoursLeft(level.getGameTime()));
    }

    /** Game ticks until it is done; 0 when it is not sealed. */
    public long remaining() {
        return level == null ? 0 : fermentation.remaining(level.getGameTime());
    }

    /** Lets {@code ticks} of game time pass for the fermentation at once and looks at the clock right away. */
    public void backdate(long ticks) {
        fermentation.backdate(ticks);
        setChanged();
        check();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("StartedAt", fermentation.startedAt());
        output.putInt("Duration", fermentation.duration());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        fermentation.restore(input.getLongOr("StartedAt", -1), input.getIntOr("Duration", 0));
    }
}
