package io.github.dirnei.mc.servus.alchemy.registry;

import io.github.dirnei.mc.servus.alchemy.CalcinerBlock;
import io.github.dirnei.mc.servus.alchemy.CoagulatorBlock;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorBlock;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorCrownBlock;
import io.github.dirnei.mc.servus.alchemy.calciner.CalcinerLayout.Role;
import io.github.dirnei.mc.servus.alchemy.calciner.CalcinerPartBlock;
import io.github.dirnei.mc.servus.alchemy.calciner.CalcinerShellBlock;
import io.github.dirnei.mc.servus.alchemy.coagulator.VatBlock;
import io.github.dirnei.mc.servus.alchemy.hollow.GnomeHollowBlock;
import io.github.dirnei.mc.servus.alchemy.hollow.PrincipleDropperBlock;
import io.github.dirnei.mc.servus.alchemy.purifier.CatchBasinBlock;
import io.github.dirnei.mc.servus.alchemy.purifier.PurifierBlock;
import io.github.dirnei.mc.servus.alchemy.purifier.ResidueCrockBlock;
import io.github.dirnei.mc.servus.alchemy.structure.CopperDuctBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ServusAlchemyBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ServusAlchemyIds.MOD_ID);

    public static final DeferredBlock<CalcinerBlock> CALCINER =
            BLOCKS.registerBlock("calciner", CalcinerBlock::new, () -> MachineContent.machine());

    public static final DeferredBlock<CalcinerPartBlock> FIREBOX = calcinerPart("calciner_firebox", Role.FIREBOX,
            bricks().lightLevel(state -> state.getValue(CalcinerPartBlock.LIT) ? 13 : 0));
    public static final DeferredBlock<CalcinerPartBlock> ASH_PAN = calcinerPart("calciner_ash_pan", Role.ASH_PAN, bricks());
    public static final DeferredBlock<CalcinerPartBlock> ALEMBIC = calcinerPart("alembic", Role.ALEMBIC, copper());
    public static final DeferredBlock<CalcinerPartBlock> RECEIVER = calcinerPart("receiver", Role.RECEIVER,
            BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(1.0F).sound(SoundType.GLASS).noOcclusion());
    public static final DeferredBlock<CalcinerPartBlock> SULFUR_HOOD = calcinerPart("sulfur_hood", Role.HOOD, copper());
    public static final DeferredBlock<CopperDuctBlock> COPPER_DUCT = BLOCKS.registerBlock("copper_duct", CopperDuctBlock::new, () -> copper());

    /** Stone Bricks of a formed Calciner; mined like Stone Bricks and dropping them. */
    public static final DeferredBlock<CalcinerShellBlock> CALCINER_STONE_BRICKS = BLOCKS.registerBlock("calciner_stone_bricks",
            CalcinerShellBlock::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE).noOcclusion());
    /** Deepslate Tiles of a formed Calciner; mined like Deepslate Tiles and dropping them. */
    public static final DeferredBlock<CalcinerShellBlock> CALCINER_DEEPSLATE_TILES = BLOCKS.registerBlock("calciner_deepslate_tiles",
            CalcinerShellBlock::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE_TILES).noOcclusion());

    /** The Purifier, a wooden vat that leaches Ash into liquid Salt. */
    public static final DeferredBlock<PurifierBlock> PURIFIER = BLOCKS.registerBlock("purifier", PurifierBlock::new,
            () -> wood());

    /** The Catch Basin below one plank of the Purifier, where the Salt gathers. */
    public static final DeferredBlock<CatchBasinBlock> CATCH_BASIN = BLOCKS.registerBlock("catch_basin", CatchBasinBlock::new,
            () -> wood());

    /** The Residue Crock below the other plank of the Purifier, where the Caput Mortuum falls. */
    public static final DeferredBlock<ResidueCrockBlock> RESIDUE_CROCK = BLOCKS.registerBlock("residue_crock", ResidueCrockBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).strength(1.25F, 4.2F).sound(SoundType.STONE).noOcclusion());

    /** A Principle Vat of the Coagulator build. */
    public static final DeferredBlock<VatBlock> VAT = BLOCKS.registerBlock("principle_vat", VatBlock::new, () -> copper());

    public static final DeferredBlock<CoagulatorBlock> COAGULATOR =
            BLOCKS.registerBlock("coagulator", CoagulatorBlock::new, () -> MachineContent.machine());

    public static final DeferredBlock<Block> SALT_CRYSTAL_BLOCK = BLOCKS.registerSimpleBlock("salt_crystal_block",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(0.8F).requiresCorrectToolForDrops().sound(SoundType.CALCITE));

    public static final DeferredBlock<AthanorBlock> ATHANOR = BLOCKS.registerBlock("athanor", AthanorBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2.0F, 6.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.STONE).noOcclusion().lightLevel(state -> state.getValue(MachineBlock.LIT) ? 13 : 0));
    /** The invisible space above a formed Athanor; no item, no drop. */
    public static final DeferredBlock<AthanorCrownBlock> ATHANOR_CROWN = BLOCKS.registerBlock("athanor_crown", AthanorCrownBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(2.0F, 6.0F).sound(SoundType.STONE).noOcclusion().noLootTable()
                    .pushReaction(PushReaction.BLOCK));

    public static final DeferredBlock<GnomeHollowBlock> GNOME_HOLLOW = BLOCKS.registerBlock("gnome_hollow", GnomeHollowBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE));

    /** The Principle Dropper, a copper drip vessel that feeds the Gnome Hollow below it. */
    public static final DeferredBlock<PrincipleDropperBlock> PRINCIPLE_DROPPER = BLOCKS.registerBlock("principle_dropper",
            PrincipleDropperBlock::new, () -> copper());

    private static DeferredBlock<CalcinerPartBlock> calcinerPart(String name, Role role, BlockBehaviour.Properties properties) {
        return BLOCKS.registerBlock(name, props -> new CalcinerPartBlock(role, props), () -> properties);
    }

    /** Properties of vanilla Bricks. */
    private static BlockBehaviour.Properties bricks() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2.0F, 6.0F).requiresCorrectToolForDrops()
                .sound(SoundType.STONE).noOcclusion();
    }

    private static BlockBehaviour.Properties wood() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F, 3.0F).sound(SoundType.WOOD).noOcclusion();
    }

    private static BlockBehaviour.Properties copper() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(3.0F, 6.0F).requiresCorrectToolForDrops()
                .sound(SoundType.COPPER).noOcclusion();
    }

    private ServusAlchemyBlocks() {
    }
}
