package io.github.dirnei.mc.servus.storage.crafting;

import io.github.dirnei.mc.servus.lib.block.SidedBlockEntity;
import io.github.dirnei.mc.servus.lib.side.RelativeSide;
import io.github.dirnei.mc.servus.lib.side.SideConfig;
import io.github.dirnei.mc.servus.lib.side.SideMode;
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
public class CraftingStationBlockEntity extends BlockEntity implements MenuProvider, SidedBlockEntity {

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

    private final SideConfig sides = SideConfig.machine();
    private final java.util.Map<Direction, IItemHandler> sideHandlers = new java.util.EnumMap<>(Direction.class);
    private final IItemHandler anySide = new SideHandler(null);

    /** All thirteen slots for automation: inputs take only used items, outputs only give, both only where the side allows it. */
    private final class SideHandler implements IItemHandler {
        private final Direction side;

        private SideHandler(Direction side) {
            this.side = side;
        }

        private SideMode mode() {
            return side == null ? SideMode.AUTO : sideMode(side);
        }

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
            if (slot >= INPUT_SLOTS || !mode().allowsInsert() || !uses(stack)) {
                return stack;
            }
            return inputs.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot < INPUT_SLOTS || !mode().allowsExtract() ? ItemStack.EMPTY : outputs.extractItem(slot - INPUT_SLOTS, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot < INPUT_SLOTS && mode().allowsInsert() && uses(stack);
        }
    }

    public CraftingStationBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.CRAFTING_STATION.get(), pos, state);
        // The front is where the player stands: never an item side.
        sides.lock(RelativeSide.FRONT, SideMode.DISABLED);
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

    /** The handler a neighbour on that side sees, or the unrestricted view for a null side. */
    public IItemHandler handlerFor(Direction side) {
        if (side == null) {
            return anySide;
        }
        return sideHandlers.computeIfAbsent(side, SideHandler::new);
    }

    // Sides

    @Override
    public SideConfig sideConfig() {
        return sides;
    }

    @Override
    public Direction facing() {
        return getBlockState().hasProperty(CraftingStationBlock.FACING) ? getBlockState().getValue(CraftingStationBlock.FACING) : Direction.NORTH;
    }

    @Override
    public SideMode cycleSide(RelativeSide side) {
        SideMode mode = sides.cycle(side);
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.invalidateCapabilities(worldPosition);
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
        return mode;
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
        return saveFromPattern(-1);
    }

    /**
     * Adds the recipe the pattern grid matches, or replaces recipe {@code replace}
     * with it when {@code replace} is a configured index.
     *
     * @return an empty string on success, else {@code empty}, {@code no_match}, {@code duplicate} or {@code full}
     */
    public String saveFromPattern(int replace) {
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
        Optional<RecipeHolder<CraftingRecipe>> match = match(level, cells);
        if (match.isEmpty()) {
            return "no_match";
        }
        ResourceLocation id = match.get().id();
        boolean replacing = replace >= 0 && replace < recipes.size();
        for (int i = 0; i < recipes.size(); i++) {
            if (recipes.get(i).id().equals(id) && !(replacing && i == replace)) {
                return "duplicate";
            }
        }
        if (replacing) {
            recipes.set(replace, new StationRecipe(id, cells));
            active = -1;
            progress = 0;
        } else {
            if (recipes.size() >= recipeSlots()) {
                return "full";
            }
            recipes.add(new StationRecipe(id, cells));
        }
        recipesChanged();
        return "";
    }

    /** The vanilla crafting recipe a 3 by 3 grid matches, for saving and for the editor's preview. */
    public static Optional<RecipeHolder<CraftingRecipe>> match(Level level, List<ItemStack> cells) {
        if (cells.stream().allMatch(ItemStack::isEmpty)) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, cells), level);
    }

    /** Fills the pattern grid with a template, for editing a recipe. */
    public void loadPattern(List<ItemStack> template) {
        for (int i = 0; i < GRID; i++) {
            pattern.setStackInSlot(i, i < template.size() ? template.get(i).copy() : ItemStack.EMPTY);
        }
    }

    public void clearPattern() {
        for (int i = 0; i < GRID; i++) {
            pattern.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    /** The template laid out by a vanilla crafting recipe, one ingredient item per cell, for the recipe viewer. */
    public static Optional<List<ItemStack>> templateOf(Level level, ResourceLocation id) {
        Optional<RecipeHolder<?>> holder = level.getRecipeManager().byKey(id);
        if (holder.isEmpty() || !(holder.get().value() instanceof CraftingRecipe recipe)) {
            return Optional.empty();
        }
        List<ItemStack> cells = new ArrayList<>(GRID);
        for (int i = 0; i < GRID; i++) {
            cells.add(ItemStack.EMPTY);
        }
        int width = recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped ? shaped.getWidth() : 3;
        List<net.minecraft.world.item.crafting.Ingredient> ingredients = recipe.getIngredients();
        for (int i = 0; i < ingredients.size() && i < GRID; i++) {
            ItemStack[] items = ingredients.get(i).getItems();
            if (items.length > 0) {
                int cell = width == 3 ? i : (i / width) * 3 + i % width;
                if (cell < GRID) {
                    cells.set(cell, items[0].copyWithCount(1));
                }
            }
        }
        return Optional.of(cells);
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
            SideMode mode = sideMode(direction);
            if (!mode.pushes() && !mode.pulls()) {
                continue;
            }
            IItemHandler neighbour = level.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(direction), direction.getOpposite());
            if (neighbour == null) {
                continue;
            }
            if (mode.pulls()) {
                pullFrom(neighbour);
            }
            if (!mode.pushes()) {
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

    /** Takes one stack a configured recipe uses from the neighbour into the inputs. */
    private void pullFrom(IItemHandler neighbour) {
        for (int from = 0; from < neighbour.getSlots(); from++) {
            ItemStack offered = neighbour.extractItem(from, 64, true);
            if (offered.isEmpty() || !uses(offered)) {
                continue;
            }
            int room = offered.getCount() - ItemHandlerHelper.insertItem(inputs, offered, true).getCount();
            if (room <= 0) {
                continue;
            }
            ItemStack taken = neighbour.extractItem(from, room, false);
            if (!taken.isEmpty()) {
                ItemHandlerHelper.insertItem(inputs, taken, false);
                return;
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
        tag.putByteArray("Sides", sides.toBytes());
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
        if (tag.contains("Sides")) {
            sides.fromBytes(tag.getByteArray("Sides"));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("Recipes", saveRecipes(registries));
        tag.putByteArray("Sides", sides.toBytes());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
