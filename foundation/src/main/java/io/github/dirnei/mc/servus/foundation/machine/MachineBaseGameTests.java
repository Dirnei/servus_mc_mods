package io.github.dirnei.mc.servus.foundation.machine;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.data.DataMember;
import io.github.dirnei.mc.servus.foundation.data.DataNetworks;
import io.github.dirnei.mc.servus.foundation.data.DataRole;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.machine.generator.CoalGeneratorBlockEntity;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import io.github.dirnei.mc.servus.foundation.production.UpkeepHolder;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/** The shared machine rules and fuel upkeep, on Foundation's own burner machines. */
@GameTestHolder(ServusFoundationIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MachineBaseGameTests {

    private static final String EMPTY = "empty_12x6x12";
    private static final BlockPos POS = new BlockPos(3, 2, 3);

    private MachineBaseGameTests() {
    }

    private static BlockState facing(Block block, Direction direction) {
        return block.defaultBlockState().setValue(MachineBlock.FACING, direction);
    }

    private static UpkeepHolder upkeep(GameTestHelper helper, BlockPos pos, Direction side) {
        return helper.getLevel().getCapability(ServusFoundationCapabilities.UPKEEP, helper.absolutePos(pos), side);
    }

    /** A Pull side takes what the machine accepts from the neighbour and leaves the rest. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void machinePullsFromNeighbour(GameTestHelper helper) {
        helper.setBlock(POS, facing(MachineContent.COAL_GENERATOR.get(), Direction.NORTH));
        BlockPos chestPos = POS.west();
        helper.setBlock(chestPos, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = helper.getBlockEntity(chestPos);
        chest.setItem(0, new ItemStack(Items.COAL, 4));
        chest.setItem(1, new ItemStack(Items.DIAMOND, 3));
        MachineBlockEntity generator = helper.getBlockEntity(POS);
        helper.assertTrue(generator.relativeSide(Direction.WEST) == RelativeSide.RIGHT,
                "west is the right side of a machine facing north, as the player sees it");
        generator.sideConfig().set(RelativeSide.RIGHT, SideMode.PULL);
        helper.succeedWhen(() -> {
            helper.assertTrue(chest.countItem(Items.COAL) == 0, "the coal should have been pulled in");
            helper.assertTrue(chest.countItem(Items.DIAMOND) == 3, "the diamonds stay in the chest");
        });
    }

    /** The front of a machine takes nothing, gives nothing and cannot be set to anything else. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void machineFrontIsClosed(GameTestHelper helper) {
        helper.setBlock(POS, facing(MachineContent.COAL_GENERATOR.get(), Direction.NORTH));
        MachineBlockEntity generator = helper.getBlockEntity(POS);
        helper.assertTrue(generator.sideMode(Direction.NORTH) == SideMode.DISABLED, "the front starts closed");
        helper.assertTrue(generator.cycleFace(Direction.NORTH) == SideMode.DISABLED, "the front cannot be cycled");
        generator.sideConfig().set(RelativeSide.FRONT, SideMode.INPUT);
        helper.assertTrue(generator.sideMode(Direction.NORTH) == SideMode.DISABLED, "the front cannot be set");
        IItemHandler front = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(POS), Direction.NORTH);
        helper.assertTrue(front == null || front.insertItem(0, new ItemStack(Items.COAL, 4), false).getCount() == 4,
                "nothing goes in at the front");
        helper.succeed();
    }

    /** Foundation's burner machines keep coal at 16 in their fuel slot, on every side. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void burnerMachinesExposeUpkeep(GameTestHelper helper) {
        List<Block> burners = List.of(MachineContent.BURNER_PUMP.get(), MachineContent.COAL_GENERATOR.get());
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
        BlockPos generatorPos = new BlockPos(4, 2, 3);
        CoalGeneratorBlockEntity generator = helper.getBlockEntity(generatorPos);
        generator.inventory().setStackInSlot(CoalGeneratorBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, 5));
        helper.assertTrue(upkeep(helper, generatorPos, Direction.NORTH).upkeepCount() == 5, "5 coal in the fuel slot count 5");
        generator.inventory().setStackInSlot(CoalGeneratorBlockEntity.SLOT_FUEL, new ItemStack(Items.CHARCOAL, 5));
        helper.assertTrue(upkeep(helper, generatorPos, Direction.NORTH).upkeepCount() == 0, "charcoal is not the upkeep item");
        helper.assertTrue(upkeep(helper, POS.east(4), Direction.NORTH) == null, "air has no upkeep");
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

    /** On a data line the Coal Generator is an upkeep only member. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void generatorJoinsTheDataLineForUpkeep(GameTestHelper helper) {
        BlockPos generator = new BlockPos(5, 2, 3);
        BlockPos duct = new BlockPos(4, 2, 3);
        helper.setBlock(generator, MachineContent.COAL_GENERATOR.get().defaultBlockState());
        dataDuct(helper, duct);
        helper.succeedWhen(() -> {
            DataMember generatorMember = member(helper, duct, generator).orElse(null);
            helper.assertTrue(generatorMember != null && generatorMember.roles().equals(Set.of(DataRole.UPKEEP)),
                    "the generator should be upkeep only, got " + generatorMember);
        });
    }
}
