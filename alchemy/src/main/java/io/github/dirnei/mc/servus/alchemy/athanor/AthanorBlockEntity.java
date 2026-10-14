package io.github.dirnei.mc.servus.alchemy.athanor;

import io.github.dirnei.mc.servus.alchemy.Principle;
import io.github.dirnei.mc.servus.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.alchemy.Principles;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.TeamOwned;
import io.github.dirnei.mc.servus.alchemy.TeamOwnership;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorLayout.Duct;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorLayout.Tank;
import io.github.dirnei.mc.servus.alchemy.journal.Journal;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlockEntities;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlocks;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyRecipes;
import io.github.dirnei.mc.servus.alchemy.structure.CopperDuctBlock;
import io.github.dirnei.mc.servus.alchemy.structure.FormingEffect;
import io.github.dirnei.mc.servus.alchemy.structure.Structures;
import io.github.dirnei.mc.servus.foundation.logistics.tank.FluidTankBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineDefaults;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import io.github.dirnei.mc.servus.foundation.production.ProductionProvider;
import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.foundation.transfer.FluidView;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The alchemist's furnace: burns principles for the craft of the item lying on it, as far as its
 * structure allows. It holds no principles itself; it reads and drains the three Fluid Tanks of its build.
 */
public class AthanorBlockEntity extends MachineBlockEntity implements TeamOwned {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final Identifier ID = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "athanor");
    private static final MachineStats DEFAULTS = MachineDefaults.stats(ID.toString());
    /** Ticks between tier checks while nothing else asks for one. */
    private static final int TIER_INTERVAL = 100;

    private final TeamOwnership ownership = new TeamOwnership(this);
    private int tier;
    private int tierCooldown;
    private ItemStack shownInput = ItemStack.EMPTY;
    private ItemStack shownOutput = ItemStack.EMPTY;

    public AthanorBlockEntity(BlockPos pos, BlockState state) {
        super(ServusAlchemyBlockEntities.ATHANOR.get(), pos, state, 2);
    }

    public int tier() {
        return tier;
    }

    public String tierName() {
        return AthanorTiers.nameOf(AthanorTierData.INSTANCE.tiers(), tier);
    }

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
                Direction toAthanor = Direction.getApproximateNearest(-duct.dx(), 0, -duct.dz());
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
    private List<FluidView> tankHandlers() {
        List<FluidView> handlers = new ArrayList<>();
        if (level == null || level.isClientSide()) {
            return handlers;
        }
        for (Tank tank : tankLayout()) {
            FluidView handler = FluidView.of(level.getCapability(Capabilities.Fluid.BLOCK, tankPos(tank), Direction.UP));
            if (handler != null) {
                handlers.add(handler);
            }
        }
        return handlers;
    }

    /** What the three tanks hold, by principle; any tank may hold any principle. */
    public Principles principles() {
        Principles held = Principles.NONE;
        for (FluidView handler : tankHandlers()) {
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
        List<FluidView> handlers = tankHandlers();
        for (Principle principle : Principle.values()) {
            int left = amounts.get(principle);
            for (FluidView handler : handlers) {
                if (left <= 0) {
                    break;
                }
                FluidStack wanted = new FluidStack(PrincipleFluids.fluid(principle), left);
                left -= handler.drain(wanted, false).getAmount();
            }
        }
    }

    @Override
    public TeamOwnership ownership() {
        return ownership;
    }

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
            player.sendOverlayMessage(Component.translatable("message.servus_alchemy.athanor.formed"));
            return;
        }
        if (missingHearthBrick() != null) {
            player.sendSystemMessage(Component.translatable("message.servus_alchemy.athanor.missing"));
            return;
        }
        Tank tank = missingTank();
        if (tank != null) {
            player.sendSystemMessage(Component.translatable("message.servus_alchemy.athanor.missing_tank", sideName(tank.side())));
            return;
        }
        Duct duct = missingDuct();
        if (duct != null) {
            player.sendSystemMessage(Component.translatable("message.servus_alchemy.athanor.missing_duct", sideName(duct.side())));
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
        player.sendOverlayMessage(Component.translatable("message.servus_alchemy.athanor.formed"));
    }

    private static Component sideName(Enum<?> side) {
        return Component.translatable("message.servus_alchemy.athanor.tank." + side.name().toLowerCase(Locale.ROOT));
    }

    /** Unforms a formed Athanor whose hearth lost a brick or that lost a tank or a duct. */
    public void checkHearth() {
        if (level != null && !level.isClientSide() && formed() && (missingHearthBrick() != null || missingTank() != null || missingDuct() != null)) {
            setFormed(false);
        }
    }

    /** Unforms a formed Athanor, for when the space above it is taken. */
    public void unform() {
        if (level != null && !level.isClientSide()) {
            setFormed(false);
        }
    }

    private void setFormed(boolean formed) {
        BlockState state = level.getBlockState(worldPosition);
        if (state.getBlock() instanceof AthanorBlock && state.getValue(AthanorBlock.FORMED) != formed) {
            level.setBlock(worldPosition, state.setValue(AthanorBlock.FORMED, formed), Block.UPDATE_ALL);
            setDucts(level, worldPosition, state.getValue(MachineBlock.FACING), formed);
            setCrown(level, worldPosition, formed);
        }
    }

    /** Places the crown in the free space above a formed Athanor, or takes it away. */
    public static void setCrown(Level level, BlockPos pos, boolean formed) {
        BlockPos above = pos.above();
        BlockState state = level.getBlockState(above);
        if (formed && (state.isAir() || state.canBeReplaced())) {
            level.setBlock(above, ServusAlchemyBlocks.ATHANOR_CROWN.get().defaultBlockState(), Block.UPDATE_ALL);
        } else if (!formed && state.getBlock() instanceof AthanorCrownBlock) {
            level.setBlock(above, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private Optional<RecipeHolder<AthanorRecipe>> currentHolder() {
        ItemStack stack = inventory.getStackInSlot(SLOT_INPUT);
        if (level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return ServusRecipes.first(level, ServusAlchemyRecipes.ATHANOR.get(), new SingleRecipeInput(stack));
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
            // The periodic tier may be stale; a craft starts on the structure as it stands.
            checkTier();
        }
        if (recipe.isEmpty() || !canCraft(recipe.get())) {
            resetProgress();
            return false;
        }
        if (stepProgress()) {
            finishCraft(recipe.get());
        }
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

    @Override
    protected Identifier machineId() {
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
        return ServusRecipes.all(level, ServusAlchemyRecipes.ATHANOR.get()).stream()
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
                for (RecipeHolder<AthanorRecipe> holder : ServusRecipes.all(level, ServusAlchemyRecipes.ATHANOR.get())) {
                    AthanorRecipe recipe = holder.value();
                    ItemStack item = ServusRecipes.firstItem(recipe.input().ingredient());
                    if (recipe.tier() > tier || item.isEmpty()) {
                        continue;
                    }
                    recipes.add(new ProductionRecipe(holder.id().identifier(), List.of(item.copyWithCount(recipe.input().count())),
                            List.of(recipe.result().copy()), Math.max(1, recipe.time())));
                }
                recipes.sort(Comparator.comparing(recipe -> recipe.id().toString()));
                return recipes;
            }

            @Override
            public Optional<Identifier> active() {
                return currentHolder().filter(holder -> holder.value().tier() <= tier).map(holder -> holder.id().identifier());
            }
        };
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        output.putInt("Tier", tier);
        ownership.save(tag);
        output.store(tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag tag = ValueIo.whole(input);
        tier = input.getIntOr("Tier", 0);
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
