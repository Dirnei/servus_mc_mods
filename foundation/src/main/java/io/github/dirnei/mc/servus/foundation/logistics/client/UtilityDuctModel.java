package io.github.dirnei.mc.servus.foundation.logistics.client;

import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctModelData;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctParts;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.PanelSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

/**
 * The duct's baked model, composed from the part models by the lines and arms
 * the block entity reports, plus one panel per covered face (see
 * {@link PanelQuads}). The centre part follows the line set, each arm
 * the lines connected on that side, and an arm's ring faces (the tinted ones)
 * only show when the side is an endpoint. A data line tier other than the
 * base one swaps every data containing texture ({@code duct_data},
 * {@code duct_*_data}, {@code duct_*_data_v}) for the tier's variant
 * ({@code <name>_<skin>}), remapping the UVs onto the variant's sprite.
 */
public final class UtilityDuctModel implements IDynamicBakedModel {

    private static final Pattern DATA_TEXTURE = Pattern.compile("block/duct_(?:[a-z]+_)*data(?:_v)?");

    private final BakedModel base;
    private final Map<String, BakedModel> parts;
    /** Reskinned quads by skin, keyed by the original quad (identity). */
    private final Map<String, Map<BakedQuad, BakedQuad>> skinned = new ConcurrentHashMap<>();

    /**
     * @param parts part models by {@link DuctParts} name; a missing name falls back to the base model
     */
    public UtilityDuctModel(BakedModel base, Map<String, BakedModel> parts) {
        this.base = base;
        this.parts = parts;
    }

    private final PanelQuads panelQuads = new PanelQuads();

    private BakedModel part(String name) {
        return parts.getOrDefault(name, base);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                    ModelData extraData, @Nullable RenderType renderType) {
        Integer lines = extraData.get(DuctModelData.LINES);
        if (lines == null) {
            return base.getQuads(state, side, rand, extraData, renderType);
        }
        if (side != null) {
            return List.of();
        }
        List<BakedQuad> quads = renderType == null || base.getRenderTypes(state, rand, extraData).contains(renderType)
                ? ductQuads(state, rand, extraData, renderType, lines) : new ArrayList<>();
        PanelSet panels = extraData.get(DuctModelData.PANELS);
        if (panels != null) {
            boolean seeThrough = PanelReveal.revealing();
            for (Direction face : Direction.values()) {
                BlockState panel = panels.state(face);
                if (panel == null) {
                    continue;
                }
                if (seeThrough) {
                    if (renderType == null || renderType == RenderType.translucent()) {
                        quads.addAll(panelQuads.seeThroughQuads(panel, face, panels.isHollow(face)));
                    }
                } else if (renderType == null || renderType == PanelQuads.renderType(panel, rand)) {
                    quads.addAll(panelQuads.quads(panel, face, panels.isHollow(face)));
                }
            }
        }
        return quads;
    }

    /** The centre and arm parts for the lines and arms in the model data, reskinned for the data tier. */
    private List<BakedQuad> ductQuads(@Nullable BlockState state, RandomSource rand, ModelData extraData,
                                      @Nullable RenderType renderType, int lines) {
        int[] arms = extraData.get(DuctModelData.ARMS);
        Integer endpoints = extraData.get(DuctModelData.ENDPOINTS);
        int linesMask = DuctParts.normalize(lines);
        List<BakedQuad> quads = new ArrayList<>(part(DuctParts.centerName(linesMask, runAxis(arms)))
                .getQuads(state, null, rand, ModelData.EMPTY, renderType));
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
            for (BakedQuad quad : part(DuctParts.armName(direction, armLines)).getQuads(state, null, rand, ModelData.EMPTY, renderType)) {
                if (endpoint || !quad.isTinted()) {
                    quads.add(quad);
                }
            }
        }
        String skin = extraData.get(DuctModelData.DATA_SKIN);
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
        TextureAtlasSprite from = quad.getSprite();
        ResourceLocation name = from.contents().name();
        if (!DATA_TEXTURE.matcher(name.getPath()).matches()) {
            return quad;
        }
        TextureAtlasSprite to = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS)
                .getSprite(name.withSuffix("_" + skin));
        if (to.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
            return quad;
        }
        int[] vertices = quad.getVertices().clone();
        for (int i = 0; i + IQuadTransformer.STRIDE <= vertices.length; i += IQuadTransformer.STRIDE) {
            int u = i + IQuadTransformer.UV0;
            int v = u + 1;
            vertices[u] = Float.floatToRawIntBits(to.getU(from.getUOffset(Float.intBitsToFloat(vertices[u]))));
            vertices[v] = Float.floatToRawIntBits(to.getV(from.getVOffset(Float.intBitsToFloat(vertices[v]))));
        }
        return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), to, quad.isShade(),
                quad.hasAmbientOcclusion());
    }

    /** The axis the centre's bands run along: the one axis every arm lies on, Z without arms, null for a junction. */
    private static Direction.Axis runAxis(int[] arms) {
        int all = 0;
        if (arms != null) {
            for (int mask : arms) {
                all |= mask;
            }
        }
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
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        ChunkRenderTypeSet types = base.getRenderTypes(state, rand, data);
        PanelSet panels = data.get(DuctModelData.PANELS);
        if (panels == null) {
            return types;
        }
        List<RenderType> panelTypes = new ArrayList<>();
        for (Direction face : Direction.values()) {
            BlockState panel = panels.state(face);
            if (panel != null) {
                panelTypes.add(PanelReveal.revealing() ? RenderType.translucent() : PanelQuads.renderType(panel, rand));
            }
        }
        return ChunkRenderTypeSet.union(types, ChunkRenderTypeSet.of(panelTypes));
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        Integer lines = data.get(DuctModelData.LINES);
        if (lines != null && DuctParts.normalize(lines) != DuctParts.ITEM) {
            return part(DuctParts.centerName(lines)).getParticleIcon(ModelData.EMPTY);
        }
        return base.getParticleIcon(data);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return base.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return base.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return base.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return base.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return base.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return base.getOverrides();
    }
}
