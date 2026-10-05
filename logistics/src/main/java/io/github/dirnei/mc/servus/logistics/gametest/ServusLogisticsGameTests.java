package io.github.dirnei.mc.servus.logistics.gametest;

import io.github.dirnei.mc.servus.logistics.ServusLogisticsIds;
import io.github.dirnei.mc.servus.logistics.duct.Channel;
import io.github.dirnei.mc.servus.logistics.duct.UtilityDuctBlockEntity;
import io.github.dirnei.mc.servus.logistics.facade.FacadeItem;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsItems;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ServusLogisticsIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ServusLogisticsGameTests {

    private static final String EMPTY = "empty_12x6x12";
    private static final BlockPos DUCT = new BlockPos(3, 1, 3);

    private ServusLogisticsGameTests() {
    }

    /** Stone plus iron gives six stone facades, a slab gives nothing, and a duct takes and returns a facade. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void facadeRecipeAndApplication(GameTestHelper helper) {
        Optional<RecipeHolder<CraftingRecipe>> stone = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,
                CraftingInput.of(2, 1, List.of(new ItemStack(Items.STONE), new ItemStack(Items.IRON_INGOT))), helper.getLevel());
        helper.assertTrue(stone.isPresent(), "stone plus iron should match the facade recipe");
        ItemStack result = stone.get().value().assemble(
                CraftingInput.of(2, 1, List.of(new ItemStack(Items.STONE), new ItemStack(Items.IRON_INGOT))),
                helper.getLevel().registryAccess());
        helper.assertTrue(result.is(ServusLogisticsItems.FACADE.get()) && result.getCount() == 6,
                "result should be six facades");
        helper.assertTrue(FacadeItem.blockOf(result) == Blocks.STONE, "facade should remember stone");

        Optional<RecipeHolder<CraftingRecipe>> slab = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,
                CraftingInput.of(2, 1, List.of(new ItemStack(Items.STONE_SLAB), new ItemStack(Items.IRON_INGOT))), helper.getLevel());
        helper.assertTrue(slab.isEmpty() || !(slab.get().value() instanceof io.github.dirnei.mc.servus.logistics.facade.FacadeRecipe),
                "a slab must not make a facade");

        helper.setBlock(DUCT, ServusLogisticsBlocks.UTILITY_DUCT.get().defaultBlockState());
        UtilityDuctBlockEntity duct = helper.getBlockEntity(DUCT);
        duct.addChannel(Channel.ITEM);
        duct.setFacade(Blocks.STONE.defaultBlockState());
        helper.assertTrue(duct.facade() != null && duct.facade().is(Blocks.STONE), "duct should carry the stone facade");
        ItemStack removed = duct.removeFacade();
        helper.assertTrue(removed.is(ServusLogisticsItems.FACADE.get()) && FacadeItem.blockOf(removed) == Blocks.STONE,
                "removing should give the stone facade back");
        helper.assertTrue(duct.facade() == null, "duct should have no facade after removal");
        helper.succeed();
    }
}
