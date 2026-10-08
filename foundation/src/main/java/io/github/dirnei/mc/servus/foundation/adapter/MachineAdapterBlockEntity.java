package io.github.dirnei.mc.servus.foundation.adapter;

import io.github.dirnei.mc.servus.foundation.data.DataMember;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.graph.Side;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.production.ItemSupplier;
import io.github.dirnei.mc.servus.foundation.production.ProductionProvider;
import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * A Machine Adapter: reports the faced machine's recipes from its profile,
 * forwards ingredients and upkeep into the machine, offers its output, and
 * keeps the upkeep side stocked by asking suppliers on the data line.
 */
public class MachineAdapterBlockEntity extends BlockEntity implements MenuProvider {

    /** Ticks between upkeep checks. */
    public static final int CHECK_INTERVAL = 40;
    /** Ticks after which an unanswered upkeep request is given up. */
    public static final int REQUEST_TIMEOUT = 1200;
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 64;
    /** Cooking time when a recipe type has none. */
    private static final int DEFAULT_TICKS = 200;

    private ItemStack upkeepItem = ItemStack.EMPTY;
    private int upkeepLevel = MachineProfile.DEFAULT_LEVEL;
    private boolean upkeepSet;

    private int expected;
    private long deadline;
    private int timer;

    private RecipeManager cachedManager;
    private String cachedType;
    private List<ProductionRecipe> cachedRecipes = List.of();

    private final AdapterItemHandler handler = new AdapterItemHandler(this);
    private final ProductionProvider production = new ProductionProvider() {
        @Override
        public List<ProductionRecipe> recipes() {
            return MachineAdapterBlockEntity.this.recipes();
        }

        @Override
        public Optional<ResourceLocation> active() {
            return MachineAdapterBlockEntity.this.active();
        }
    };

    public MachineAdapterBlockEntity(BlockPos pos, BlockState state) {
        super(AdapterContent.BLOCK_ENTITY.get(), pos, state);
    }

    public IItemHandler itemHandler() {
        return handler;
    }

    public ProductionProvider production() {
        return production;
    }

    // The machine

    public Direction facing() {
        return getBlockState().getValue(MachineAdapterBlock.FACING);
    }

    public BlockPos machinePos() {
        return worldPosition.relative(facing());
    }

    public BlockState machineState() {
        return level == null ? null : level.getBlockState(machinePos());
    }

    public Optional<MachineProfile> profile() {
        BlockState state = machineState();
        return state == null ? Optional.empty() : MachineProfiles.INSTANCE.forState(state);
    }

    /** The machine's name, or empty when there is no block. */
    public Component machineName() {
        BlockState state = machineState();
        return state == null || state.isAir() ? Component.empty() : state.getBlock().getName();
    }

    /** The machine's own facing, north when it has none. */
    private Side machineFacing(BlockState state) {
        Direction direction = Direction.NORTH;
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            direction = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        } else if (state.hasProperty(BlockStateProperties.FACING)) {
            direction = state.getValue(BlockStateProperties.FACING);
        }
        return Conversions.toSide(direction);
    }

    /** The machine's item handler on the side with the role, if the profile has one. */
    IItemHandler machineHandler(MachineProfile.Role role) {
        if (level == null) {
            return null;
        }
        Optional<MachineProfile> profile = profile();
        if (profile.isEmpty()) {
            return null;
        }
        BlockState state = machineState();
        Optional<Side> side = ProfileSides.firstOf(profile.get(), role, machineFacing(state));
        return side.map(value -> level.getCapability(Capabilities.ItemHandler.BLOCK, machinePos(), Conversions.toDirection(value)))
                .orElse(null);
    }

    // Recipes

    private List<ProductionRecipe> recipes() {
        if (!(level instanceof ServerLevel server)) {
            return List.of();
        }
        Optional<MachineProfile> profile = profile();
        if (profile.isEmpty()) {
            return List.of();
        }
        RecipeManager manager = server.getRecipeManager();
        if (manager != cachedManager || !profile.get().recipeType().equals(cachedType)) {
            cachedManager = manager;
            cachedType = profile.get().recipeType();
            cachedRecipes = build(server, manager, cachedType);
        }
        return cachedRecipes;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static List<ProductionRecipe> build(ServerLevel level, RecipeManager manager, String typeId) {
        ResourceLocation id = ResourceLocation.tryParse(typeId);
        RecipeType<?> type = id == null ? null : BuiltInRegistries.RECIPE_TYPE.get(id);
        if (type == null) {
            return List.of();
        }
        List<ProductionRecipe> recipes = new ArrayList<>();
        Set<ResourceLocation> ids = new HashSet<>();
        for (Object entry : manager.getAllRecipesFor((RecipeType) type)) {
            RecipeHolder<?> holder = (RecipeHolder<?>) entry;
            Recipe<?> recipe = holder.value();
            List<Ingredient> ingredients = recipe.getIngredients().stream().filter(ingredient -> !ingredient.isEmpty()).toList();
            ItemStack result = recipe.getResultItem(level.registryAccess());
            if (ingredients.size() != 1 || result.isEmpty()) {
                continue;
            }
            int ticks = recipe instanceof AbstractCookingRecipe cooking ? cooking.getCookingTime() : DEFAULT_TICKS;
            for (ItemStack input : ingredients.get(0).getItems()) {
                ResourceLocation item = BuiltInRegistries.ITEM.getKey(input.getItem());
                ResourceLocation recipeId = holder.id().withSuffix("/" + item.getNamespace() + "/" + item.getPath());
                if (ids.add(recipeId)) {
                    recipes.add(new ProductionRecipe(recipeId, List.of(input.copyWithCount(1)), List.of(result.copy()), ticks));
                }
            }
        }
        recipes.sort(Comparator.comparing(recipe -> recipe.id().toString()));
        return List.copyOf(recipes);
    }

    /** The recipe of the item in the machine's ingredient side, while there is one. */
    private Optional<ResourceLocation> active() {
        IItemHandler ingredients = machineHandler(MachineProfile.Role.INGREDIENTS);
        if (ingredients == null) {
            return Optional.empty();
        }
        for (int slot = 0; slot < ingredients.getSlots(); slot++) {
            ItemStack stack = ingredients.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            for (ProductionRecipe recipe : recipes()) {
                if (ItemStack.isSameItemSameComponents(recipe.inputs().get(0), stack)) {
                    return Optional.of(recipe.id());
                }
            }
        }
        return Optional.empty();
    }

    // Upkeep

    public ItemStack upkeepItem() {
        return upkeepItem;
    }

    public int upkeepLevel() {
        return upkeepLevel;
    }

    /** Upkeep items in the machine's upkeep side right now. */
    public int upkeepCount() {
        IItemHandler upkeep = machineHandler(MachineProfile.Role.UPKEEP);
        if (upkeep == null || upkeepItem.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (int slot = 0; slot < upkeep.getSlots(); slot++) {
            ItemStack stack = upkeep.getStackInSlot(slot);
            if (ItemStack.isSameItemSameComponents(stack, upkeepItem)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /** Upkeep items asked for and not yet arrived. */
    public int expected() {
        return expected;
    }

    public void setUpkeepItem(ItemStack stack) {
        upkeepItem = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        upkeepSet = true;
        expected = 0;
        setChanged();
    }

    public void setUpkeepLevel(int level) {
        upkeepLevel = Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
        upkeepSet = true;
        setChanged();
    }

    /**
     * Puts arriving upkeep into the machine's upkeep side, up to what is expected.
     *
     * @return what is left for the ingredient side
     */
    ItemStack receiveUpkeep(ItemStack stack, boolean simulate) {
        if (expected <= 0 || upkeepItem.isEmpty() || !ItemStack.isSameItemSameComponents(stack, upkeepItem)) {
            return stack;
        }
        IItemHandler upkeep = machineHandler(MachineProfile.Role.UPKEEP);
        if (upkeep == null) {
            return stack;
        }
        int offered = Math.min(stack.getCount(), expected);
        ItemStack left = ItemHandlerHelper.insertItemStacked(upkeep, stack.copyWithCount(offered), simulate);
        int taken = offered - left.getCount();
        if (!simulate) {
            expected -= taken;
        }
        return stack.copyWithCount(stack.getCount() - taken);
    }

    /** Takes the profile's default upkeep the first time the adapter meets a machine with a profile. */
    private void applyDefaults(MachineProfile profile) {
        if (upkeepSet) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(profile.upkeepItem());
        upkeepItem = id == null || profile.upkeepItem().isEmpty() ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.get(id));
        upkeepLevel = Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, profile.upkeepLevel()));
        upkeepSet = true;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineAdapterBlockEntity adapter) {
        if (++adapter.timer >= CHECK_INTERVAL) {
            adapter.timer = 0;
            adapter.checkUpkeep((ServerLevel) level);
        }
    }

    /** Asks a supplier on the data line for the missing upkeep, unless a request is still on its way. */
    void checkUpkeep(ServerLevel server) {
        Optional<MachineProfile> profile = profile();
        if (profile.isEmpty()) {
            return;
        }
        applyDefaults(profile.get());
        if (upkeepItem.isEmpty()) {
            return;
        }
        if (expected > 0 && server.getGameTime() > deadline) {
            expected = 0;
        }
        int missing = upkeepLevel - upkeepCount();
        if (expected > 0 || missing <= 0) {
            return;
        }
        Optional<DataNetwork> network = DataNetworks.networkAt(server, worldPosition);
        if (network.isEmpty()) {
            return;
        }
        List<DataMember> members = new ArrayList<>(network.get().dataMembers());
        members.sort(Comparator.comparingLong(member -> member.pos().asLong()));
        for (DataMember member : members) {
            if (!server.isLoaded(member.pos())) {
                continue;
            }
            ItemSupplier supplier = server.getCapability(ServusFoundationCapabilities.SUPPLIER, member.pos(), null);
            if (supplier == null) {
                continue;
            }
            int answer = supplier.supply(worldPosition, upkeepItem, missing);
            if (answer > 0) {
                expected = answer;
                deadline = server.getGameTime() + REQUEST_TIMEOUT;
                return;
            }
        }
    }

    // Saving

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!upkeepItem.isEmpty()) {
            tag.put("upkeep", upkeepItem.save(registries));
        }
        tag.putInt("level", upkeepLevel);
        tag.putBoolean("set", upkeepSet);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        upkeepItem = tag.contains("upkeep") ? ItemStack.parseOptional(registries, tag.getCompound("upkeep")) : ItemStack.EMPTY;
        upkeepLevel = tag.contains("level") ? tag.getInt("level") : MachineProfile.DEFAULT_LEVEL;
        upkeepSet = tag.getBoolean("set");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // Menu

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.servus_foundation.machine_adapter");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new AdapterMenu(containerId, inventory, this);
    }
}
