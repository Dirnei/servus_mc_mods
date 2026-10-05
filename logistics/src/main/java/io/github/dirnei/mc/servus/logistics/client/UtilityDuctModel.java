package io.github.dirnei.mc.servus.logistics.client;

import io.github.dirnei.mc.servus.logistics.duct.DuctModelData;
import java.util.List;
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

/** Wraps the duct's baked model: with a facade in the model data it looks like that block. */
public final class UtilityDuctModel implements IDynamicBakedModel {

    private final BakedModel base;

    public UtilityDuctModel(BakedModel base) {
        this.base = base;
    }

    private static BakedModel modelOf(BlockState facade) {
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(facade);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                    ModelData extraData, @Nullable RenderType renderType) {
        BlockState facade = extraData.get(DuctModelData.FACADE);
        if (facade != null) {
            return modelOf(facade).getQuads(facade, side, rand, ModelData.EMPTY, renderType);
        }
        return base.getQuads(state, side, rand, extraData, renderType);
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
        return facade != null ? modelOf(facade).getParticleIcon(ModelData.EMPTY) : base.getParticleIcon(data);
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
