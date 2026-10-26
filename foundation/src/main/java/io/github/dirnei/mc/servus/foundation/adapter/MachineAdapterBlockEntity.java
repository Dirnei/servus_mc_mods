package io.github.dirnei.mc.servus.foundation.adapter;

import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.component.DataComponentGetter;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.foundation.graph.Side;
import io.github.dirnei.mc.servus.foundation.naming.MachineNameSlot;
import io.github.dirnei.mc.servus.foundation.naming.NamedMachine;
import io.github.dirnei.mc.servus.foundation.naming.NamedMachines;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.production.ProductionProvider;
import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import io.github.dirnei.mc.servus.foundation.production.SingleInputRecipes;
import io.github.dirnei.mc.servus.foundation.production.UpkeepHolder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import io.github.dirnei.mc.servus.foundation.transfer.ItemView;
import io.github.dirnei.mc.servus.foundation.material.MaterialContent;
import io.github.dirnei.mc.servus.foundation.material.MaterialHolder;
import io.github.dirnei.mc.servus.foundation.material.MaterialSlot;
import net.minecraft.core.component.DataComponentMap;
import net.neoforged.neoforge.model.data.ModelData;

/**
 * A Machine Adapter: reports the faced machine's recipes from its profile,
 * forwards ingredients and upkeep into the machine, offers its output, and
 * exposes the upkeep side's upkeep, which the data network keeps stocked.
 */
public class MachineAdapterBlockEntity extends BlockEntity implements MaterialHolder, MenuProvider, NamedMachine {

    private final MaterialSlot materialSlot = new MaterialSlot(this);
    private final MachineNameSlot nameSlot = new MachineNameSlot(this, AdapterContent.KIND);

    /** Ticks after which an unanswered upkeep request is given up. */
    public static final int REQUEST_TIMEOUT = DataNetwork.UPKEEP_TIMEOUT;
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 64;
    /** Cooking time when a recipe type has none. */
    private static final int DEFAULT_TICKS = 200;

    private ItemStack upkeepItem = ItemStack.EMPTY;
    private int upkeepLevel = MachineProfile.DEFAULT_LEVEL;
    private boolean upkeepSet;

    private int expected;
    private long deadline;

    private final SingleInputRecipes recipes = new SingleInputRecipes();

    private final AdapterItemHandler handler = new AdapterItemHandler(this);
    private final UpkeepHolder upkeep = new UpkeepHolder() {
        @Override
        public ItemStack upkeepItem() {
            return MachineAdapterBlockEntity.this.upkeepItem();
        }

        @Override
        public int upkeepLevel() {
            return MachineAdapterBlockEntity.this.upkeepLevel();
        }

        @Override
        public int upkeepCount() {
            return MachineAdapterBlockEntity.this.upkeepCount();
        }

        @Override
        public void upkeepRequested(int amount, long until) {
            expected = amount;
            deadline = until;
        }
    };
    private final ProductionProvider production = new ProductionProvider() {
        @Override
        public List<ProductionRecipe> recipes() {
            return MachineAdapterBlockEntity.this.recipes();
        }

        @Override
        public Optional<Identifier> active() {
            return MachineAdapterBlockEntity.this.active();
        }
    };

    public MachineAdapterBlockEntity(BlockPos pos, BlockState state) {
        super(AdapterContent.BLOCK_ENTITY.get(), pos, state);
    }

    public ResourceHandler<ItemResource> itemHandler() {
        return handler;
    }

    public ProductionProvider production() {
        return production;
    }

    /** The machine's upkeep, kept stocked by the data network. */
    public UpkeepHolder upkeep() {
        return upkeep;
    }

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
    ItemView machineHandler(MachineProfile.Role role) {
        if (level == null) {
            return null;
        }
        Optional<MachineProfile> profile = profile();
        if (profile.isEmpty()) {
            return null;
        }
        Optional<Side> side = ProfileSides.firstOf(profile.get(), role, machineFacing(machineState()));
        return side.map(value -> ItemView.of(level.getCapability(Capabilities.Item.BLOCK, machinePos(), Conversions.toDirection(value))))
                .orElse(null);
    }

    private List<ProductionRecipe> recipes() {
        if (!(level instanceof ServerLevel server)) {
            return List.of();
        }
        Optional<MachineProfile> profile = profile();
        if (profile.isEmpty()) {
            return List.of();
        }
        return recipes.list(server, SingleInputRecipes.type(profile.get().recipeType()),
                recipe -> recipe instanceof AbstractCookingRecipe cooking ? cooking.cookingTime() : DEFAULT_TICKS);
    }

    /** The recipe of the item in the machine's ingredient side, while there is one. */
    private Optional<Identifier> active() {
        ItemView ingredients = machineHandler(MachineProfile.Role.INGREDIENTS);
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

    public ItemStack upkeepItem() {
        applyDefaultsOnServer();
        return upkeepItem;
    }

    public int upkeepLevel() {
        applyDefaultsOnServer();
        return upkeepLevel;
    }

    /** Upkeep items in the machine's upkeep side right now. */
    public int upkeepCount() {
        ItemView upkeep = machineHandler(MachineProfile.Role.UPKEEP);
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
        if (expected > 0 && level != null && level.getGameTime() > deadline) {
            expected = 0;
        }
        return expected;
    }

    /** For tests: waits for {@code amount} upkeep items as if a supplier had answered. */
    void expect(int amount) {
        upkeep.upkeepRequested(amount, level == null ? 0 : level.getGameTime() + REQUEST_TIMEOUT);
    }

    public void setUpkeepItem(ItemStack stack) {
        upkeepItem = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        upkeepSet = true;
        expected = 0;
        setChanged();
    }

    public void setUpkeepLevel(int level) {
        upkeepLevel = clampLevel(level);
        upkeepSet = true;
        setChanged();
    }

    /**
     * Puts arriving upkeep into the machine's upkeep side, up to what is expected.
     *
     * @return what is left for the ingredient side
     */
    ItemStack receiveUpkeep(ItemStack stack, boolean simulate) {
        if (expected() <= 0 || upkeepItem.isEmpty() || !ItemStack.isSameItemSameComponents(stack, upkeepItem)) {
            return stack;
        }
        ItemView upkeep = machineHandler(MachineProfile.Role.UPKEEP);
        if (upkeep == null) {
            return stack;
        }
        int offered = Math.min(stack.getCount(), expected);
        ItemStack left = upkeep.insertItemStacked(stack.copyWithCount(offered), simulate);
        int taken = offered - left.getCount();
        if (!simulate) {
            expected -= taken;
        }
        return stack.copyWithCount(stack.getCount() - taken);
    }

    private void applyDefaultsOnServer() {
        if (!upkeepSet && level != null && !level.isClientSide()) {
            profile().ifPresent(this::applyDefaults);
        }
    }

    /** Takes the profile's default upkeep the first time the adapter meets a machine with a profile. */
    private void applyDefaults(MachineProfile profile) {
        if (upkeepSet) {
            return;
        }
        Identifier id = Identifier.tryParse(profile.upkeepItem());
        upkeepItem = id == null || profile.upkeepItem().isEmpty() ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.getValue(id));
        upkeepLevel = clampLevel(profile.upkeepLevel());
        upkeepSet = true;
        setChanged();
    }

    private static int clampLevel(int level) {
        return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        materialSlot.save(output);
        if (!upkeepItem.isEmpty()) {
            output.store("upkeep", ItemStack.CODEC, upkeepItem);
        }
        output.putInt("level", upkeepLevel);
        output.putBoolean("set", upkeepSet);
        NamedMachines.save(this, output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        materialSlot.load(input);
        upkeepItem = input.read("upkeep", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        upkeepLevel = input.getIntOr("level", MachineProfile.DEFAULT_LEVEL);
        upkeepSet = input.getBooleanOr("set", false);
        NamedMachines.load(this, input);
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

    @Override
    public MachineNameSlot nameSlot() {
        return nameSlot;
    }

    /** An unnamed adapter reads as its faced machine with its own label: "Furnace (Adapter #1)". */
    @Override
    public Component label() {
        Component faced = machineName();
        if (!name().isEmpty() || faced.getString().isEmpty()) {
            return NamedMachine.super.label();
        }
        return Component.translatable(NAMED_KEY, faced, numberLabel());
    }

    @Override
    public Component getDisplayName() {
        return NamedMachine.super.label();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new AdapterMenu(containerId, inventory, this);
    }

    @Override
    public MaterialSlot materialSlot() {
        return materialSlot;
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);
        materialSlot.apply(input.get(MaterialContent.MATERIAL.get()));
        NamedMachines.apply(this, input);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        materialSlot.collect(builder);
        NamedMachines.collect(this, builder);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        MaterialSlot.removeFromTag(output);
        NamedMachines.removeFromTag(this, output);
    }

    @Override
    public ModelData getModelData() {
        return materialSlot.modelData();
    }
}
