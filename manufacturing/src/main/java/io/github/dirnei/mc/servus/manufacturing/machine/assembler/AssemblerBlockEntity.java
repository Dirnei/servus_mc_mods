package io.github.dirnei.mc.servus.manufacturing.machine.assembler;

import io.github.dirnei.mc.servus.lib.craft.IngredientGatherer;
import io.github.dirnei.mc.servus.lib.craft.RoundRobin;
import io.github.dirnei.mc.servus.lib.team.Teams;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineStats;
import io.github.dirnei.mc.servus.manufacturing.machine.PoweredMachineBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.menu.AssemblerMenu;
import io.github.dirnei.mc.servus.manufacturing.recipe.AssemblerRecipeInput;
import io.github.dirnei.mc.servus.manufacturing.recipe.AssemblingRecipe;
import io.github.dirnei.mc.servus.manufacturing.recipe.AssemblingRecipes;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import io.github.dirnei.mc.servus.manufacturing.research.ResearchManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Crafts any configured recipe, vanilla crafting or assembling, from nine
 * input slots. Up to {@code recipe_slots} recipes are configured through the
 * pattern grid or a recipe viewer and run round robin.
 */
public class AssemblerBlockEntity extends PoweredMachineBlockEntity {

    public static final int SLOT_INPUT_FIRST = 0;
    public static final int INPUT_SLOTS = 9;
    public static final int SLOT_OUTPUT = 9;
    public static final int DATA_ACTIVE_RECIPE = 6;
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "assembler");
    private static final MachineStats DEFAULTS = MachineStats.crafter(20, 20, 8000, 2);

    /** Ghost grid the player lays a recipe out in; never consumed. */
    private final ItemStackHandler pattern = new ItemStackHandler(ConfiguredRecipe.GRID) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final List<ConfiguredRecipe> recipes = new ArrayList<>();
    private int cursor;
    private int active = -1;

    public AssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(ServusManufacturingBlockEntities.ASSEMBLER.get(), pos, state, INPUT_SLOTS + 1);
    }

    // Configuration

    public ItemStackHandler pattern() {
        return pattern;
    }

    public List<ConfiguredRecipe> recipes() {
        return Collections.unmodifiableList(recipes);
    }

    public int recipeSlots() {
        return stats().recipeSlots();
    }

    public int activeIndex() {
        return active;
    }

    /** Adds a recipe by id, from a recipe viewer. Tells the player why when refused. */
    public void addRecipe(ResourceLocation id, Player player) {
        if (level == null) {
            return;
        }
        Optional<RecipeHolder<?>> holder = level.getRecipeManager().byKey(id);
        if (holder.isEmpty()) {
            tell(player, "unknown");
            return;
        }
        if (holder.get().value() instanceof AssemblingRecipe) {
            @SuppressWarnings("unchecked")
            RecipeHolder<AssemblingRecipe> typed = (RecipeHolder<AssemblingRecipe>) holder.get();
            add(ConfiguredRecipe.fromAssembling(typed), player);
        } else if (holder.get().value() instanceof CraftingRecipe) {
            @SuppressWarnings("unchecked")
            RecipeHolder<CraftingRecipe> typed = (RecipeHolder<CraftingRecipe>) holder.get();
            add(ConfiguredRecipe.fromCrafting(typed), player);
        } else {
            tell(player, "unsupported");
        }
    }

    /** Adds whatever recipe the pattern grid describes. */
    public void addFromPattern(Player player) {
        if (level == null) {
            return;
        }
        List<ItemStack> cells = new ArrayList<>(ConfiguredRecipe.GRID);
        for (int i = 0; i < ConfiguredRecipe.GRID; i++) {
            cells.add(pattern.getStackInSlot(i));
        }
        if (cells.stream().allMatch(ItemStack::isEmpty)) {
            tell(player, "empty_pattern");
            return;
        }
        AssemblerRecipeInput assemblingInput = new AssemblerRecipeInput(cells);
        for (RecipeHolder<AssemblingRecipe> holder : AssemblingRecipes.sorted(level)) {
            if (holder.value().matches(assemblingInput, level)) {
                add(ConfiguredRecipe.fromAssembling(holder), player);
                return;
            }
        }
        Optional<RecipeHolder<CraftingRecipe>> crafting =
                level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, cells), level);
        if (crafting.isPresent()) {
            add(ConfiguredRecipe.fromPattern(crafting.get().id(), cells), player);
            return;
        }
        tell(player, "no_match");
    }

    private void add(ConfiguredRecipe recipe, Player player) {
        if (recipes.stream().anyMatch(existing -> existing.id().equals(recipe.id()))) {
            tell(player, "duplicate");
            return;
        }
        if (recipes.size() >= recipeSlots()) {
            tell(player, "full");
            return;
        }
        if (!isUnlocked(recipe, player)) {
            tell(player, "locked");
            return;
        }
        recipes.add(recipe);
        recipesChanged();
    }

    public void removeRecipe(int index) {
        if (index >= 0 && index < recipes.size()) {
            recipes.remove(index);
            if (active == index) {
                active = -1;
                progress = 0;
            } else if (active > index) {
                active--;
            }
            recipesChanged();
        }
    }

    public void clearPattern() {
        for (int i = 0; i < ConfiguredRecipe.GRID; i++) {
            pattern.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    private boolean isUnlocked(ConfiguredRecipe recipe, Player player) {
        Optional<ResourceLocation> tech = recipe.assembling(level).flatMap(AssemblingRecipe::tech);
        if (tech.isEmpty()) {
            return true;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return true;
        }
        return ResearchManager.state(serverPlayer.server, Teams.teamOf(serverPlayer)).isCompleted(tech.get().toString());
    }

    private void tell(Player player, String reason) {
        if (player != null) {
            player.displayClientMessage(Component.translatable("message.servus_manufacturing.assembler." + reason), true);
        }
    }

    private void recipesChanged() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // Crafting

    private record Plan(ConfiguredRecipe recipe, int[] take, ItemStack result, NonNullList<ItemStack> remainders, int time) {
    }

    private List<ItemStack> inputs() {
        List<ItemStack> stacks = new ArrayList<>(INPUT_SLOTS);
        for (int slot = SLOT_INPUT_FIRST; slot < SLOT_INPUT_FIRST + INPUT_SLOTS; slot++) {
            stacks.add(inventory.getStackInSlot(slot));
        }
        return stacks;
    }

    /** Works out whether the recipe can run now and what it would take and give. */
    private Optional<Plan> plan(ConfiguredRecipe recipe) {
        if (level == null) {
            return Optional.empty();
        }
        List<ItemStack> inputs = inputs();
        List<ItemKey> keys = new ArrayList<>(INPUT_SLOTS);
        int[] counts = new int[INPUT_SLOTS];
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = inputs.get(i);
            keys.add(stack.isEmpty() ? null : new ItemKey(stack));
            counts[i] = stack.getCount();
        }
        Map<ItemKey, Integer> required = new HashMap<>();
        for (ItemStack cell : recipe.template()) {
            if (!cell.isEmpty()) {
                required.merge(new ItemKey(cell), cell.getCount(), Integer::sum);
            }
        }
        Optional<int[]> take = IngredientGatherer.gather(keys, counts, required);
        if (take.isEmpty()) {
            return Optional.empty();
        }

        Optional<CraftingRecipe> crafting = recipe.crafting(level);
        if (crafting.isPresent()) {
            CraftingInput input = CraftingInput.of(3, 3, recipe.template());
            if (!crafting.get().matches(input, level)) {
                return Optional.empty();
            }
            ItemStack result = crafting.get().assemble(input, level.registryAccess());
            NonNullList<ItemStack> remainders = crafting.get().getRemainingItems(input);
            return fits(result, remainders)
                    ? Optional.of(new Plan(recipe, take.get(), result, remainders, stats().workTicks()))
                    : Optional.empty();
        }
        Optional<AssemblingRecipe> assembling = recipe.assembling(level);
        if (assembling.isPresent()) {
            List<ItemStack> gathered = new ArrayList<>();
            for (int i = 0; i < INPUT_SLOTS; i++) {
                if (take.get()[i] > 0) {
                    gathered.add(inputs.get(i).copyWithCount(take.get()[i]));
                }
            }
            AssemblerRecipeInput input = new AssemblerRecipeInput(gathered);
            if (!assembling.get().matches(input, level)) {
                return Optional.empty();
            }
            ItemStack result = assembling.get().assemble(input, level.registryAccess());
            return fits(result, NonNullList.create())
                    ? Optional.of(new Plan(recipe, take.get(), result, NonNullList.create(), assembling.get().time()))
                    : Optional.empty();
        }
        return Optional.empty();
    }

    private boolean fits(ItemStack result, NonNullList<ItemStack> remainders) {
        if (!canInsertOutput(SLOT_OUTPUT, result)) {
            return false;
        }
        for (ItemStack remainder : remainders) {
            if (remainder.isEmpty()) {
                continue;
            }
            if (!ItemHandlerHelper.insertItem(inputHandler(), remainder.copy(), true).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private ItemStackHandler inputHandler() {
        return inventory;
    }

    @Override
    protected boolean canWork() {
        if (level == null || recipes.isEmpty()) {
            active = -1;
            return false;
        }
        if (active >= 0 && active < recipes.size() && plan(recipes.get(active)).isPresent()) {
            return true;
        }
        active = RoundRobin.next(recipes.size(), cursor, i -> plan(recipes.get(i)).isPresent());
        return active >= 0;
    }

    @Override
    protected void finishWork() {
        if (active < 0 || active >= recipes.size()) {
            return;
        }
        Optional<Plan> planned = plan(recipes.get(active));
        if (planned.isEmpty()) {
            active = -1;
            return;
        }
        Plan plan = planned.get();
        for (int i = 0; i < INPUT_SLOTS; i++) {
            if (plan.take()[i] > 0) {
                inventory.extractItem(SLOT_INPUT_FIRST + i, plan.take()[i], false);
            }
        }
        insertOutput(SLOT_OUTPUT, plan.result());
        for (ItemStack remainder : plan.remainders()) {
            if (!remainder.isEmpty()) {
                ItemStack left = ItemHandlerHelper.insertItem(inventory, remainder.copy(), false);
                if (!left.isEmpty()) {
                    insertOutput(SLOT_OUTPUT, left);
                }
            }
        }
        cursor = active + 1;
        active = -1;
    }

    /** Ticks of the active recipe; without one the machine never completes a cycle. */
    @Override
    protected int workTicks() {
        if (active >= 0 && active < recipes.size()) {
            return plan(recipes.get(active)).map(plan -> Math.max(1, plan.time())).orElse(Integer.MAX_VALUE);
        }
        return Integer.MAX_VALUE;
    }

    // Machine contract

    @Override
    protected ResourceLocation machineId() {
        return ID;
    }

    @Override
    protected MachineStats defaultStats() {
        return DEFAULTS;
    }

    @Override
    protected boolean isInputSlot(int slot) {
        return slot >= SLOT_INPUT_FIRST && slot < SLOT_INPUT_FIRST + INPUT_SLOTS;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    protected boolean acceptsInput(int slot, ItemStack stack) {
        for (ConfiguredRecipe recipe : recipes) {
            if (recipe.uses(stack)) {
                return true;
            }
            if (level != null && recipe.assembling(level).map(r -> AssemblingRecipes.isIngredientOf(r, stack)).orElse(false)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected int getData(int index) {
        if (index == DATA_ACTIVE_RECIPE) {
            return active;
        }
        if (index == DATA_WORK_TICKS) {
            int ticks = workTicks();
            return ticks == Integer.MAX_VALUE ? 0 : ticks;
        }
        return super.getData(index);
    }

    // Persistence and sync

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Pattern", pattern.serializeNBT(registries));
        tag.put("Recipes", saveRecipes(registries));
        tag.putInt("Cursor", cursor);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Pattern")) {
            pattern.deserializeNBT(registries, tag.getCompound("Pattern"));
        }
        if (tag.contains("Recipes")) {
            loadRecipes(tag.getList("Recipes", Tag.TAG_COMPOUND), registries);
        }
        cursor = tag.getInt("Cursor");
        active = -1;
    }

    private ListTag saveRecipes(HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        recipes.forEach(recipe -> list.add(recipe.save(registries)));
        return list;
    }

    private void loadRecipes(ListTag list, HolderLookup.Provider registries) {
        recipes.clear();
        for (int i = 0; i < list.size(); i++) {
            ConfiguredRecipe.load(list.getCompound(i), registries).ifPresent(recipes::add);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("Recipes", saveRecipes(registries));
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AssemblerMenu(containerId, playerInventory, this);
    }

    /** Item identity for gathering: item plus components, count ignored. */
    private record ItemKey(ItemStack stack) {
        @Override
        public boolean equals(Object other) {
            return other instanceof ItemKey key && ItemStack.isSameItemSameComponents(stack, key.stack);
        }

        @Override
        public int hashCode() {
            return ItemStack.hashItemAndComponents(stack);
        }
    }
}
