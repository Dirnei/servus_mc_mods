package io.github.dirnei.mc.servus.delicacies.firebox;

import io.github.dirnei.mc.servus.delicacies.kitchen.KitchenSlots;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

/**
 * The firebox a multiblock master keeps: one fuel slot, the wood fire and the Heating Element. The owner ticks it on
 * the server through {@link #tick()}, offers {@link #firebox()} to its cooking spots and {@link #fuelView()} to
 * automation, and saves it with {@link #save(ValueOutput)}. Automation puts in only fuel and only while no element
 * heats; it takes out what no longer burns, and while an element heats also the fuel left in the slot.
 */
public final class MasterFirebox {

    private final BlockEntity owner;
    private final Consumer<Boolean> showLit;
    private final ItemSlots fuel = new ItemSlots(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isFuel(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            owner.setChanged();
        }
    };
    private final FireboxHeating heating;
    private final Firebox firebox = new Firebox() {
        @Override
        public boolean isHot() {
            return hot();
        }

        @Override
        public void demandHeat() {
            demanded = true;
        }
    };
    private final KitchenSlots fuelView = new KitchenSlots(fuel, (slot, stack) -> isFuel(stack) && acceptsFuel(),
            (slot, stack) -> !isFuel(stack) || hasElement());
    private boolean demanded;

    /**
     * @param owner    the master block entity, which saves this firebox and sends clients the gauge
     * @param statsId  the firebox's id in {@code data/<ns>/heating/}
     * @param defaults its Heating Element numbers when no datapack names it
     * @param showLit  shows on the owner's blocks whether the firebox is hot, when that changes
     */
    public MasterFirebox(BlockEntity owner, Identifier statsId, HeatingStats defaults, Consumer<Boolean> showLit) {
        this.owner = owner;
        this.showLit = showLit;
        this.heating = new FireboxHeating(statsId, defaults, this::burnNextFuel, owner::setChanged);
    }

    /** How many ticks one {@code stack} item burns in a firebox; 0 when it is no fuel. */
    public static int burnTime(@Nullable Level level, ItemStack stack) {
        return level == null || stack.isEmpty() ? 0 : stack.getBurnTime(RecipeType.SMELTING, level.fuelValues());
    }

    /** Whether {@code stack} burns in a firebox in {@code level}. */
    public static boolean isFuel(@Nullable Level level, ItemStack stack) {
        return burnTime(level, stack) > 0;
    }

    private boolean isFuel(ItemStack stack) {
        return isFuel(owner.getLevel(), stack);
    }

    private int burnNextFuel() {
        ItemStack stack = fuel.getStackInSlot(0);
        int ticks = burnTime(owner.getLevel(), stack);
        if (ticks <= 0) {
            return 0;
        }
        ItemStackTemplate remainder = stack.getCraftingRemainder();
        ItemStack left = stack.copy();
        left.shrink(1);
        fuel.setStackInSlot(0, left.isEmpty() && remainder != null ? remainder.create() : left);
        return ticks;
    }

    /** What the cooking spots on top ask for heat. */
    public Firebox firebox() {
        return firebox;
    }

    /** The fuel slot as automation sees it. */
    public ResourceHandler<ItemResource> fuelView() {
        return fuelView;
    }

    public FireboxHeating heating() {
        return heating;
    }

    /** Whether the fire or the element heats the spots right now. */
    public boolean hot() {
        return heating.heat().hot();
    }

    public boolean hasElement() {
        return heating.hasElement();
    }

    /** Whether new fuel may go in: not while the element heats. */
    public boolean acceptsFuel() {
        return heating.acceptsFuel();
    }

    /** The fuel stack in the slot. */
    public ItemStack fuelStack() {
        return fuel.getStackInSlot(0);
    }

    /** Puts {@code stack} into the fuel slot when it is fuel and no element heats; returns what did not fit. */
    public ItemStack addFuel(ItemStack stack) {
        if (!isFuel(stack) || !acceptsFuel()) {
            return stack;
        }
        return fuel.insertItem(0, stack, false);
    }

    /** One server tick: the fire or the element heats, the owner shows when that changes, and clients get the gauge. */
    public void tick() {
        boolean wasHot = hot();
        boolean gauge = heating.tick(demanded);
        demanded = false;
        if (hot()) {
            owner.setChanged();
        }
        if (wasHot != hot()) {
            showLit.accept(hot());
        }
        if (gauge) {
            sendGauge();
        }
    }

    /** Sends clients the owner's update tag, which carries the gauge. */
    public void sendGauge() {
        Level level = owner.getLevel();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(owner.getBlockPos(), owner.getBlockState(), owner.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Installs the element; false when there already is one. The fuel in the slot stays. */
    public boolean installElement() {
        return heating.install();
    }

    /** Takes the element out; returns the item, or empty when there was none. */
    public ItemStack takeElement() {
        return heating.takeElement();
    }

    /** Puts the fire out and forgets the last demand, for a master that unforms. */
    public void putOut() {
        heating.fire().restore(0, 0);
        demanded = false;
    }

    /** Drops the fuel and the element at {@code pos}. */
    public void dropAll(Level level, BlockPos pos) {
        fuel.dropAll(level, pos);
        heating.dropElement(level, pos);
    }

    /**
     * What a player hears who asks how the firebox heats: the element's heat, threshold and FE, or the fire's state and
     * fuel under {@code fireKeys}, which has {@code .lit}, {@code .cold} and each with {@code .no_fuel}.
     */
    public Component status(String fireKeys) {
        String fire = hot() ? "lit" : "cold";
        if (hasElement()) {
            return Component.translatable("message.servus_delicacies.stove.electric." + fire, heating.heatPercent(),
                    heating.hotAtPercent(), heating.storedEnergy());
        }
        ItemStack stack = fuelStack();
        if (stack.isEmpty()) {
            return Component.translatable(fireKeys + "." + fire + ".no_fuel");
        }
        return Component.translatable(fireKeys + "." + fire, stack.getCount(), stack.getHoverName());
    }

    public void save(ValueOutput output) {
        heating.save(output);
        fuel.save(output, "Fuel");
    }

    public void load(ValueInput input) {
        heating.load(input);
        fuel.load(input, "Fuel");
    }
}
