package io.github.dirnei.mc.servus.manufacturing.datagen;

import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinerBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CoagulatorBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.coagulator.VatBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.CopperDuctBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerPartBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerShellBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.purifier.PurifierBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.purifier.PurifierSideBlock;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;

/**
 * Models of the Calciner, built from boxes with vanilla textures. Every block has an unformed
 * model of its own and a formed piece; the formed pieces overhang into their neighbours so the
 * whole structure reads as one brick furnace with a copper still. In model space the front faces
 * north, so the player's right when looking at it is west (-x).
 */
final class CalcinerModels {

    private static final String BRICKS = "block/bricks";
    private static final String COPPER = "block/copper_block";
    private static final String CUT_COPPER = "block/cut_copper";
    private static final String GLASS = "block/glass";
    private static final String IRON = "block/iron_block";
    private static final String IRON_BARS = "block/iron_bars";
    private static final String STONE = "block/smooth_stone";
    private static final String CAULDRON = "block/cauldron_side";
    private static final String COAL = "block/coal_block";
    private static final String EMBERS = "block/magma";
    private static final String DARK_OAK = "block/dark_oak_planks";

    /** Where the stone cornice starts and the stone plinth ends, in sixteenths. */
    private static final int CORNICE = 14;
    private static final int PLINTH = 2;

    private final BlockStateProvider states;

    CalcinerModels(BlockStateProvider states) {
        this.states = states;
    }

    void register() {
        calciner();
        ModelFile firebox = firebox("calciner_firebox", COAL);
        ModelFile fireboxLit = firebox("calciner_firebox_lit", EMBERS);
        part(ServusManufacturingBlocks.FIREBOX.get(), firebox, fireboxLit, firebox, fireboxLit);
        ModelFile ashPan = ashPan("calciner_ash_pan", false);
        ModelFile ashPanFormed = ashPan("calciner_ash_pan_formed", true);
        part(ServusManufacturingBlocks.ASH_PAN.get(), ashPan, ashPan, ashPanFormed, ashPanFormed);
        ModelFile alembic = alembic("alembic", false);
        ModelFile alembicFormed = alembic("alembic_formed", true);
        part(ServusManufacturingBlocks.ALEMBIC.get(), alembic, alembic, alembicFormed, alembicFormed);
        ModelFile receiver = receiver("receiver", false);
        ModelFile receiverFormed = receiver("receiver_formed", true);
        part(ServusManufacturingBlocks.RECEIVER.get(), receiver, receiver, receiverFormed, receiverFormed);
        ModelFile hood = hood("sulfur_hood");
        part(ServusManufacturingBlocks.SULFUR_HOOD.get(), hood, hood, hood, hood);
        shell();
        duct();
        purifier();
        coagulator();
        vat();
    }

    /** The Coagulator: the old block while unformed; formed, a stone casting table with an iron mold. */
    private void coagulator() {
        ModelFile unlit = states.models().orientable("coagulator", states.mcLoc("block/cobblestone"), states.modLoc("block/coagulator_front"),
                states.mcLoc(STONE));
        ModelFile lit = states.models().orientable("coagulator_lit", states.mcLoc("block/cobblestone"), states.modLoc("block/coagulator_front_lit"),
                states.mcLoc(STONE));
        BlockModelBuilder formed = builder("coagulator_formed")
                .texture("stone", states.mcLoc(STONE)).texture("front", states.modLoc("block/coagulator_front"))
                .texture("iron", states.mcLoc(IRON)).texture("particle", states.mcLoc(STONE));
        formed.element().from(0, 0, 0).to(16, 12, 16)
                .allFaces((direction, face) -> face.texture(direction == Direction.NORTH ? "#front" : "#stone")).end();
        box(formed, 0, 12, 0, 16, 16, 3, "#stone");
        box(formed, 0, 12, 13, 16, 16, 16, "#stone");
        box(formed, 0, 12, 3, 3, 16, 13, "#stone");
        box(formed, 13, 12, 3, 16, 16, 13, "#stone");
        box(formed, 3, 12, 3, 13, 13, 13, "#iron");
        Block block = ServusManufacturingBlocks.COAGULATOR.get();
        states.getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(CoagulatorBlock.FORMED) ? formed : state.getValue(MachineBlock.LIT) ? lit : unlit)
                .rotationY(rotation(state.getValue(MachineBlock.FACING)))
                .build());
        states.simpleBlockItem(block, unlit);
    }

    /** The Principle Vat: an open copper vat; formed, spouts lead from the side Vats into the middle one and from it down into the mold. */
    private void vat() {
        ModelFile plain = vatModel("principle_vat", -1);
        ModelFile[] pieces = {vatModel("principle_vat_left", 0), vatModel("principle_vat_middle", 1), vatModel("principle_vat_right", 2)};
        Block block = ServusManufacturingBlocks.VAT.get();
        states.getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(VatBlock.FORMED) ? pieces[state.getValue(VatBlock.PIECE)] : plain)
                .rotationY(rotation(state.getValue(VatBlock.FACING)))
                .build());
        states.simpleBlockItem(block, plain);
    }

    /** Piece 0 is the left Vat (toward +x the player's left), 2 the right one, 1 the middle above the mold. */
    private ModelFile vatModel(String name, int piece) {
        BlockModelBuilder model = builder(name)
                .texture("copper", states.mcLoc(COPPER)).texture("cut", states.mcLoc(CUT_COPPER))
                .texture("particle", states.mcLoc(COPPER));
        box(model, 2, 0, 2, 14, 1, 14, "#cut");
        box(model, 2, 1, 2, 14, 12, 3, "#copper");
        box(model, 2, 1, 13, 14, 12, 14, "#copper");
        box(model, 2, 1, 3, 3, 12, 13, "#copper");
        box(model, 13, 1, 3, 14, 12, 13, "#copper");
        switch (piece) {
            case 0 -> box(model, -4, 1.5F, 7, 2, 3.5F, 9, "#cut");
            case 2 -> box(model, 14, 1.5F, 7, 20, 3.5F, 9, "#cut");
            case 1 -> box(model, 7, -4, 7, 9, 0, 9, "#cut");
            default -> {
            }
        }
        return model;
    }

    /**
     * The Purifier: an oak vat with two iron bands, a Catch Basin and a Residue Crock. Formed, the
     * vat grows a dark oak spout out of each side down into the block beside the log, and the Basin
     * and the Crock get a dark oak rim and a bracket against the log they face.
     */
    private void purifier() {
        ModelFile vat = purifierVat("purifier", false);
        ModelFile vatFormed = purifierVat("purifier_formed", true);
        Block vatBlock = ServusManufacturingBlocks.PURIFIER.get();
        states.getVariantBuilder(vatBlock).forAllStates(state -> state.getValue(PurifierBlock.FORMED)
                ? ConfiguredModel.builder().modelFile(vatFormed).rotationY(rotation(state.getValue(PurifierBlock.FACING))).build()
                : ConfiguredModel.builder().modelFile(vat).build());
        states.simpleBlockItem(vatBlock, vat);
        side(ServusManufacturingBlocks.CATCH_BASIN.get(), catchBasin("catch_basin", false), catchBasin("catch_basin_formed", true));
        side(ServusManufacturingBlocks.RESIDUE_CROCK.get(), residueCrock("residue_crock", false), residueCrock("residue_crock_formed", true));
    }

    private void side(Block block, ModelFile plain, ModelFile formed) {
        states.getVariantBuilder(block).forAllStates(state -> state.getValue(PurifierSideBlock.FORMED)
                ? ConfiguredModel.builder().modelFile(formed).rotationY(rotation(state.getValue(PurifierSideBlock.FACING))).build()
                : ConfiguredModel.builder().modelFile(plain).build());
        states.simpleBlockItem(block, plain);
    }

    private ModelFile purifierVat(String name, boolean formed) {
        BlockModelBuilder model = builder(name)
                .texture("planks", states.mcLoc("block/oak_planks")).texture("iron", states.mcLoc(IRON))
                .texture("spout", states.mcLoc(DARK_OAK)).texture("particle", states.mcLoc("block/oak_planks"));
        box(model, 1, 0, 1, 15, 2, 15, "#planks");
        box(model, 1, 0, 1, 15, 14, 3, "#planks");
        box(model, 1, 0, 13, 15, 14, 15, "#planks");
        box(model, 1, 0, 3, 3, 14, 13, "#planks");
        box(model, 13, 0, 3, 15, 14, 13, "#planks");
        for (int y : new int[] {3, 10}) {
            box(model, 0.75F, y, 0.75F, 15.25F, y + 1, 1, "#iron");
            box(model, 0.75F, y, 15, 15.25F, y + 1, 15.25F, "#iron");
            box(model, 0.75F, y, 1, 1, y + 1, 15, "#iron");
            box(model, 15, y, 1, 15.25F, y + 1, 15, "#iron");
        }
        if (formed) {
            // The spouts reach far outside the block, where ambient occlusion samples the wrong neighbours and turns them black.
            model.ao(false);
            // One spout out of each side wall, through the open cell beside the vat, down into the Basin or the Crock.
            for (boolean west : new boolean[] {true, false}) {
                spoutBox(model, west, -9, 9, 7, 1, 11, 9, "#spout");
                spoutBox(model, west, -9, -5, 7, -7, 9, 9, "#spout");
                spoutBox(model, west, -9.25F, 2, 6.75F, -6.75F, 3, 9.25F, "#iron");
            }
        }
        return model;
    }

    /** A box on the west side of the vat, or its mirror on the east side. */
    private static void spoutBox(BlockModelBuilder model, boolean west, float x1, float y1, float z1, float x2, float y2, float z2, String texture) {
        if (west) {
            uvBox(model, x1, y1, z1, x2, y2, z2, texture);
        } else {
            uvBox(model, 16 - x2, y1, z1, 16 - x1, y2, z2, texture);
        }
    }

    /**
     * A box with its UVs pinned to the texture's corner. Without explicit UVs Minecraft derives
     * them from the position, and outside the block (below 0 or above 16) they leave the sprite and
     * show its neighbours in the atlas.
     */
    static void uvBox(BlockModelBuilder model, float x1, float y1, float z1, float x2, float y2, float z2, String texture) {
        float dx = Math.min(16, x2 - x1);
        float dy = Math.min(16, y2 - y1);
        float dz = Math.min(16, z2 - z1);
        model.element().from(x1, y1, z1).to(x2, y2, z2).allFaces((direction, face) -> {
            switch (direction.getAxis()) {
                case X -> face.uvs(0, 0, dz, dy);
                case Y -> face.uvs(0, 0, dx, dz);
                case Z -> face.uvs(0, 0, dx, dy);
            }
            face.texture(texture);
        }).end();
    }

    /** The Catch Basin: a low oak trough on the stand, where the Salt gathers. */
    private ModelFile catchBasin(String name, boolean formed) {
        BlockModelBuilder basin = builder(name)
                .texture("planks", states.mcLoc("block/oak_planks")).texture("iron", states.mcLoc(IRON))
                .texture("spout", states.mcLoc(DARK_OAK)).texture("particle", states.mcLoc("block/oak_planks"));
        box(basin, 1, 0, 1, 15, 2, 15, "#planks");
        box(basin, 1, 0, 1, 15, 10, 3, "#planks");
        box(basin, 1, 0, 13, 15, 10, 15, "#planks");
        box(basin, 1, 0, 3, 3, 10, 13, "#planks");
        box(basin, 13, 0, 3, 15, 10, 13, "#planks");
        box(basin, 0.75F, 6, 0.75F, 15.25F, 7, 1, "#iron");
        box(basin, 0.75F, 6, 15, 15.25F, 7, 15.25F, "#iron");
        box(basin, 0.75F, 6, 1, 1, 7, 15, "#iron");
        box(basin, 15, 6, 1, 15.25F, 7, 15, "#iron");
        if (formed) {
            sideFrame(basin);
        }
        return basin;
    }

    /** The Residue Crock: a squat terracotta pot with a brick rim, where the Caput Mortuum falls. */
    private ModelFile residueCrock(String name, boolean formed) {
        BlockModelBuilder crock = builder(name)
                .texture("clay", states.mcLoc("block/terracotta")).texture("bricks", states.mcLoc(BRICKS))
                .texture("spout", states.mcLoc(DARK_OAK)).texture("particle", states.mcLoc("block/terracotta"));
        box(crock, 3, 0, 3, 13, 2, 13, "#clay");
        box(crock, 2, 1, 2, 14, 10, 4, "#clay");
        box(crock, 2, 1, 12, 14, 10, 14, "#clay");
        box(crock, 2, 1, 4, 4, 10, 12, "#clay");
        box(crock, 12, 1, 4, 14, 10, 12, "#clay");
        box(crock, 1.5F, 10, 1.5F, 14.5F, 11.5F, 4, "#bricks");
        box(crock, 1.5F, 10, 12, 14.5F, 11.5F, 14.5F, "#bricks");
        box(crock, 1.5F, 10, 4, 4, 11.5F, 12, "#bricks");
        box(crock, 12, 10, 4, 14.5F, 11.5F, 12, "#bricks");
        if (formed) {
            sideFrame(crock);
        }
        return crock;
    }

    /** A dark oak stand under a formed Basin or Crock with a bracket to the log it faces (north in model space). */
    private static void sideFrame(BlockModelBuilder model) {
        box(model, 0.5F, 0, 0.5F, 2.5F, 4, 2.5F, "#spout");
        box(model, 13.5F, 0, 0.5F, 15.5F, 4, 2.5F, "#spout");
        box(model, 0.5F, 0, 13.5F, 2.5F, 4, 15.5F, "#spout");
        box(model, 13.5F, 0, 13.5F, 15.5F, 4, 15.5F, "#spout");
        box(model, 5, 3, 0, 11, 5, 1, "#spout");
    }

    /** The Copper Duct: a straight copper pipe while unformed, its piece of the run from Hood to tank when formed. */
    private void duct() {
        ModelFile straight = ductModel("copper_duct", -1);
        ModelFile[] pieces = {ductModel("copper_duct_across", 0), ductModel("copper_duct_elbow", 1), ductModel("copper_duct_down", 2),
                ductModel("copper_duct_athanor", 3), ductModel("copper_duct_athanor_over", 4)};
        Block block = ServusManufacturingBlocks.COPPER_DUCT.get();
        states.getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(CopperDuctBlock.FORMED) ? pieces[state.getValue(CopperDuctBlock.PIECE)] : straight)
                .rotationY(rotation(state.getValue(CopperDuctBlock.FACING)))
                .build());
        states.simpleBlockItem(block, straight);
    }

    /**
     * One piece of the duct. The Hood lies toward -x (the player's right), the Sulfur tank below
     * the last piece. Piece 0 runs across and into the Hood's pipe, 1 turns down, 2 drops into the tank.
     * For the Athanor (toward -x) the pipe lies at the bottom: piece 3 runs from the tower's socket and
     * ends in a flange (the side of a stacked tank), 4 ends over the tank and goes down into its top.
     */
    private ModelFile ductModel(String name, int piece) {
        BlockModelBuilder model = builder(name)
                .texture("copper", states.mcLoc(COPPER)).texture("cut", states.mcLoc(CUT_COPPER))
                .texture("particle", states.mcLoc(COPPER));
        switch (piece) {
            case 0 -> box(model, -5, 6, 6, 16, 10, 10, "#copper");
            case 1 -> {
                box(model, 0, 6, 6, 10, 10, 10, "#copper");
                box(model, 6, 0, 6, 10, 6, 10, "#copper");
                box(model, 5, 5, 5, 11, 11, 11, "#cut");
            }
            case 2 -> {
                box(model, 6, 2, 6, 10, 16, 10, "#copper");
                box(model, 4, 0, 4, 12, 2, 12, "#cut");
            }
            case 3 -> {
                box(model, 0, 0, 6, 16, 4, 10, "#copper");
                box(model, 14, 0, 5, 16, 5, 11, "#cut");
            }
            case 4 -> {
                box(model, 0, 0, 6, 10, 4, 10, "#copper");
                box(model, 5, 0, 5, 11, 5, 11, "#cut");
                box(model, 4, 0, 4, 12, 1, 12, "#cut");
            }
            default -> {
                box(model, 0, 6, 6, 16, 10, 10, "#copper");
                box(model, 0, 5, 5, 2, 11, 11, "#cut");
                box(model, 14, 5, 5, 16, 11, 11, "#cut");
            }
        }
        return model;
    }

    private static int rotation(Direction facing) {
        return ((int) facing.toYRot() + 180) % 360;
    }

    /** The mouth: a stone brick box with the old front while unformed, an arch with the crucible when formed. */
    private void calciner() {
        ModelFile unlit = states.models().orientable("calciner", states.mcLoc("block/stone_bricks"), states.modLoc("block/calciner_front"),
                states.mcLoc(STONE));
        ModelFile lit = states.models().orientable("calciner_lit", states.mcLoc("block/stone_bricks"), states.modLoc("block/calciner_front_lit"),
                states.mcLoc(STONE));
        ModelFile formed = mouth("calciner_formed", COAL);
        ModelFile formedLit = mouth("calciner_formed_lit", EMBERS);
        Block block = ServusManufacturingBlocks.CALCINER.get();
        states.getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(pick(state.getValue(CalcinerBlock.FORMED), state.getValue(MachineBlock.LIT), unlit, lit, formed, formedLit))
                .rotationY(rotation(state.getValue(MachineBlock.FACING)))
                .build());
        states.simpleBlockItem(block, unlit);
    }

    private static ModelFile pick(boolean formed, boolean lit, ModelFile unlit, ModelFile litModel, ModelFile formedModel, ModelFile formedLit) {
        if (formed) {
            return lit ? formedLit : formedModel;
        }
        return lit ? litModel : unlit;
    }

    /** A part turned by its facing, with lit and formed models. */
    private void part(Block block, ModelFile unlit, ModelFile lit, ModelFile formed, ModelFile formedLit) {
        states.getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(pick(state.getValue(CalcinerPartBlock.FORMED), state.getValue(CalcinerPartBlock.LIT), unlit, lit, formed, formedLit))
                .rotationY(rotation(state.getValue(CalcinerPartBlock.FACING)))
                .build());
        states.simpleBlockItem(block, unlit);
    }

    /** The bricks of a formed Calciner: a stone cornice on the top row, a stone plinth on the bottom row. */
    private void shell() {
        ModelFile top = bricks("calciner_shell_top", true, false);
        ModelFile middle = bricks("calciner_shell_middle", false, false);
        ModelFile bottom = bricks("calciner_shell_bottom", false, true);
        states.getVariantBuilder(ServusManufacturingBlocks.CALCINER_SHELL.get()).forAllStates(state -> {
            int up = CalcinerLayout.cell(state.getValue(CalcinerShellBlock.CELL)).up();
            return ConfiguredModel.builder()
                    .modelFile(up == 0 ? top : up == -1 ? middle : bottom)
                    .rotationY(rotation(state.getValue(CalcinerShellBlock.FACING)))
                    .build();
        });
    }

    private BlockModelBuilder builder(String name) {
        return states.models().withExistingParent(name, states.mcLoc("block/block"));
    }

    private BlockModelBuilder bricks(String name, boolean cornice, boolean plinth) {
        BlockModelBuilder model = builder(name).texture("bricks", states.mcLoc(BRICKS)).texture("particle", states.mcLoc(BRICKS));
        // The body stops where a stone band starts, so no two faces lie in one plane and flicker.
        box(model, 0, plinth ? PLINTH : 0, 0, 16, cornice ? CORNICE : 16, 16, "#bricks");
        trim(model, cornice, plinth);
        return model;
    }

    /** The stone bands that run along the whole formed furnace, front and back. */
    private void trim(BlockModelBuilder model, boolean cornice, boolean plinth) {
        if (!cornice && !plinth) {
            return;
        }
        model.texture("stone", states.mcLoc(STONE));
        if (cornice) {
            box(model, 0, CORNICE, -1, 16, 16, 17, "#stone");
        }
        if (plinth) {
            box(model, 0, 0, -1, 16, PLINTH, 17, "#stone");
        }
    }

    /** A brick block with an opening in the front: frame, back wall and sides. */
    private BlockModelBuilder brickFrame(String name, int openingBottom, int openingTop) {
        return brickFrame(name, openingBottom, openingTop, 0, 16, 3);
    }

    /** A brick frame between {@code bottom} and {@code top}, leaving room for a stone band above or below. */
    private BlockModelBuilder brickFrame(String name, int openingBottom, int openingTop, int bottom, int top, int depth) {
        BlockModelBuilder model = builder(name).texture("bricks", states.mcLoc(BRICKS)).texture("particle", states.mcLoc(BRICKS));
        box(model, 0, bottom, depth, 16, top, 16, "#bricks");
        box(model, 0, bottom, 0, 3, top, depth, "#bricks");
        box(model, 13, bottom, 0, 16, top, depth, "#bricks");
        box(model, 3, openingTop, 0, 13, top, depth, "#bricks");
        if (openingBottom > bottom) {
            box(model, 3, bottom, 0, 13, openingBottom, depth, "#bricks");
        }
        return model;
    }

    /** Coals behind an iron grate. */
    private ModelFile firebox(String name, String coals) {
        BlockModelBuilder model = brickFrame(name, 2, 12)
                .texture("coals", states.mcLoc(coals))
                .texture("bars", states.mcLoc(IRON_BARS))
                .renderType("cutout");
        // Behind the grate, never in its plane.
        box(model, 3, 2, 1.5F, 13, 5, 3, "#coals");
        plane(model, 3, 2, 13, 12, 1, "#bars");
        return model;
    }

    /** A low drawer with an iron tray, where the Salt gathers. */
    private ModelFile ashPan(String name, boolean formed) {
        BlockModelBuilder model = brickFrame(name, 2, 8, formed ? PLINTH : 0, 16, 3).texture("iron", states.mcLoc(IRON));
        box(model, 3, 2, 1, 13, 3, 3, "#iron");
        box(model, 3, 3, 1, 4, 5, 3, "#iron");
        box(model, 12, 3, 1, 13, 5, 3, "#iron");
        trim(model, false, formed);
        return model;
    }

    /** The formed mouth: an arch over the crucible, embers below it, a copper collar up into the Alembic. */
    private ModelFile mouth(String name, String embers) {
        BlockModelBuilder model = brickFrame(name, 2, 13, 0, CORNICE, 9)
                .texture("crucible", states.mcLoc(CAULDRON))
                .texture("embers", states.mcLoc(embers))
                .texture("cut", states.mcLoc(CUT_COPPER));
        box(model, 3, 2, 1, 13, 3, 9, "#embers");
        box(model, 5, 3, 2, 11, 9, 8, "#crucible");
        box(model, 6, 15, 6, 10, 20, 10, "#cut");
        trim(model, true, false);
        return model;
    }

    /** The copper still head that catches the vapour; formed, its beak reaches the Receiver on the player's right. */
    private ModelFile alembic(String name, boolean formed) {
        BlockModelBuilder model = builder(name)
                .texture("copper", states.mcLoc(COPPER)).texture("cut", states.mcLoc(CUT_COPPER))
                .texture("particle", states.mcLoc(COPPER));
        box(model, 3, 0, 3, 13, 3, 13, "#cut");
        box(model, 4, 3, 4, 12, 10, 12, "#copper");
        box(model, 6, 10, 6, 10, 13, 10, "#copper");
        box(model, 5, 13, 5, 11, 15, 11, "#cut");
        // Formed, the spout crosses the air cell to the Receiver beyond it.
        box(model, formed ? -16 : 0, 8, 7, 4, 10, 9, "#cut");
        return model;
    }

    /** A glass flask on a copper stand, where the Mercury drips in. */
    private ModelFile receiver(String name, boolean formed) {
        BlockModelBuilder model = builder(name)
                .texture("glass", states.mcLoc(GLASS)).texture("cut", states.mcLoc(CUT_COPPER))
                .texture("particle", states.mcLoc(GLASS))
                .renderType("cutout");
        box(model, 3, 0, 3, 13, 1, 13, "#cut");
        box(model, 4, 1, 4, 12, 9, 12, "#glass");
        box(model, 6, 9, 6, 10, 14, 10, "#glass");
        if (formed) {
            // The end of the Alembic's spout, dipping into the flask's neck.
            box(model, 10, 8, 7, 16, 10, 9, "#cut");
        }
        return model;
    }

    /** The copper hood where the Sulfur rises and settles. */
    private ModelFile hood(String name) {
        BlockModelBuilder model = builder(name)
                .texture("copper", states.mcLoc(COPPER)).texture("cut", states.mcLoc(CUT_COPPER))
                .texture("particle", states.mcLoc(COPPER));
        box(model, 2, 0, 2, 14, 3, 14, "#cut");
        box(model, 4, 3, 4, 12, 6, 12, "#copper");
        box(model, 5, 6, 5, 11, 14, 11, "#copper");
        box(model, 4, 14, 4, 12, 16, 12, "#cut");
        return model;
    }

    static void box(BlockModelBuilder model, float x1, float y1, float z1, float x2, float y2, float z2, String texture) {
        model.element().from(x1, y1, z1).to(x2, y2, z2).allFaces((direction, face) -> face.texture(texture)).end();
    }

    /** A flat plane facing north and south, for bars and grates. */
    static void plane(BlockModelBuilder model, float x1, float y1, float x2, float y2, float z, String texture) {
        model.element().from(x1, y1, z).to(x2, y2, z)
                .face(Direction.NORTH).texture(texture).end()
                .face(Direction.SOUTH).texture(texture).end()
                .end();
    }
}
