package io.github.dirnei.mc.servus.foundation.logistics.client;

import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctModelData;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctParts;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctShapes;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.PanelSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.quad.MutableQuad;
import net.neoforged.neoforge.model.data.ModelData;

/**
 * The duct's block state model, composed from the part models by the lines and
 * arms the block entity reports, plus one panel per covered face (see
 * {@link PanelQuads}). The centre part follows the line set, each arm the
 * lines connected on that side, and an arm's ring faces (the tinted ones) only
 * show when the side is an endpoint. Every part is moved to the duct's
 * resting pose (see {@link DuctPoseQuads}). A data line tier other than the base one
 * swaps every data containing texture ({@code duct_data}, {@code duct_*_data},
 * {@code duct_*_data_v}) for the tier's variant ({@code <name>_<skin>}),
 * remapping the UVs onto the variant's sprite.
 */
public final class UtilityDuctModel implements DynamicBlockStateModel {

    private static final Pattern DATA_TEXTURE = Pattern.compile("block/duct_(?:[a-z]+_)*data(?:_v)?");

    private final BlockStateModel base;
    private final Map<String, BlockStateModelPart> parts;
    /** Reskinned quads by skin, keyed by the original quad (identity). */
    private final Map<String, Map<BakedQuad, BakedQuad>> skinned = new ConcurrentHashMap<>();
    private final PanelQuads panelQuads = new PanelQuads();
    private final DuctPoseQuads poseQuads = new DuctPoseQuads(this::partQuads);

    /**
     * @param parts part models by {@link DuctParts} name; a missing name falls back to the base model
     */
    public UtilityDuctModel(BlockStateModel base, Map<String, BlockStateModelPart> parts) {
        this.base = base;
        this.parts = parts;
    }

    /** The unculled quads of a part, or of the base model when the part is missing. */
    private List<BakedQuad> partQuads(String name) {
        BlockStateModelPart part = parts.get(name);
        if (part != null) {
            return part.getQuads(null);
        }
        List<BlockStateModelPart> baseParts = new ArrayList<>();
        base.collectParts(RandomSource.create(42), baseParts);
        List<BakedQuad> quads = new ArrayList<>();
        baseParts.forEach(basePart -> quads.addAll(basePart.getQuads(null)));
        return quads;
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
                             List<BlockStateModelPart> output) {
        ModelData data = level.getModelData(pos);
        Integer lines = data.get(DuctModelData.LINES);
        // Until the block entity's lines reach the client, any base model would show the wrong line.
        if (lines == null) {
            return;
        }
        List<BakedQuad> quads = ductQuads(data, lines);
        PanelSet panels = data.get(DuctModelData.PANELS);
        if (panels != null) {
            boolean seeThrough = PanelReveal.revealing();
            for (Direction face : Direction.values()) {
                BlockState panel = panels.state(face);
                if (panel != null) {
                    quads.addAll(seeThrough ? panelQuads.seeThroughQuads(panel, face, panels.isHollow(face))
                            : panelQuads.quads(panel, face, panels.isHollow(face)));
                }
            }
        }
        QuadCollection.Builder builder = new QuadCollection.Builder();
        quads.forEach(builder::addUnculledFace);
        output.add(new SimpleModelWrapper(builder.build(), true, particleMaterial(level, pos, state)));
    }

    /** The centre and arm parts for the lines, arms and pose in the model data, reskinned for the data tier. */
    private List<BakedQuad> ductQuads(ModelData data, int lines) {
        int[] arms = data.get(DuctModelData.ARMS);
        Integer endpoints = data.get(DuctModelData.ENDPOINTS);
        int pose = valueOrZero(data.get(DuctModelData.POSE));
        int faces = valueOrZero(data.get(DuctModelData.FACES));
        int linesMask = DuctParts.normalize(lines);
        int centerRadius = DuctShapes.centerRadius(Integer.bitCount(linesMask), union(arms));
        List<BakedQuad> quads = new ArrayList<>(poseQuads.center(DuctParts.centerName(linesMask, runAxis(arms)), centerRadius, pose));
        for (Direction direction : Direction.values()) {
            int armLines = 0;
            for (Line line : Line.VALUES) {
                if (arms != null && line.ordinal() < arms.length && (arms[line.ordinal()] & DuctModelData.bit(direction)) != 0) {
                    armLines |= 1 << line.ordinal();
                }
            }
            if (armLines == 0) {
                continue;
            }
            boolean endpoint = endpoints != null && (endpoints & DuctModelData.bit(direction)) != 0;
            int armRadius = DuctShapes.radius(Integer.bitCount(armLines));
            String armPart = DuctParts.armName(direction, armLines);
            String stepPart = Integer.bitCount(armLines) >= 2 ? DuctParts.centerName(armLines, null) : armPart;
            DuctPoseQuads.ArmQuads arm = poseQuads.arm(armPart, stepPart, direction, armRadius, centerRadius, pose, faces);
            quads.addAll(arm.body());
            if (endpoint) {
                quads.addAll(arm.ring());
            }
        }
        String skin = data.get(DuctModelData.DATA_SKIN);
        if (skin != null && !skin.isEmpty() && (linesMask & (1 << Line.DATA.ordinal())) != 0) {
            Map<BakedQuad, BakedQuad> cache = skinned.computeIfAbsent(skin, key -> new ConcurrentHashMap<>());
            quads.replaceAll(quad -> cache.computeIfAbsent(quad, original -> reskin(original, skin)));
        }
        return quads;
    }

    /**
     * The quad on the tier variant of its texture when that texture contains
     * the data line, the quad itself otherwise or when the variant is missing.
     */
    private static BakedQuad reskin(BakedQuad quad, String skin) {
        TextureAtlasSprite from = quad.materialInfo().sprite();
        Identifier name = from.contents().name();
        if (!DATA_TEXTURE.matcher(name.getPath()).matches()) {
            return quad;
        }
        TextureAtlasSprite to = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS)
                .getSprite(name.withSuffix("_" + skin));
        if (to.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
            return quad;
        }
        BakedQuad.MaterialInfo info = quad.materialInfo();
        return new MutableQuad().setFrom(quad)
                .setSpriteAndMoveUv(to, info.layer(), info.itemRenderType())
                .toBakedQuad();
    }

    private static int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    /** The six bit mask of sides with an arm of any line. */
    private static int union(int[] arms) {
        int all = 0;
        if (arms != null) {
            for (int mask : arms) {
                all |= mask;
            }
        }
        return all;
    }

    /** The axis the centre's bands run along: the one axis every arm lies on, Z without arms, null for a junction. */
    private static Direction.Axis runAxis(int[] arms) {
        int all = union(arms);
        Direction.Axis found = null;
        for (Direction direction : Direction.values()) {
            if ((all & DuctModelData.bit(direction)) == 0) {
                continue;
            }
            if (found == null) {
                found = direction.getAxis();
            } else if (found != direction.getAxis()) {
                return null;
            }
        }
        return found == null ? Direction.Axis.Z : found;
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        Integer lines = level.getModelData(pos).get(DuctModelData.LINES);
        if (lines != null && DuctParts.normalize(lines) != DuctParts.ITEM) {
            BlockStateModelPart part = parts.get(DuctParts.centerName(lines));
            if (part != null) {
                return part.particleMaterial();
            }
        }
        return base.particleMaterial();
    }

    @Override
    public Material.Baked particleMaterial() {
        return base.particleMaterial();
    }

    @Override
    @BakedQuad.MaterialFlags
    public int materialFlags() {
        return base.materialFlags();
    }

    @Override
    @BakedQuad.MaterialFlags
    public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return materialFlags();
    }
}
