package io.github.dirnei.mc.servus.foundation.logistics.client;

import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctModelData;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctParts;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

/**
 * The duct's baked model: with a facade in the model data it looks like that
 * block; otherwise it is composed from the part models by the lines and arms
 * the block entity reports. The centre part follows the line set, each arm
 * the lines connected on that side, and an arm's ring faces (the tinted ones)
 * only show when the side is an endpoint.
 */
public final class UtilityDuctModel implements IDynamicBakedModel {

    private final BakedModel base;
    private final Map<String, BakedModel> parts;

    /**
     * @param parts part models by {@link DuctParts} name; a missing name falls back to the base model
     */
    public UtilityDuctModel(BakedModel base, Map<String, BakedModel> parts) {
        this.base = base;
        this.parts = parts;
    }

    private static BakedModel modelOf(BlockState facade) {
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(facade);
    }

    private BakedModel part(String name) {
        return parts.getOrDefault(name, base);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                    ModelData extraData, @Nullable RenderType renderType) {
        BlockState facade = extraData.get(DuctModelData.FACADE);
        if (facade != null) {
            return modelOf(facade).getQuads(facade, side, rand, ModelData.EMPTY, renderType);
        }
        Integer lines = extraData.get(DuctModelData.LINES);
        if (lines == null) {
            return base.getQuads(state, side, rand, extraData, renderType);
        }
        if (side != null) {
            return List.of();
        }
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
        return quads;
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
        BlockState facade = data.get(DuctModelData.FACADE);
        if (facade != null) {
            return modelOf(facade).getRenderTypes(facade, rand, ModelData.EMPTY);
        }
        return base.getRenderTypes(state, rand, data);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        BlockState facade = data.get(DuctModelData.FACADE);
        if (facade != null) {
            return modelOf(facade).getParticleIcon(ModelData.EMPTY);
        }
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
