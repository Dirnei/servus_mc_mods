package io.github.dirnei.mc.servus.manufacturing.menu;

import io.github.dirnei.mc.servus.lib.team.Teams;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.assembler.AssemblerBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.recipe.AssemblingRecipe;
import io.github.dirnei.mc.servus.manufacturing.recipe.AssemblingRecipes;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingMenus;
import io.github.dirnei.mc.servus.manufacturing.research.ClientResearch;
import io.github.dirnei.mc.servus.manufacturing.research.ResearchManager;
import io.github.dirnei.mc.servus.manufacturing.research.ResearchState;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.crafting.RecipeHolder;

public class AssemblerMenu extends MachineMenu {

    private final List<RecipeHolder<AssemblingRecipe>> recipes;
    private final Predicate<ResourceLocation> techCompleted;

    public AssemblerMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine) {
        this(containerId, playerInventory, machine, machine.containerData(), serverTechs(playerInventory.player));
    }

    private AssemblerMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine,
                          ContainerData data, Predicate<ResourceLocation> techCompleted) {
        super(ServusManufacturingMenus.ASSEMBLER.get(), containerId, playerInventory, machine, data);
        this.recipes = AssemblingRecipes.sorted(machine.getLevel());
        this.techCompleted = techCompleted;
    }

    public static AssemblerMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new AssemblerMenu(containerId, playerInventory, readMachine(playerInventory, buf),
                new SimpleContainerData(MachineBlockEntity.DATA_COUNT), ClientResearch::isCompleted);
    }

    private static Predicate<ResourceLocation> serverTechs(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            ResearchState state = ResearchManager.state(serverPlayer.server, Teams.teamOf(serverPlayer));
            return tech -> state.isCompleted(tech.toString());
        }
        return tech -> false;
    }

    @Override
    protected void addMachineSlots() {
        addSlot(new MachineSlot(machine, AssemblerBlockEntity.SLOT_INPUT_FIRST, 44, 17));
        addSlot(new MachineSlot(machine, AssemblerBlockEntity.SLOT_INPUT_FIRST + 1, 62, 17));
        addSlot(new MachineSlot(machine, AssemblerBlockEntity.SLOT_INPUT_FIRST + 2, 44, 35));
        addSlot(new MachineSlot(machine, AssemblerBlockEntity.SLOT_INPUT_FIRST + 3, 62, 35));
        addSlot(new OutputSlot(machine, AssemblerBlockEntity.SLOT_OUTPUT, 116, 35));
    }

    /** Recipes in the same order on client and server. */
    public List<RecipeHolder<AssemblingRecipe>> recipes() {
        return recipes;
    }

    /** Index into {@link #recipes()} of the selected recipe, or -1. */
    public int selectedIndex() {
        return data().get(AssemblerBlockEntity.DATA_SELECTED_RECIPE);
    }

    /** Whether the recipe at the index needs no tech or its tech is completed. */
    public boolean isUnlocked(int index) {
        if (index < 0 || index >= recipes.size()) {
            return false;
        }
        return recipes.get(index).value().tech().map(techCompleted::test).orElse(true);
    }

    /** Button ids are recipe indices; the list size means "clear selection". */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(machine instanceof AssemblerBlockEntity assembler)) {
            return false;
        }
        if (id == recipes.size()) {
            assembler.selectRecipe(null);
            return true;
        }
        if (id >= 0 && id < recipes.size() && isUnlocked(id)) {
            assembler.selectRecipe(recipes.get(id).id());
            return true;
        }
        return super.clickMenuButton(player, id);
    }
}
