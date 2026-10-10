package io.github.dirnei.mc.servus.manufacturing.alchemy.athanor;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineDefaults;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import io.github.dirnei.mc.servus.foundation.machine.OwnedMachine;
import io.github.dirnei.mc.servus.foundation.production.ProductionProvider;
import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.TeamOwnership;
import io.github.dirnei.mc.servus.foundation.logistics.tank.FluidTankBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principles;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorLayout.Duct;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorLayout.Tank;
import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.CopperDuctBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.Facing;
import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.Structures;
import io.github.dirnei.mc.servus.manufacturing.journal.Journal;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingRecipes;
import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.FormingEffect;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * The alchemist's furnace: burns principles for the craft of the item lying on it, as far as its
 * structure allows. It holds no principles itself; it reads and drains the three Fluid Tanks of its build.
 */
public class AthanorBlockEntity extends MachineBlockEntity implements OwnedMachine {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "athanor");
    private static final MachineStats DEFAULTS = MachineDefaults.stats(ID.toString());
    /** Ticks between tier checks while nothing else asks for one. */
    private static final int TIER_INTERVAL = 100;

    private final TeamOwnership ownership = new TeamOwnership(this::setChanged);
    private int tier;
    private int tierCooldown;
    private ItemStack shownInput = ItemStack.EMPTY;
    private ItemStack shownOutput = ItemStack.EMPTY;

    public AthanorBlockEntity(BlockPos pos, BlockState state) {
        super(ServusManufacturingBlockEntities.ATHANOR.get(), pos, state, 2);
    }

    public int tier() {
        return tier;
    }

    public String tierName() {
        return AthanorTiers.nameOf(AthanorTierData.INSTANCE.tiers(), tier);
    }

    // Tanks

    /** Where the tank on {@code tank}'s side stands, by the Athanor's front. */
    public BlockPos tankPos(Tank tank) {
        return worldPosition.offset(tank.dx(), 0, tank.dz());
    }

    public List<Tank> tankLayout() {
        return AthanorLayout.tanks(Structures.facing(getBlockState().getValue(MachineBlock.FACING)));
    }

    /** The first tank position without a Fluid Tank, or null when all three stand. Three block reads. */
    private Tank missingTank() {
        for (Tank tank : tankLayout()) {
            if (!(level.getBlockState(tankPos(tank)).getBlock() instanceof FluidTankBlock)) {
                return tank;
            }
        }
        return null;
    }

    /** Where a duct of the build stands, one block above the Athanor's level. */
    public BlockPos ductPos(Duct duct) {
        return worldPosition.offset(duct.dx(), 1, duct.dz());
    }

    /**
     * The first duct position without a Copper Duct, or null when all stand. A duct counts when it
     * is free or already this build's; over a tank stacked that high the tank takes its place. Six block reads.
     */
    private Duct missingDuct() {
        for (Duct duct : AthanorLayout.ducts(Structures.facing(getBlockState().getValue(MachineBlock.FACING)))) {
            BlockState state = level.getBlockState(ductPos(duct));
            boolean free = state.getBlock() instanceof CopperDuctBlock && (!state.getValue(CopperDuctBlock.FORMED) || CopperDuctBlock.athanorPiece(state));
            boolean stacked = duct.overTank() && state.getBlock() instanceof FluidTankBlock;
            if (!free && !stacked) {
                return duct;
            }
        }
        return null;
    }

    /** Forms the ducts of an Athanor at {@code pos} facing {@code front} into its pipes, or gives them back their straight look. */
    public static void setDucts(Level level, BlockPos pos, Direction front, boolean formed) {
        for (Duct duct : AthanorLayout.ducts(Structures.facing(front))) {
            BlockPos at = pos.offset(duct.dx(), 1, duct.dz());
            BlockState state = level.getBlockState(at);
            if (!(state.getBlock() instanceof CopperDuctBlock)) {
                continue;
            }
            BlockState next = state;
            if (formed && !state.getValue(CopperDuctBlock.FORMED)) {
                Direction toAthanor = Direction.getNearest(-duct.dx(), 0, -duct.dz());
                next = CopperDuctBlock.athanorState(state, duct.overTank() ? CopperDuctBlock.ATHANOR_OVER : CopperDuctBlock.ATHANOR_NEAR, toAthanor);
            } else if (!formed && CopperDuctBlock.athanorPiece(state)) {
                next = state.setValue(CopperDuctBlock.FORMED, false).setValue(CopperDuctBlock.PIECE, 0);
            }
            if (next != state) {
                level.setBlock(at, next, Block.UPDATE_ALL);
            }
        }
    }

    /** The fluid handlers of the three tanks that stand. Server thread only. */
    private List<IFluidHandler> tankHandlers() {
        List<IFluidHandler> handlers = new ArrayList<>();
        if (level == null || level.isClientSide()) {
            return handlers;
        }
        for (Tank tank : tankLayout()) {
            IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, tankPos(tank), Direction.UP);
            if (handler != null) {
                handlers.add(handler);
            }
        }
        return handlers;
    }

    /** What the three tanks hold, by principle; any tank may hold any principle. */
    public Principles principles() {
        Principles held = Principles.NONE;
        for (IFluidHandler handler : tankHandlers()) {
            for (int i = 0; i < handler.getTanks(); i++) {
                FluidStack fluid = handler.getFluidInTank(i);
                for (Principle principle : Principle.values()) {
                    if (!fluid.isEmpty() && fluid.getFluid().isSame(PrincipleFluids.fluid(principle))) {
                        held = held.with(principle, held.get(principle) + fluid.getAmount());
                    }
                }
            }
        }
        return held;
    }


    /** Drains {@code amounts} from the tanks, each principle from whichever tanks hold it. */
    private void drain(Principles amounts) {
        List<IFluidHandler> handlers = tankHandlers();
        for (Principle principle : Principle.values()) {
            int left = amounts.get(principle);
            for (IFluidHandler handler : handlers) {
                if (left <= 0) {
                    break;
                }
                FluidStack wanted = new FluidStack(PrincipleFluids.fluid(principle), left);
                left -= handler.drain(wanted, IFluidHandler.FluidAction.EXECUTE).getAmount();
            }
        }
    }

    // Owner

    @Override
    public Optional<UUID> owner() {
        return ownership.owner();
    }

    @Override
    public void setOwner(UUID player) {
        ownership.setOwner(level, player);
    }

    public void setTeam(UUID team) {
        ownership.setTeam(team);
    }

    public UUID team() {
        return ownership.team();
    }

    // Tier

    /** Reads the structure around the Athanor and stores the tier in the block state. A handful of block reads. */
    public void checkTier() {
        if (level == null || level.isClientSide()) {
            return;
        }
        int found = AthanorTiers.tierOf(AthanorTierData.INSTANCE.tiers(), block -> {
            BlockState around = level.getBlockState(worldPosition.offset(block.dx(), block.dy(), block.dz()));
            return BuiltInRegistries.BLOCK.getKey(around.getBlock()).toString();
        });
        tierCooldown = 0;
        checkHearth();
        if (found != tier) {
            tier = found;
            setChanged();
        }
        BlockState state = level.getBlockState(worldPosition);
        if (state.hasProperty(AthanorBlock.TIER) && state.getValue(AthanorBlock.TIER) != Math.min(found, 3)) {
            level.setBlock(worldPosition, state.setValue(AthanorBlock.TIER, Math.min(found, 3)), Block.UPDATE_CLIENTS);
        }
    }

    // Hearth

    /** Whether the Athanor stands formed on its hearth with its tanks. */
    public boolean formed() {
        BlockState state = getBlockState();
        return state.getBlock() instanceof AthanorBlock && state.getValue(AthanorBlock.FORMED);
    }

    /** The first hearth position that is not Bricks, or null when the hearth is complete. 13 block reads. */
    private BlockPos missingHearthBrick() {
        for (AthanorTiers.BlockAt brick : AthanorLayout.hearth()) {
            BlockPos at = worldPosition.offset(brick.dx(), brick.dy(), brick.dz());
            if (!level.getBlockState(at).is(Blocks.BRICKS)) {
                return at;
            }
        }
        return null;
    }

    /** The Journal's click: forms the Athanor on a complete hearth with its tanks and makes the player its owner, else says what is missing. */
    public void tryForm(Player player) {
        if (level == null || level.isClientSide()) {
            return;
        }
        if (formed()) {
            player.displayClientMessage(Component.translatable("message.servus_manufacturing.athanor.formed"), true);
            return;
        }
        if (missingHearthBrick() != null) {
            player.displayClientMessage(Component.translatable("message.servus_manufacturing.athanor.missing"), false);
            return;
        }
        Tank tank = missingTank();
        if (tank != null) {
            player.displayClientMessage(Component.translatable("message.servus_manufacturing.athanor.missing_tank",
                    Component.translatable("message.servus_manufacturing.athanor.tank." + tank.side().name().toLowerCase(java.util.Locale.ROOT))), false);
            return;
        }
        Duct duct = missingDuct();
        if (duct != null) {
            player.displayClientMessage(Component.translatable("message.servus_manufacturing.athanor.missing_duct",
                    Component.translatable("message.servus_manufacturing.athanor.tank." + duct.side().name().toLowerCase(java.util.Locale.ROOT))), false);
            return;
        }
        setFormed(true);
        List<BlockPos> cells = new ArrayList<>(List.of(worldPosition));
        AthanorLayout.hearth().forEach(brick -> cells.add(worldPosition.offset(brick.dx(), brick.dy(), brick.dz())));
        tankLayout().forEach(each -> cells.add(tankPos(each)));
        AthanorLayout.ducts(Structures.facing(getBlockState().getValue(MachineBlock.FACING))).forEach(each -> cells.add(ductPos(each)));
        FormingEffect.play(level, worldPosition, cells);
        setOwner(player.getUUID());
        checkTier();
        player.displayClientMessage(Component.translatable("message.servus_manufacturing.athanor.formed"), true);
    }

    /** Unforms a formed Athanor whose hearth lost a brick or that lost a tank or a duct. */
    public void checkHearth() {
        if (level != null && !level.isClientSide() && formed() && (missingHearthBrick() != null || missingTank() != null || missingDuct() != null)) {
            setFormed(false);
        }
    }

    private void setFormed(boolean formed) {
        BlockState state = level.getBlockState(worldPosition);
        if (state.getBlock() instanceof AthanorBlock && state.getValue(AthanorBlock.FORMED) != formed) {
            level.setBlock(worldPosition, state.setValue(AthanorBlock.FORMED, formed), Block.UPDATE_ALL);
            setDucts(level, worldPosition, state.getValue(MachineBlock.FACING), formed);
        }
    }

    // Crafting

    private Optional<RecipeHolder<AthanorRecipe>> currentHolder() {
        ItemStack stack = inventory.getStackInSlot(SLOT_INPUT);
        if (level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(ServusManufacturingRecipes.ATHANOR.get(), new SingleRecipeInput(stack), level);
    }

    private Optional<AthanorRecipe> currentRecipe() {
        return currentHolder().map(RecipeHolder::value);
    }

    private boolean canCraft(AthanorRecipe recipe) {
        return formed()
                && recipe.tier() <= tier
                && principles().covers(recipe.principles())
                && canInsertOutput(SLOT_OUTPUT, recipe.result());
    }

    @Override
    protected boolean tick() {
        syncInputIfChanged();
        if (++tierCooldown >= TIER_INTERVAL) {
            checkTier();
        }
        Optional<AthanorRecipe> recipe = currentRecipe();
        if (recipe.isPresent() && progress == 0 && canCraft(recipe.get())) {
            // A craft is about to start: read the structure now rather than trust a tier up to five seconds old.
            checkTier();
        }
        if (recipe.isEmpty() || !canCraft(recipe.get())) {
            if (progress != 0) {
                progress = 0;
                setChanged();
            }
            return false;
        }
        progress++;
        if (progress >= Math.max(1, recipe.get().time())) {
            progress = 0;
            finishCraft(recipe.get());
        }
        setChanged();
        return true;
    }

    private void finishCraft(AthanorRecipe recipe) {
        drain(recipe.principles());
        inventory.extractItem(SLOT_INPUT, recipe.input().count(), false);
        insertOutput(SLOT_OUTPUT, recipe.result().copy());
        if (level instanceof ServerLevel server) {
            Journal.recordAthanor(server, team(), recipe.result(), tierName());
        }
    }

    /** Sends the item or the result lying on top to clients when it changes, for the renderer. */
    private void syncInputIfChanged() {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        ItemStack output = inventory.getStackInSlot(SLOT_OUTPUT);
        if (level != null && (!ItemStack.isSameItemSameComponents(input, shownInput) || !ItemStack.isSameItemSameComponents(output, shownOutput))) {
            shownInput = input.copy();
            shownOutput = output.copy();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected int workTicks() {
        return currentRecipe().map(recipe -> Math.max(1, recipe.time())).orElse(0);
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
        return slot == SLOT_INPUT;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    protected boolean acceptsInput(int slot, ItemStack stack) {
        if (level == null) {
            return false;
        }
        return level.getRecipeManager().getAllRecipesFor(ServusManufacturingRecipes.ATHANOR.get()).stream()
                .anyMatch(holder -> holder.value().accepts(stack));
    }

    /** How far the current craft is, 0 to 100, for Jade. */
    public int progressPercent() {
        int work = workTicks();
        return work <= 0 ? 0 : Math.min(100, progress * 100 / work);
    }

    /** The Foundation production view: one recipe per craft the current tier allows; principles come into the build's tanks. */
    public ProductionProvider production() {
        return new ProductionProvider() {
            @Override
            public List<ProductionRecipe> recipes() {
                if (level == null || level.isClientSide()) {
                    return List.of();
                }
                List<ProductionRecipe> recipes = new ArrayList<>();
                for (RecipeHolder<AthanorRecipe> holder : level.getRecipeManager().getAllRecipesFor(ServusManufacturingRecipes.ATHANOR.get())) {
                    AthanorRecipe recipe = holder.value();
                    ItemStack[] items = recipe.input().ingredient().getItems();
                    if (recipe.tier() > tier || items.length == 0) {
                        continue;
                    }
                    recipes.add(new ProductionRecipe(holder.id(), List.of(items[0].copyWithCount(recipe.input().count())),
                            List.of(recipe.result().copy()), Math.max(1, recipe.time())));
                }
                recipes.sort(Comparator.comparing(recipe -> recipe.id().toString()));
                return recipes;
            }

            @Override
            public Optional<ResourceLocation> active() {
                return currentHolder().filter(holder -> holder.value().tier() <= tier).map(RecipeHolder::id);
            }
        };
    }

    // Persistence and sync

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Tier", tier);
        ownership.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tier = tag.getInt("Tier");
        ownership.load(tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        return tag;
    }

    /** The Athanor has no GUI. */
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return null;
    }
}
