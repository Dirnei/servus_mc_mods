package io.github.dirnei.mc.servus.foundation.wood.client;

import io.github.dirnei.mc.servus.foundation.wood.Wood;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.model.data.ModelData;

/**
 * The block state model of a wood block: board quads ({@link Wood#BOARD}) are
 * retextured onto the planks sprite of the block's wood, every other quad
 * passes through, unless the block's swaps name another texture for it. The
 * block reads the wood from its model data; items use {@link WoodItemModel}.
 * A resource reload builds new wrappers, which drops every cache.
 */
public final class WoodModel implements DynamicBlockStateModel {

    private record PartKey(BlockStateModelPart part, Identifier wood, Map<Identifier, Identifier> swaps) {
    }

    private final BlockStateModel base;
    private final WoodSkin skin;
    private final Map<PartKey, BlockStateModelPart> parts = new ConcurrentHashMap<>();

    public WoodModel(BlockStateModel base) {
        this(base, WoodSkin.WOOD);
    }

    public WoodModel(BlockStateModel base, WoodSkin skin) {
        this.base = base;
        this.skin = skin;
    }

    private Identifier woodOf(ModelData data) {
        Identifier wood = data.get(skin.property());
        return wood != null ? wood : skin.fallback();
    }

    private Map<Identifier, Identifier> swapsOf(ModelData data) {
        Map<Identifier, Identifier> swaps = skin.swaps() ? data.get(Wood.SWAPS) : null;
        return swaps != null ? swaps : Map.of();
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
                             List<BlockStateModelPart> output) {
        ModelData data = level.getModelData(pos);
        Identifier wood = woodOf(data);
        Map<Identifier, Identifier> swaps = swapsOf(data);
        List<BlockStateModelPart> baseParts = new ArrayList<>();
        base.collectParts(level, pos, state, random, baseParts);
        for (BlockStateModelPart part : baseParts) {
            output.add(parts.computeIfAbsent(new PartKey(part, wood, swaps), key -> retexture(part, wood, swaps)));
        }
    }

    private BlockStateModelPart retexture(BlockStateModelPart part, Identifier wood, Map<Identifier, Identifier> swaps) {
        TextureAtlasSprite planks = WoodTextures.spriteOf(wood);
        QuadCollection.Builder builder = new QuadCollection.Builder();
        for (BakedQuad quad : part.getQuads(null)) {
            builder.addUnculledFace(WoodTextures.apply(quad, skin.board(), planks, swaps));
        }
        for (Direction direction : Direction.values()) {
            for (BakedQuad quad : part.getQuads(direction)) {
                builder.addCulledFace(direction, WoodTextures.apply(quad, skin.board(), planks, swaps));
            }
        }
        return new SimpleModelWrapper(builder.build(), part.useAmbientOcclusion(), part.particleMaterial());
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        Identifier wood = woodOf(level.getModelData(pos));
        return new Material.Baked(WoodTextures.spriteOf(wood), false);
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
}
