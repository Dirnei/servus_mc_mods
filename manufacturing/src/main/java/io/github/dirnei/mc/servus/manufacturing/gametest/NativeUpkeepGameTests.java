package io.github.dirnei.mc.servus.manufacturing.gametest;

import io.github.dirnei.mc.servus.foundation.data.DataMember;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.DataRole;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.production.ProductionProvider;
import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.foundation.production.UpkeepHolder;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.machine.crusher.CrusherBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.smelter.BurnerSmelterBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlocks;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Manufacturing machines on the data line: fuel upkeep and producers for orders. */
@GameTestHolder(ServusManufacturingIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NativeUpkeepGameTests {

    private static final String EMPTY = "empty_12x6x12";

    private NativeUpkeepGameTests() {
    }

    private static UpkeepHolder upkeep(GameTestHelper helper, BlockPos pos, Direction side) {
        return helper.getLevel().getCapability(ServusFoundationCapabilities.UPKEEP, helper.absolutePos(pos), side);
    }

    private static ProductionProvider production(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getCapability(ServusFoundationCapabilities.PRODUCTION, helper.absolutePos(pos), Direction.UP);
    }

    /** Every burner machine keeps coal at 16 in its fuel slot, on every side. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void burnerMachinesExposeUpkeep(GameTestHelper helper) {
        List<Block> burners = List.of(ServusManufacturingBlocks.BURNER_EXTRACTOR.get(), ServusManufacturingBlocks.BURNER_SMELTER.get(),
                ServusManufacturingBlocks.BURNER_PUMP.get(), ServusManufacturingBlocks.COAL_GENERATOR.get());
        for (int i = 0; i < burners.size(); i++) {
            BlockPos pos = new BlockPos(2 + 2 * i, 2, 3);
            helper.setBlock(pos, burners.get(i).defaultBlockState());
            for (Direction side : Direction.values()) {
                UpkeepHolder holder = upkeep(helper, pos, side);
                helper.assertTrue(holder != null, burners.get(i) + " should expose upkeep on " + side);
                helper.assertTrue(holder.upkeepItem().is(Items.COAL) && holder.upkeepLevel() == 16,
                        burners.get(i) + " should keep 16 coal, got " + holder.upkeepItem() + " " + holder.upkeepLevel());
                helper.assertTrue(holder.upkeepCount() == 0, "an empty fuel slot counts 0");
            }
        }
        BlockPos smelter = new BlockPos(4, 2, 3);
        BurnerSmelterBlockEntity entity = helper.getBlockEntity(smelter);
        entity.inventory().setStackInSlot(BurnerSmelterBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, 5));
        helper.assertTrue(upkeep(helper, smelter, Direction.NORTH).upkeepCount() == 5, "5 coal in the fuel slot count 5");
        entity.inventory().setStackInSlot(BurnerSmelterBlockEntity.SLOT_FUEL, new ItemStack(Items.CHARCOAL, 5));
        helper.assertTrue(upkeep(helper, smelter, Direction.NORTH).upkeepCount() == 0, "charcoal is not the upkeep item");
        helper.succeed();
    }

    /** Electric machines keep no upkeep. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void electricExtractorHasNoUpkeep(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(pos, ServusManufacturingBlocks.ELECTRIC_EXTRACTOR.get().defaultBlockState());
        helper.assertTrue(upkeep(helper, pos, Direction.NORTH) == null, "the Electric Extractor has no upkeep");
        BlockPos crusher = new BlockPos(5, 2, 3);
        helper.setBlock(crusher, ServusManufacturingBlocks.CRUSHER.get().defaultBlockState());
        helper.assertTrue(upkeep(helper, crusher, Direction.NORTH) == null, "the Crusher has no upkeep");
        helper.succeed();
    }

    /** The Burner Smelter reports smelting recipes at its own work time and the recipe of its input as active. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void smelterReportsSmeltingProduction(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(pos, ServusManufacturingBlocks.BURNER_SMELTER.get().defaultBlockState());
        ProductionProvider provider = production(helper, pos);
        helper.assertTrue(provider != null, "the smelter should expose the production capability");
        ProductionRecipe iron = provider.recipes().stream()
                .filter(recipe -> recipe.inputs().get(0).is(Items.RAW_IRON) && recipe.outputs().get(0).is(Items.IRON_INGOT))
                .findFirst().orElse(null);
        helper.assertTrue(iron != null, "raw iron to iron ingot should be reported");
        helper.assertTrue(iron.inputs().get(0).getCount() == 1 && iron.outputs().get(0).getCount() == 1 && iron.ticks() == 80,
                "one raw iron makes one ingot in 80 ticks, got " + iron);
        helper.assertTrue(provider.active().isEmpty(), "idle: nothing active");
        BurnerSmelterBlockEntity smelter = helper.getBlockEntity(pos);
        smelter.inventory().setStackInSlot(BurnerSmelterBlockEntity.SLOT_INPUT, new ItemStack(Items.RAW_IRON, 3));
        helper.assertTrue(provider.active().filter(id -> id.equals(iron.id())).isPresent(), "raw iron in the input is active");
        helper.succeed();
    }

    /** The Crusher reports its crushing recipes with input count and time. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void crusherReportsCrushingProduction(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(pos, ServusManufacturingBlocks.CRUSHER.get().defaultBlockState());
        ProductionProvider provider = production(helper, pos);
        helper.assertTrue(provider != null, "the crusher should expose the production capability");
        ProductionRecipe grit = provider.recipes().stream()
                .filter(recipe -> recipe.inputs().get(0).is(Items.RAW_IRON))
                .findFirst().orElse(null);
        helper.assertTrue(grit != null && grit.outputs().get(0).is(ServusManufacturingItems.IRON_GRIT.get())
                        && grit.outputs().get(0).getCount() == 2 && grit.ticks() > 0,
                "raw iron should crush to two iron grit, got " + grit);
        CrusherBlockEntity crusher = helper.getBlockEntity(pos);
        crusher.inventory().setStackInSlot(CrusherBlockEntity.SLOT_INPUT, new ItemStack(Items.RAW_IRON));
        helper.assertTrue(provider.active().filter(id -> id.equals(grit.id())).isPresent(), "raw iron in the input is active");
        helper.succeed();
    }

    private static void dataDuct(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ServusLogisticsBlocks.UTILITY_DUCT.get().defaultBlockState());
        UtilityDuctBlockEntity duct = helper.getBlockEntity(pos);
        duct.addLine(Line.DATA);
        BlockPos absolute = helper.absolutePos(pos);
        BlockState state = helper.getLevel().getBlockState(absolute);
        helper.getLevel().setBlock(absolute, Block.updateFromNeighbourShapes(state, helper.getLevel(), absolute), Block.UPDATE_ALL);
    }

    private static Optional<DataMember> member(GameTestHelper helper, BlockPos duct, BlockPos pos) {
        return DataNetworks.networkAt(helper.getLevel(), helper.absolutePos(duct)).flatMap(network ->
                network.dataMembers().stream().filter(member -> member.pos().equals(helper.absolutePos(pos))).findFirst());
    }

    /** On a data line the smelter is a producer with upkeep and the Coal Generator an upkeep only member. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void burnersJoinTheDataLine(GameTestHelper helper) {
        BlockPos smelter = new BlockPos(3, 2, 3);
        BlockPos generator = new BlockPos(5, 2, 3);
        BlockPos duct = new BlockPos(4, 2, 3);
        helper.setBlock(smelter, ServusManufacturingBlocks.BURNER_SMELTER.get().defaultBlockState());
        helper.setBlock(generator, ServusManufacturingBlocks.COAL_GENERATOR.get().defaultBlockState());
        dataDuct(helper, duct);
        helper.succeedWhen(() -> {
            DataMember smelterMember = member(helper, duct, smelter).orElse(null);
            DataMember generatorMember = member(helper, duct, generator).orElse(null);
            helper.assertTrue(smelterMember != null && smelterMember.roles().equals(Set.of(DataRole.PRODUCER, DataRole.UPKEEP)),
                    "the smelter should be producer and upkeep, got " + smelterMember);
            helper.assertTrue(generatorMember != null && generatorMember.roles().equals(Set.of(DataRole.UPKEEP)),
                    "the generator should be upkeep only, got " + generatorMember);
        });
    }

    /** What packets do to a producer: raw iron and coal in through one face, the ingot out through it. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void smelterTakesOrderItemsThroughAFace(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(pos, ServusManufacturingBlocks.BURNER_SMELTER.get().defaultBlockState());
        IItemHandler face = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), Direction.EAST);
        helper.assertTrue(face != null, "the smelter should expose an item handler on its side");
        helper.assertTrue(ItemHandlerHelper.insertItemStacked(face, new ItemStack(Items.COAL, 4), false).isEmpty(), "coal goes in");
        helper.assertTrue(ItemHandlerHelper.insertItemStacked(face, new ItemStack(Items.RAW_IRON, 1), false).isEmpty(), "raw iron goes in");
        BurnerSmelterBlockEntity smelter = helper.getBlockEntity(pos);
        helper.assertTrue(smelter.inventory().getStackInSlot(BurnerSmelterBlockEntity.SLOT_FUEL).is(Items.COAL)
                && smelter.inventory().getStackInSlot(BurnerSmelterBlockEntity.SLOT_INPUT).is(Items.RAW_IRON), "coal is fuel, raw iron the input");
        helper.succeedWhen(() -> {
            ItemStack out = ItemStack.EMPTY;
            for (int slot = 0; slot < face.getSlots() && out.isEmpty(); slot++) {
                out = face.extractItem(slot, 1, true);
            }
            helper.assertTrue(out.is(Items.IRON_INGOT), "the ingot should be extractable through the side, got " + out);
        });
    }
}
