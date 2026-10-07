package io.github.dirnei.mc.servus.storage.crafting;

import io.github.dirnei.mc.servus.lib.craft.IngredientGatherer;
import io.github.dirnei.mc.servus.lib.craft.RoundRobin;
import io.github.dirnei.mc.servus.lib.production.ProductionProvider;
import io.github.dirnei.mc.servus.lib.production.ProductionRecipe;
import io.github.dirnei.mc.servus.storage.crate.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.menu.CraftingStationMenu;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Storage's own producer: crafts the vanilla recipes the player configured
 * from a pattern grid, without power, and pushes results into neighbours.
 */
public class CraftingStationBlockEntity extends BlockEntity implements MenuProvider {

    public static final int INPUT_SLOTS = 9;
    public static final int OUTPUT_SLOTS = 4;
    public static final int GRID = 9;
    private static final int PUSH_INTERVAL = 8;

    /** A configured vanilla crafting recipe with the grid items that stand for it. */
    public record StationRecipe(ResourceLocation id, List<ItemStack> template) {

        public StationRecipe {
            template = List.copyOf(template);
        }

        /** Whether the stack is one of the template items. */
        public boolean uses(ItemStack stack) {
            for (ItemStack cell : template) {
                if (!cell.isEmpty() && ItemStack.isSameItemSameComponents(cell, stack)) {
                    return true;
                }
            }
            return false;
        }

        public Optional<RecipeHolder<CraftingRecipe>> resolve(Level level) {
            return level.getRecipeManager().byKey(id)
                    .filter(holder -> holder.value() instanceof CraftingRecipe)
                    .map(holder -> {
                        @SuppressWarnings("unchecked")
                        RecipeHolder<CraftingRecipe> crafting = (RecipeHolder<CraftingRecipe>) holder;
                        return crafting;
                    });
        }

        /** The recipe's result for display and for the production view. */
        public ItemStack result(Level level) {
            return resolve(level).map(holder -> holder.value().getResultItem(level.registryAccess())).orElse(ItemStack.EMPTY);
        }

        /** Template counts per item, the inputs one craft consumes. */
        public Map<ItemKey, Integer> required() {
            Map<ItemKey, Integer> required = new HashMap<>();
            for (ItemStack cell : template) {
                if (!cell.isEmpty()) {
                    required.merge(ItemKey.of(cell), cell.getCount(), Integer::sum);
                }
            }
            return required;
        }

        public CompoundTag save(HolderLookup.Provider registries) {
            CompoundTag tag = new CompoundTag();
            tag.putString("Id", id.toString());
            ListTag list = new ListTag();
            for (ItemStack cell : template) {
                list.add(cell.isEmpty() ? new CompoundTag() : cell.save(registries));
            }
            tag.put("Template", list);
            return tag;
        }

        public static Optional<StationRecipe> load(CompoundTag tag, HolderLookup.Provider registries) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("Id"));
            if (id == null) {
                return Optional.empty();
            }
            List<ItemStack> template = new ArrayList<>();
            for (Tag cell : tag.getList("Template", Tag.TAG_COMPOUND)) {
                CompoundTag compound = (CompoundTag) cell;
                template.add(compound.isEmpty() ? ItemStack.EMPTY : ItemStack.parseOptional(registries, compound));
            }
            while (template.size() < GRID) {
                template.add(ItemStack.EMPTY);
            }
            return Optional.of(new StationRecipe(id, template));
        }
    }

    private record Plan(int[] take, ItemStack result, NonNullList<ItemStack> remainders) {
    }

    private final ItemStackHandler inputs = new ItemStackHandler(INPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final ItemStackHandler outputs = new ItemStackHandler(OUTPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final ItemStackHandler pattern = new ItemStackHandler(GRID) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };
    private final List<StationRecipe> recipes = new ArrayList<>();
    private int progress;
    private int cursor;
    private int active = -1;
    private int pushTimer;

    /** All thirteen slots for automation: inputs take only used items, outputs only give. */
    private final IItemHandler sideHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return INPUT_SLOTS + OUTPUT_SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot < INPUT_SLOTS ? inputs.getStackInSlot(slot) : outputs.getStackInSlot(slot - INPUT_SLOTS);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot >= INPUT_SLOTS || !uses(stack)) {
                return stack;
            }
            return inputs.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot < INPUT_SLOTS ? ItemStack.EMPTY : outputs.extractItem(slot - INPUT_SLOTS, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot < INPUT_SLOTS && uses(stack);
        }
    };

    public CraftingStationBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.CRAFTING_STATION.get(), pos, state);
    }

    public ItemStackHandler inputs() {
        return inputs;
    }

    public ItemStackHandler outputs() {
        return outputs;
    }

    public ItemStackHandler pattern() {
        return pattern;
    }

    public IItemHandler sideHandler() {
        return sideHandler;
    }

    public List<StationRecipe> recipes() {
        return Collections.unmodifiableList(recipes);
    }

    public int recipeSlots() {
        return StorageStatsManager.INSTANCE.craftingStationRecipes();
    }

    public int workTicks() {
        return StorageStatsManager.INSTANCE.craftingStationTicks();
    }

    public int progress() {
        return progress;
    }

    public int activeIndex() {
        return active;
    }

    /** Whether some configured recipe takes the item. */
    public boolean uses(ItemStack stack) {
        for (StationRecipe recipe : recipes) {
            if (recipe.uses(stack)) {
                return true;
            }
        }
        return false;
    }

    // Configuration

    /**
     * Adds the recipe the pattern grid matches.
     *
     * @return an empty string on success, else {@code empty}, {@code no_match}, {@code duplicate} or {@code full}
     */
    public String saveFromPattern() {
        if (level == null) {
            return "no_match";
        }
        List<ItemStack> cells = new ArrayList<>();
        boolean any = false;
        for (int i = 0; i < GRID; i++) {
            ItemStack cell = pattern.getStackInSlot(i);
            cells.add(cell.isEmpty() ? ItemStack.EMPTY : cell.copyWithCount(1));
            any |= !cell.isEmpty();
        }
        if (!any) {
            return "empty";
        }
        Optional<RecipeHolder<CraftingRecipe>> match = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, cells), level);
        if (match.isEmpty()) {
            return "no_match";
        }
        ResourceLocation id = match.get().id();
        for (StationRecipe recipe : recipes) {
            if (recipe.id().equals(id)) {
                return "duplicate";
            }
        }
        if (recipes.size() >= recipeSlots()) {
            return "full";
        }
        recipes.add(new StationRecipe(id, cells));
        recipesChanged();
        return "";
    }

    public void removeRecipe(int index) {
        if (index >= 0 && index < recipes.size()) {
            recipes.remove(index);
            active = -1;
            progress = 0;
            recipesChanged();
        }
    }

    private void recipesChanged() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    // Production view

    public ProductionProvider production() {
        return new ProductionProvider() {
            @Override
            public List<ProductionRecipe> recipes() {
                List<ProductionRecipe> list = new ArrayList<>();
                if (level == null) {
                    return list;
                }
                for (StationRecipe recipe : recipes) {
                    List<ItemStack> inputs = new ArrayList<>();
                    recipe.required().forEach((key, count) -> inputs.add(key.stack(count)));
                    ItemStack result = recipe.result(level);
                    list.add(new ProductionRecipe(recipe.id(), inputs, result.isEmpty() ? List.of() : List.of(result), Math.max(1, workTicks())));
                }
                return list;
            }

            @Override
            public Optional<ResourceLocation> active() {
                return active >= 0 && active < recipes.size() ? Optional.of(recipes.get(active).id()) : Optional.empty();
            }
        };
    }

    // Crafting

    private Optional<Plan> plan(StationRecipe recipe) {
        if (level == null) {
            return Optional.empty();
        }
        Map<ItemKey, Integer> required = recipe.required();
        if (required.isEmpty()) {
            return Optional.empty();
        }
        List<ItemKey> keys = new ArrayList<>();
        int[] counts = new int[INPUT_SLOTS];
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = inputs.getStackInSlot(i);
            keys.add(stack.isEmpty() ? null : ItemKey.of(stack));
            counts[i] = stack.getCount();
        }
        Optional<int[]> take = IngredientGatherer.gather(keys, counts, required);
        if (take.isEmpty()) {
            return Optional.empty();
        }
        Optional<RecipeHolder<CraftingRecipe>> holder = recipe.resolve(level);
        if (holder.isEmpty()) {
            return Optional.empty();
        }
        CraftingInput input = CraftingInput.of(3, 3, recipe.template());
        CraftingRecipe crafting = holder.get().value();
        if (!crafting.matches(input, level)) {
            return Optional.empty();
        }
        ItemStack result = crafting.assemble(input, level.registryAccess());
        NonNullList<ItemStack> remainders = crafting.getRemainingItems(input);
        if (!fits(result, remainders)) {
            return Optional.empty();
        }
        return Optional.of(new Plan(take.get(), result, remainders));
    }

    private boolean fits(ItemStack result, NonNullList<ItemStack> remainders) {
        ItemStackHandler scratch = new ItemStackHandler(OUTPUT_SLOTS);
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            scratch.setStackInSlot(i, outputs.getStackInSlot(i).copy());
        }
        if (!ItemHandlerHelper.insertItem(scratch, result.copy(), false).isEmpty()) {
            return false;
        }
        for (ItemStack remainder : remainders) {
            if (!remainder.isEmpty() && !ItemHandlerHelper.insertItem(scratch, remainder.copy(), false).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** @return whether the station is working */
    private boolean tick() {
        if (recipes.isEmpty()) {
            progress = 0;
            active = -1;
            return false;
        }
        if (active < 0 || active >= recipes.size() || plan(recipes.get(active)).isEmpty()) {
            active = RoundRobin.next(recipes.size(), cursor, i -> plan(recipes.get(i)).isPresent());
        }
        if (active < 0) {
            progress = 0;
            return false;
        }
        progress++;
        if (progress >= workTicks()) {
            progress = 0;
            finish();
        }
        setChanged();
        return true;
    }

    private void finish() {
        Optional<Plan> plan = plan(recipes.get(active));
        if (plan.isEmpty()) {
            active = -1;
            return;
        }
        int[] take = plan.get().take();
        for (int i = 0; i < INPUT_SLOTS; i++) {
            if (take[i] > 0) {
                inputs.extractItem(i, take[i], false);
            }
        }
        ItemHandlerHelper.insertItem(outputs, plan.get().result(), false);
        for (ItemStack remainder : plan.get().remainders()) {
            if (!remainder.isEmpty()) {
                ItemHandlerHelper.insertItem(outputs, remainder.copy(), false);
            }
        }
        cursor = active + 1;
        active = -1;
    }

    private void pushOutputs() {
        if (++pushTimer < PUSH_INTERVAL || level == null) {
            return;
        }
        pushTimer = 0;
        for (Direction direction : Direction.values()) {
            IItemHandler neighbour = level.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(direction), direction.getOpposite());
            if (neighbour == null) {
                continue;
            }
            for (int i = 0; i < OUTPUT_SLOTS; i++) {
                ItemStack stack = outputs.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    ItemStack left = ItemHandlerHelper.insertItem(neighbour, stack.copy(), false);
                    if (left.getCount() != stack.getCount()) {
                        outputs.setStackInSlot(i, left);
                    }
                }
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CraftingStationBlockEntity station) {
        boolean working = station.tick();
        station.pushOutputs();
        if (state.hasProperty(CraftingStationBlock.LIT) && state.getValue(CraftingStationBlock.LIT) != working) {
            level.setBlock(pos, state.setValue(CraftingStationBlock.LIT, working), Block.UPDATE_CLIENTS);
        }
    }

    /** Drops every item in the inputs and outputs. */
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStackHandler handler : List.of(inputs, outputs)) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    Block.popResource(level, pos, stack);
                    handler.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        }
    }

    // Menu

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CraftingStationMenu(containerId, playerInventory, this);
    }

    // Persistence and sync

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inputs", inputs.serializeNBT(registries));
        tag.put("Outputs", outputs.serializeNBT(registries));
        tag.put("Pattern", pattern.serializeNBT(registries));
        tag.putInt("Progress", progress);
        tag.putInt("Cursor", cursor);
        tag.put("Recipes", saveRecipes(registries));
    }

    private ListTag saveRecipes(HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (StationRecipe recipe : recipes) {
            list.add(recipe.save(registries));
        }
        return list;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inputs")) {
            inputs.deserializeNBT(registries, tag.getCompound("Inputs"));
        }
        if (tag.contains("Outputs")) {
            outputs.deserializeNBT(registries, tag.getCompound("Outputs"));
        }
        if (tag.contains("Pattern")) {
            pattern.deserializeNBT(registries, tag.getCompound("Pattern"));
        }
        progress = tag.getInt("Progress");
        cursor = tag.getInt("Cursor");
        recipes.clear();
        for (Tag entry : tag.getList("Recipes", Tag.TAG_COMPOUND)) {
            StationRecipe.load((CompoundTag) entry, registries).ifPresent(recipes::add);
        }
        active = -1;
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
}
