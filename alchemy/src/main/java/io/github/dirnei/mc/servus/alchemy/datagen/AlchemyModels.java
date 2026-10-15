package io.github.dirnei.mc.servus.alchemy.datagen;

import io.github.dirnei.mc.servus.alchemy.CalcinerBlock;
import io.github.dirnei.mc.servus.alchemy.CoagulatorBlock;
import io.github.dirnei.mc.servus.alchemy.calciner.CalcinerLayout;
import io.github.dirnei.mc.servus.alchemy.calciner.CalcinerPartBlock;
import io.github.dirnei.mc.servus.alchemy.calciner.CalcinerShellBlock;
import io.github.dirnei.mc.servus.alchemy.coagulator.VatBlock;
import io.github.dirnei.mc.servus.alchemy.homunculus.VasBlock;
import io.github.dirnei.mc.servus.alchemy.purifier.PurifierBlock;
import io.github.dirnei.mc.servus.alchemy.purifier.PurifierSideBlock;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlocks;
import io.github.dirnei.mc.servus.alchemy.structure.CopperDuctBlock;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ConfiguredModel;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelFile;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelRotations;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;

/**
 * Models of the Calciner, the Purifier and the Coagulator builds, made of boxes with vanilla textures.
 * Every block has an unformed model of its own and a formed piece; the formed pieces overhang into their
 * neighbours so each build reads as one piece. In model space the front faces north, so the player's
 * right when looking at it is west (-x).
 */
final class AlchemyModels {

    private static final String BRICKS = "block/bricks";
    private static final String COPPER = "block/copper_block";
    private static final String CUT_COPPER = "block/cut_copper";
    private static final String IRON = "block/iron_block";
    private static final String IRON_BARS = "block/iron_bars";
    private static final String STONE = "block/smooth_stone";
    private static final String CAULDRON = "block/cauldron_side";
    private static final String COAL = "block/coal_block";
    private static final String EMBERS = "block/magma";
    private static final String DARK_OAK = "block/dark_oak_planks";
    private static final String STONE_BRICKS = "block/stone_bricks";
    private static final String DEEPSLATE_TILES = "block/deepslate_tiles";
    private static final String POLISHED_DEEPSLATE = "block/polished_deepslate";
    private static final String STEEL = "servus_foundation:block/electric_pump_steel";

    private final BlockStateProvider states;

    AlchemyModels(BlockStateProvider states) {
        this.states = states;
    }

    void register() {
        calciner();
        ModelFile firebox = firebox("calciner_firebox", COAL);
        ModelFile fireboxLit = firebox("calciner_firebox_lit", EMBERS);
        part(ServusAlchemyBlocks.FIREBOX.get(), firebox, fireboxLit, firebox, fireboxLit);
        ModelFile ashPan = ashPan("calciner_ash_pan", false);
        ModelFile ashPanFormed = ashPan("calciner_ash_pan_formed", true);
        part(ServusAlchemyBlocks.ASH_PAN.get(), ashPan, ashPan, ashPanFormed, ashPanFormed);
        ModelFile alembic = alembic("alembic", false);
        ModelFile alembicFormed = alembic("alembic_formed", true);
        part(ServusAlchemyBlocks.ALEMBIC.get(), alembic, alembic, alembicFormed, alembicFormed);
        ModelFile receiver = receiver("receiver", false);
        ModelFile receiverFormed = receiver("receiver_formed", true);
        part(ServusAlchemyBlocks.RECEIVER.get(), receiver, receiver, receiverFormed, receiverFormed);
        ModelFile hood = hood("sulfur_hood");
        part(ServusAlchemyBlocks.SULFUR_HOOD.get(), hood, hood, hood, hood);
        shell();
        duct();
        purifier();
        coagulator();
        vat();
        dropper();
        forge();
        receptaculum();
        vas();
    }

    /**
     * The Receptaculum, a Hearth Chest: a brick hearth with an open grate, embers and three flames, a deepslate slab, an iron
     * banded deepslate brick chest with a tile lid and a brass hasp, and a brass collar the Vas stands on. One model for
     * lit and unlit: the fire never goes out.
     */
    private void receptaculum() {
        BlockModelBuilder model = builder("receptaculum")
                .texture("bricks", states.mcLoc(BRICKS)).texture("slab", states.mcLoc(POLISHED_DEEPSLATE))
                .texture("embers", states.mcLoc(EMBERS)).texture("flame", states.modLoc("block/receptaculum_flame"))
                .texture("steel", STEEL).texture("body", states.mcLoc("block/deepslate_bricks"))
                .texture("lid", states.mcLoc(DEEPSLATE_TILES)).texture("brass", states.modLoc("block/receptaculum_brass"))
                .texture("particle", states.mcLoc("block/deepslate_bricks"));
        box(model, 0, 0, 0, 16, 1, 16, "#bricks");
        box(model, 0, 1, 14, 16, 5, 16, "#bricks");
        box(model, 0, 1, 0, 4, 5, 14, "#bricks");
        box(model, 12, 1, 0, 16, 5, 14, "#bricks");
        box(model, 4, 4.2F, 0, 12, 5, 14, "#bricks");
        box(model, 0, 5, 0, 16, 6, 16, "#slab");
        glowing(model, 4, 1, 2, 12, 1.6F, 14, "#embers");
        glowing(model, 5, 1.6F, 3, 6.5F, 3.6F, 4.5F, "#flame");
        glowing(model, 7.25F, 1.6F, 5, 8.75F, 4, 6.5F, "#flame");
        glowing(model, 9.5F, 1.6F, 3, 11, 3.2F, 4.5F, "#flame");
        for (float x : new float[] {5.75F, 7.75F, 9.75F}) {
            box(model, x, 1, 0.5F, x + 0.5F, 4.2F, 1, "#steel");
        }
        box(model, 0.5F, 6, 0.5F, 15.5F, 14, 15.5F, "#body");
        box(model, 3.1F, 6, 0.3F, 3.9F, 13.9F, 15.7F, "#steel");
        box(model, 12.1F, 6, 0.3F, 12.9F, 13.9F, 15.7F, "#steel");
        box(model, 0.3F, 14, 0.3F, 15.7F, 15.2F, 15.7F, "#lid");
        box(model, 6.8F, 11.5F, 0, 9.2F, 14.6F, 0.5F, "#brass");
        box(model, 2, 15.2F, 2, 14, 16, 14, "#brass");
        Block block = ServusAlchemyBlocks.RECEPTACULUM.get();
        states.getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(model)
                .rotationY(ModelRotations.rotationY(state.getValue(MachineBlock.FACING)))
                .build());
        states.simpleBlockItem(block, model);
    }

    /**
     * The Vas Hermeticum: a glass flask with a stepped bulb and a neck, murky liquid inside and, until it is freed, a red
     * wax seal with a brass stamp. Neighbouring glass parts overlap by a twentieth of a pixel, so no faces share a plane.
     */
    private void vas() {
        ModelFile sealed = vasModel("vas_hermeticum", true);
        ModelFile freed = vasModel("vas_hermeticum_freed", false);
        states.getVariantBuilder(ServusAlchemyBlocks.VAS_HERMETICUM.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(VasBlock.FREED) ? freed : sealed)
                .build());
    }

    private ModelFile vasModel(String name, boolean seal) {
        BlockModelBuilder model = builder(name)
                .texture("glass", states.modLoc("block/vas_glass")).texture("liquid", states.modLoc("block/vas_liquid"))
                .texture("seal", states.modLoc("block/vas_seal")).texture("brass", states.modLoc("block/receptaculum_brass"))
                .texture("particle", states.modLoc("block/vas_glass"));
        clampedBox(model, 4, 0, 4, 12, 1, 12, "#glass");
        clampedBox(model, 2.5F, 0.95F, 2.5F, 13.5F, 2.95F, 13.5F, "#glass");
        clampedBox(model, 1, 2.9F, 1, 15, 8.9F, 15, "#glass");
        clampedBox(model, 2.5F, 8.85F, 2.5F, 13.5F, 10.85F, 13.5F, "#glass");
        clampedBox(model, 6, 10.8F, 6, 10, 16.8F, 10, "#glass");
        clampedBox(model, 2.8F, 1.05F, 2.8F, 13.2F, 3, 13.2F, "#liquid");
        clampedBox(model, 1.3F, 2.95F, 1.3F, 14.7F, 7.9F, 14.7F, "#liquid");
        if (seal) {
            clampedBox(model, 5.4F, 16.4F, 5.4F, 10.6F, 18, 10.6F, "#seal");
            clampedBox(model, 6.8F, 18, 6.8F, 9.2F, 18.3F, 9.2F, "#brass");
        }
        return model;
    }

    /** A box whose faces map the texture by position like {@link #box}, shifted onto the sprite for parts above the block. */
    private static void clampedBox(BlockModelBuilder model, float x1, float y1, float z1, float x2, float y2, float z2, String texture) {
        float top = clamp(16 - y2);
        float bottom = clamp(top + y2 - y1);
        model.element().from(x1, y1, z1).to(x2, y2, z2).allFaces((direction, face) -> {
            switch (direction.getAxis()) {
                case Y -> face.uvs(x1, z1, x2, z2);
                case Z -> face.uvs(x1, top, x2, bottom);
                default -> face.uvs(z1, top, z2, bottom);
            }
            face.texture(texture);
        }).end();
    }

    private static float clamp(float uv) {
        return Math.max(0, Math.min(16, uv));
    }

    /** A box that glows at full light, for embers and flames. */
    private static void glowing(BlockModelBuilder model, float x1, float y1, float z1, float x2, float y2, float z2, String texture) {
        model.element().from(x1, y1, z1).to(x2, y2, z2).lightEmission(15).allFaces((direction, face) -> face.texture(texture)).end();
    }

    /**
     * The Salamander Forge: a deepslate brick block with an open mouth, glowing embers on its floor, a full bright fire
     * standing in it and a steel plate with a salamander above it. One model for lit and unlit: the fire never goes out.
     */
    private void forge() {
        BlockModelBuilder model = builder("salamander_forge")
                .texture("bricks", states.mcLoc("block/deepslate_bricks")).texture("embers", states.mcLoc(EMBERS))
                .texture("fire", states.mcLoc("block/fire_0")).texture("steel", STEEL)
                .texture("emblem", states.modLoc("block/salamander_forge_emblem"))
                .texture("particle", states.mcLoc("block/deepslate_bricks"));
        box(model, 0, 0, 4, 16, 16, 16, "#bricks");
        box(model, 0, 0, 0, 3, 16, 4, "#bricks");
        box(model, 13, 0, 0, 16, 16, 4, "#bricks");
        box(model, 3, 11, 0, 13, 16, 4, "#bricks");
        box(model, 3, 0, 0, 13, 3, 4, "#bricks");
        model.element().from(3, 3, 2).to(13, 5, 4).lightEmission(15).allFaces((direction, face) -> face.texture("#embers")).end();
        model.element().from(3, 5, 3).to(13, 11, 3).lightEmission(15)
                .face(Direction.NORTH).uvs(0, 6, 16, 16).texture("#fire").end()
                .face(Direction.SOUTH).uvs(0, 6, 16, 16).texture("#fire").end()
                .end();
        // No back face: it would lie on the lintel's front.
        model.element().from(5, 12, -0.5F).to(11, 15, 0)
                .allFacesExcept((direction, face) -> {
                    if (direction == Direction.NORTH) {
                        face.uvs(0, 4, 16, 12).texture("#emblem");
                    } else {
                        face.texture("#steel");
                    }
                }, Set.of(Direction.SOUTH))
                .end();
        Block block = ServusAlchemyBlocks.SALAMANDER_FORGE.get();
        states.getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(model)
                .rotationY(ModelRotations.rotationY(state.getValue(MachineBlock.FACING)))
                .build());
        states.simpleBlockItem(block, model);
    }

    /**
     * The Principle Dropper: a glass bulb on a copper plate standing on four legs, a lid with an inlet neck on top and
     * a spout from the plate down to the Hollow below. Boxes only touch where they meet, so no faces share a plane.
     */
    private void dropper() {
        BlockModelBuilder model = builder("principle_dropper")
                .texture("copper", states.mcLoc(COPPER)).texture("cut", states.mcLoc(CUT_COPPER))
                .texture("glass", states.mcLoc("block/glass")).texture("particle", states.mcLoc(COPPER));
        for (float x : new float[] {3, 11.5F}) {
            for (float z : new float[] {3, 11.5F}) {
                box(model, x, 0, z, x + 1.5F, 4, z + 1.5F, "#cut");
            }
        }
        box(model, 7, 1, 7, 9, 4, 9, "#cut");
        box(model, 7.5F, 0.25F, 7.5F, 8.5F, 1, 8.5F, "#cut");
        box(model, 3, 4, 3, 13, 5, 13, "#copper");
        // The whole glass sprite on each face, so its frame lines run along the bulb's edges.
        model.element().from(4, 5, 4).to(12, 13, 12).allFaces((direction, face) -> face.uvs(0, 0, 16, 16).texture("#glass")).end();
        box(model, 3.5F, 13, 3.5F, 12.5F, 14, 12.5F, "#cut");
        box(model, 6, 14, 6, 10, 16, 10, "#copper");
        states.simpleBlockWithItem(ServusAlchemyBlocks.PRINCIPLE_DROPPER.get(), model);
    }

    /** The Coagulator: a cobblestone block with its front while unformed; formed, a stone casting table with an iron mold. */
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
        Block block = ServusAlchemyBlocks.COAGULATOR.get();
        states.getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(CoagulatorBlock.FORMED) ? formed : state.getValue(MachineBlock.LIT) ? lit : unlit)
                .rotationY(ModelRotations.rotationY(state.getValue(MachineBlock.FACING)))
                .build());
        states.simpleBlockItem(block, unlit);
    }

    /** The Principle Vat: an open copper vat; formed, spouts lead from the side Vats into the middle one and from it down into the mold. */
    private void vat() {
        ModelFile plain = vatModel("principle_vat", -1);
        ModelFile[] pieces = {vatModel("principle_vat_left", 0), vatModel("principle_vat_middle", 1), vatModel("principle_vat_right", 2)};
        Block block = ServusAlchemyBlocks.VAT.get();
        states.getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(VatBlock.FORMED) ? pieces[state.getValue(VatBlock.PIECE)] : plain)
                .rotationY(ModelRotations.rotationY(state.getValue(VatBlock.FACING)))
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
        Block vatBlock = ServusAlchemyBlocks.PURIFIER.get();
        states.getVariantBuilder(vatBlock).forAllStates(state -> state.getValue(PurifierBlock.FORMED)
                ? ConfiguredModel.builder().modelFile(vatFormed).rotationY(ModelRotations.rotationY(state.getValue(PurifierBlock.FACING))).build()
                : ConfiguredModel.builder().modelFile(vat).build());
        states.simpleBlockItem(vatBlock, vat);
        side(ServusAlchemyBlocks.CATCH_BASIN.get(), catchBasin("catch_basin", false), catchBasin("catch_basin_formed", true));
        side(ServusAlchemyBlocks.RESIDUE_CROCK.get(), residueCrock("residue_crock", false), residueCrock("residue_crock_formed", true));
    }

    private void side(Block block, ModelFile plain, ModelFile formed) {
        states.getVariantBuilder(block).forAllStates(state -> state.getValue(PurifierSideBlock.FORMED)
                ? ConfiguredModel.builder().modelFile(formed).rotationY(ModelRotations.rotationY(state.getValue(PurifierSideBlock.FACING))).build()
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
            // Ambient occlusion samples the wrong neighbours this far outside the block and turns the spouts black.
            model.ao(false);
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
     * A box with its UVs pinned to the texture's corner. Derived UVs of a box reaching outside the
     * block (below 0 or above 16) leave the sprite and show its neighbours in the atlas.
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
        ModelFile[] pieces = {ductModel("copper_duct_elbow", 0), ductModel("copper_duct_down", 1),
                ductModel("copper_duct_athanor", CopperDuctBlock.ATHANOR_NEAR), ductModel("copper_duct_athanor_over", CopperDuctBlock.ATHANOR_OVER)};
        Block block = ServusAlchemyBlocks.COPPER_DUCT.get();
        states.getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(CopperDuctBlock.FORMED) ? pieces[state.getValue(CopperDuctBlock.PIECE)] : straight)
                .rotationY(ModelRotations.rotationY(state.getValue(CopperDuctBlock.FACING)))
                .build());
        states.simpleBlockItem(block, straight);
    }

    /**
     * One piece of the duct. The Hood lies toward -x (the player's right), the Stone Bricks below
     * the last piece. Piece 0 runs across from the Hood's pipe and turns down, 1 drops into the
     * copper collar of the Stone Bricks. For the Athanor (toward -x) the pipe lies at the bottom:
     * the near piece runs from the tower's socket and ends in a flange (the side of a stacked tank),
     * the over piece ends over the tank and goes down into its top.
     */
    private ModelFile ductModel(String name, int piece) {
        BlockModelBuilder model = builder(name)
                .texture("copper", states.mcLoc(COPPER)).texture("cut", states.mcLoc(CUT_COPPER))
                .texture("particle", states.mcLoc(COPPER));
        switch (piece) {
            case 0 -> {
                box(model, -5, 6, 6, 10, 10, 10, "#copper");
                box(model, 6, 0, 6, 10, 6, 10, "#copper");
                box(model, 5, 5, 5, 11, 11, 11, "#cut");
            }
            case 1 -> {
                // Ambient occlusion samples the wrong neighbours below the block and darkens the pipe.
                model.ao(false);
                uvBox(model, 6, -3, 6, 10, 16, 10, "#copper");
            }
            case CopperDuctBlock.ATHANOR_NEAR -> {
                box(model, 0, 0, 6, 16, 4, 10, "#copper");
                box(model, 14, 0, 5, 16, 5, 11, "#cut");
            }
            case CopperDuctBlock.ATHANOR_OVER -> {
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

    /** The mouth: a stone brick box with its front while unformed, an arch with the crucible when formed. */
    private void calciner() {
        ModelFile unlit = states.models().orientable("calciner", states.mcLoc(STONE_BRICKS), states.modLoc("block/calciner_front"),
                states.mcLoc(STONE));
        ModelFile lit = states.models().orientable("calciner_lit", states.mcLoc(STONE_BRICKS), states.modLoc("block/calciner_front_lit"),
                states.mcLoc(STONE));
        ModelFile formed = mouth("calciner_formed", COAL);
        ModelFile formedLit = mouth("calciner_formed_lit", EMBERS);
        Block block = ServusAlchemyBlocks.CALCINER.get();
        states.getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(pick(state.getValue(CalcinerBlock.FORMED), state.getValue(MachineBlock.LIT), unlit, lit, formed, formedLit))
                .rotationY(ModelRotations.rotationY(state.getValue(MachineBlock.FACING)))
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
                .rotationY(ModelRotations.rotationY(state.getValue(CalcinerPartBlock.FACING)))
                .build());
        states.simpleBlockItem(block, unlit);
    }

    /**
     * The stones of a formed Calciner. The left Stone Bricks take the Copper Duct through a copper
     * collar, the right ones hold the basin under the Receiver; the Deepslate Tiles stay plain.
     */
    private void shell() {
        ModelFile collar = collarStone("calciner_stone_bricks_collar");
        ModelFile basin = basinStone("calciner_stone_bricks_basin");
        states.getVariantBuilder(ServusAlchemyBlocks.CALCINER_STONE_BRICKS.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(CalcinerLayout.cell(state.getValue(CalcinerShellBlock.CELL)).right() < 0 ? collar : basin)
                .rotationY(ModelRotations.rotationY(state.getValue(CalcinerShellBlock.FACING)))
                .build());
        BlockModelBuilder tiles = builder("calciner_deepslate_tiles")
                .texture("stone", states.mcLoc(DEEPSLATE_TILES)).texture("particle", states.mcLoc(DEEPSLATE_TILES));
        box(tiles, 0, 0, 0, 16, 16, 16, "#stone");
        states.getVariantBuilder(ServusAlchemyBlocks.CALCINER_DEEPSLATE_TILES.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(tiles).build());
    }

    private BlockModelBuilder builder(String name) {
        return states.models().withExistingParent(name, states.mcLoc("block/block"));
    }

    /** Stone Bricks with a raised cut copper collar set into the top, where the duct goes in. */
    private ModelFile collarStone(String name) {
        BlockModelBuilder model = builder(name)
                .texture("stone", states.mcLoc(STONE_BRICKS)).texture("cut", states.mcLoc(CUT_COPPER))
                .texture("particle", states.mcLoc(STONE_BRICKS));
        box(model, 0, 0, 0, 16, 15, 16, "#stone");
        ring(model, 5, 11, 15, 16, "#stone");
        uvBox(model, 5.05F, 14, 5.05F, 10.95F, 16.6F, 10.95F, "#cut");
        return model;
    }

    /** Stone Bricks hollowed into a shallow basin with a steel grate over a drain in the middle. */
    private ModelFile basinStone(String name) {
        BlockModelBuilder model = builder(name)
                .texture("stone", states.mcLoc(STONE_BRICKS)).texture("floor", states.mcLoc(STONE))
                .texture("drain", states.mcLoc(POLISHED_DEEPSLATE)).texture("steel", STEEL)
                .texture("particle", states.mcLoc(STONE_BRICKS));
        model.element().from(0, 0, 0).to(16, 10, 16)
                .allFaces((direction, face) -> face.texture(direction == Direction.UP ? "#drain" : "#stone")).end();
        for (float[] part : new float[][] {{0, 0, 16, 6}, {0, 10, 16, 16}, {0, 6, 6, 10}, {10, 6, 16, 10}}) {
            model.element().from(part[0], 10, part[1]).to(part[2], 12, part[3])
                    .allFaces((direction, face) -> face.texture(direction == Direction.UP ? "#floor" : "#stone")).end();
        }
        ring(model, 3, 13, 12, 16, "#stone");
        // The cross bars stop just below the long bars, so no two faces share a plane.
        for (float at : new float[] {6.5F, 8.5F}) {
            box(model, at, 12.2F, 6, at + 1, 12.4F, 10, "#steel");
            box(model, 6, 12, at, 10, 12.195F, at + 1, "#steel");
        }
        return model;
    }

    /** Four boxes from {@code y1} to {@code y2} around a square hole from {@code a} to {@code b}. */
    private static void ring(BlockModelBuilder model, float a, float b, float y1, float y2, String texture) {
        box(model, 0, y1, 0, 16, y2, a, texture);
        box(model, 0, y1, b, 16, y2, 16, texture);
        box(model, 0, y1, a, a, y2, b, texture);
        box(model, b, y1, a, 16, y2, b, texture);
    }

    /** A stone block with an opening in the front: frame, back wall and sides. */
    private BlockModelBuilder frame(String name, String texture, int openingBottom, int openingTop, int depth) {
        BlockModelBuilder model = builder(name).texture("stone", states.mcLoc(texture)).texture("particle", states.mcLoc(texture));
        box(model, 0, 0, depth, 16, 16, 16, "#stone");
        box(model, 0, 0, 0, 3, 16, depth, "#stone");
        box(model, 13, 0, 0, 16, 16, depth, "#stone");
        box(model, 3, openingTop, 0, 13, 16, depth, "#stone");
        if (openingBottom > 0) {
            box(model, 3, 0, 0, 13, openingBottom, depth, "#stone");
        }
        return model;
    }

    /** Coals behind an iron grate. */
    private ModelFile firebox(String name, String coals) {
        BlockModelBuilder model = frame(name, POLISHED_DEEPSLATE, 2, 12, 3)
                .texture("coals", states.mcLoc(coals))
                .texture("bars", states.mcLoc(IRON_BARS));
        // Behind the grate, never in its plane.
        box(model, 3, 2, 1.5F, 13, 5, 3, "#coals");
        plane(model, 3, 2, 13, 12, 1, "#bars");
        return model;
    }

    /** A low drawer with an iron tray, where the Salt gathers. */
    private ModelFile ashPan(String name, boolean formed) {
        BlockModelBuilder model = frame(name, DEEPSLATE_TILES, 2, 8, 3).texture("iron", states.mcLoc(IRON));
        box(model, 3, 2, 1, 13, 3, 3, "#iron");
        box(model, 3, 3, 1, 4, 5, 3, "#iron");
        box(model, 12, 3, 1, 13, 5, 3, "#iron");
        return model;
    }

    /** The formed mouth: an arch over the crucible, embers below it, a copper collar up into the Alembic. */
    private ModelFile mouth(String name, String embers) {
        BlockModelBuilder model = frame(name, STONE_BRICKS, 2, 13, 9)
                .texture("crucible", states.mcLoc(CAULDRON))
                .texture("embers", states.mcLoc(embers))
                .texture("cut", states.mcLoc(CUT_COPPER));
        box(model, 3, 2, 1, 13, 3, 9, "#embers");
        box(model, 5, 3, 2, 11, 9, 8, "#crucible");
        box(model, 6, 15, 6, 10, 20, 10, "#cut");
        return model;
    }

    /** The copper still head that catches the vapour; formed, its beak ends inside the Receiver's drum on the player's right. */
    private ModelFile alembic(String name, boolean formed) {
        BlockModelBuilder model = builder(name)
                .texture("copper", states.mcLoc(COPPER)).texture("cut", states.mcLoc(CUT_COPPER))
                .texture("particle", states.mcLoc(COPPER));
        box(model, 3, 0, 3, 13, 3, 13, "#cut");
        box(model, 4, 3, 4, 12, 10, 12, "#copper");
        box(model, 6, 10, 6, 10, 13, 10, "#copper");
        box(model, 5, 13, 5, 11, 15, 11, "#cut");
        box(model, formed ? -6 : 0, 7.5F, 7, 4, 9.5F, 9, "#cut");
        return model;
    }

    /**
     * A copper condenser: a drum with a lid and a neck, where the vapour cools into Mercury. Unformed, it
     * stands on a copper base; formed, it stands on four copper legs in the basin below, the Alembic's
     * beak enters its side and a tap under it drips into the basin.
     */
    private ModelFile receiver(String name, boolean formed) {
        BlockModelBuilder model = builder(name)
                .texture("copper", states.mcLoc(COPPER)).texture("cut", states.mcLoc(CUT_COPPER))
                .texture("particle", states.mcLoc(COPPER));
        if (!formed) {
            box(model, 4, 0, 4, 12, 1, 12, "#cut");
            drum(model, 1);
            return model;
        }
        // Ambient occlusion samples the wrong neighbours below the block and darkens the legs.
        model.ao(false);
        for (float x : new float[] {4.6F, 10.4F}) {
            for (float z : new float[] {4.6F, 10.4F}) {
                uvBox(model, x, -4, z, x + 1, 4.5F, z + 1, "#cut");
            }
        }
        drum(model, 4);
        box(model, 6.5F, 3, 6.5F, 9.5F, 4, 9.5F, "#cut");
        box(model, 7, 1.5F, 7, 9, 3, 9, "#cut");
        box(model, 6.5F, 1, 6.5F, 9.5F, 2, 9.5F, "#cut");
        return model;
    }

    /** The condenser's drum from {@code bottom} up, with its lid and neck. */
    private static void drum(BlockModelBuilder model, float bottom) {
        box(model, 5, bottom, 5, 11, bottom + 6, 11, "#copper");
        box(model, 4.5F, bottom + 6, 4.5F, 11.5F, bottom + 7, 11.5F, "#cut");
        box(model, 7, bottom + 7, 7, 9, bottom + 8, 9, "#cut");
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
