package io.github.dirnei.mc.servus.alchemy;

import io.github.dirnei.mc.servus.alchemy.coagulator.CoagulatorSeeds;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyMenus;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineSlot;
import io.github.dirnei.mc.servus.foundation.machine.menu.OutputSlot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The Coagulator's slots (ingredient, output, seed) and its principle tanks. */
public class CoagulatorMenu extends PrincipleMenu {

    /** Where the seed slot sits, under the arrow. */
    public static final int SEED_X = 82;
    public static final int SEED_Y = 54;

    public CoagulatorMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine) {
        super(ServusAlchemyMenus.COAGULATOR.get(), containerId, playerInventory, machine);
    }

    private CoagulatorMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine, ContainerData data) {
        super(ServusAlchemyMenus.COAGULATOR.get(), containerId, playerInventory, machine, data);
    }

    public static CoagulatorMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new CoagulatorMenu(containerId, playerInventory, readMachine(playerInventory, buf),
                clientData(CoagulatorBlockEntity.DATA_COUNT));
    }

    @Override
    protected void addMachineSlots() {
        addSlot(new MachineSlot(machine, CoagulatorBlockEntity.SLOT_INGREDIENT, 56, 35));
        addSlot(new OutputSlot(machine, CoagulatorBlockEntity.SLOT_OUTPUT, 116, 35));
        addSlot(new SeedSlot(machine));
    }

    /** The seed slot of this menu. */
    public Slot seedSlot() {
        return slots.get(2);
    }

    /** Whether the seed's recipe waits for a journal chapter of the owner's team. */
    public boolean seedLocked() {
        return data().get(CoagulatorBlockEntity.DATA_LOCKED) == 1;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        Principle principle = PrincipleBuckets.gaugeOf(id);
        if (principle != null) {
            return machine instanceof CoagulatorBlockEntity coagulator && PrincipleBuckets.clickGauge(player, this, coagulator.tanks(), principle);
        }
        return super.clickMenuButton(player, id);
    }

    /** Holds a stack of one seed; any seed may replace the one in it by a click. */
    private static final class SeedSlot extends MachineSlot {

        private final MachineBlockEntity machine;

        private SeedSlot(MachineBlockEntity machine) {
            super(machine, CoagulatorBlockEntity.SLOT_SEED, SEED_X, SEED_Y);
            this.machine = machine;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return machine.getLevel() != null && CoagulatorSeeds.isSeed(machine.getLevel(), stack);
        }
    }
}
