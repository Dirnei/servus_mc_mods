package io.github.dirnei.mc.servus.brewery.beer;

import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryBlockEntities;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryRecipes;
import io.github.dirnei.mc.servus.foundation.transfer.FluidSlot;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;

/** Holds a Cask's beer and taps it into glasses. Only fluids a fermenting recipe makes go in. */
public class CaskBlockEntity extends BlockEntity {

    /** What a click with a glass at the tap did. */
    public sealed interface Tap {

        /** A glass was filled; give {@code glass} to the player. */
        record Filled(ItemStack glass) implements Tap {
        }

        /** Nothing was tapped; tell the player {@code reason}. */
        record Refused(Component reason) implements Tap {
        }
    }

    private final FluidSlot tank;

    public CaskBlockEntity(BlockPos pos, BlockState state) {
        super(ServusBreweryBlockEntities.CASK.get(), pos, state);
        this.tank = new FluidSlot(CellarStatsManager.INSTANCE.stats().caskCapacity(), this::isBeer) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
    }

    /** The tank, as the fluid capability on every side and for buckets. */
    public FluidSlot tank() {
        return tank;
    }

    /** The beer in the Cask; do not change it. */
    public FluidStack beer() {
        return tank.getFluid();
    }

    /** Fills one {@code glass} from the tap when a tapping recipe pairs it with the beer and there is enough. Server side. */
    public Tap tap(ItemStack glass) {
        FluidStack beer = tank.getFluid();
        if (beer.isEmpty()) {
            return new Tap.Refused(Component.translatable("message.servus_brewery.cask.empty"));
        }
        Optional<TappingRecipe> recipe = ServusBreweryRecipes.tapping(level, glass, beer);
        if (recipe.isEmpty()) {
            return new Tap.Refused(Component.translatable("message.servus_brewery.cask.wrong_glass", beer.getHoverName()));
        }
        if (beer.getAmount() < recipe.get().amount()) {
            return new Tap.Refused(Component.translatable("message.servus_brewery.cask.too_little", beer.getAmount(), beer.getHoverName()));
        }
        tank.drain(recipe.get().amount(), false);
        return new Tap.Filled(recipe.get().result());
    }

    /** What an empty hand is told about the Cask. */
    public Component status() {
        FluidStack beer = tank.getFluid();
        return beer.isEmpty() ? Component.translatable("message.servus_brewery.cask.empty")
                : Component.translatable("message.servus_brewery.cask.status", beer.getAmount(), beer.getHoverName());
    }

    private boolean isBeer(FluidStack fluid) {
        return level == null || ServusBreweryRecipes.isBeer(level, fluid);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.save(output, "Beer");
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tank.load(input, "Beer");
    }
}
