package io.github.dirnei.mc.servus.foundation.material.client;

import io.github.dirnei.mc.servus.foundation.material.Material;
import io.github.dirnei.mc.servus.foundation.material.MaterialKind;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.model.data.ModelData;

/**
 * The block state model of a material block: quads on the skin's placeholder
 * texture are retextured onto the sprite of the block's material, every other
 * quad passes through, unless the block's swaps name another texture for it.
 * The block reads the material from its model data; items use
 * {@link MaterialItemModel}.
 * A resource reload builds new wrappers, which drops every cache.
 */
public final class MaterialModel implements DynamicBlockStateModel {

    private record PartKey(BlockStateModelPart part, Identifier material, Map<Identifier, Identifier> swaps) {
    }

    private final BlockStateModel base;
    private final MaterialSkin skin;
    private final Map<PartKey, BlockStateModelPart> parts = new ConcurrentHashMap<>();

    public MaterialModel(BlockStateModel base, MaterialKind kind) {
        this(base, MaterialSkin.of(kind));
    }

    public MaterialModel(BlockStateModel base, MaterialSkin skin) {
        this.base = base;
        this.skin = skin;
    }

    private Identifier materialOf(ModelData data) {
        Identifier material = data.get(skin.property());
        return material != null ? material : skin.fallback();
    }

    private Map<Identifier, Identifier> swapsOf(ModelData data) {
        Map<Identifier, Identifier> swaps = skin.swaps() ? data.get(Material.SWAPS) : null;
        return swaps != null ? swaps : Map.of();
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
                             List<BlockStateModelPart> output) {
        ModelData data = level.getModelData(pos);
        Identifier material = materialOf(data);
        Map<Identifier, Identifier> swaps = swapsOf(data);
        List<BlockStateModelPart> baseParts = new ArrayList<>();
        base.collectParts(level, pos, state, random, baseParts);
        for (BlockStateModelPart part : baseParts) {
            output.add(parts.computeIfAbsent(new PartKey(part, material, swaps), key -> retexture(part, material, swaps)));
        }
    }

    private BlockStateModelPart retexture(BlockStateModelPart part, Identifier material, Map<Identifier, Identifier> swaps) {
        TextureAtlasSprite sprite = MaterialTextures.spriteOf(material);
        QuadCollection.Builder builder = new QuadCollection.Builder();
        for (BakedQuad quad : part.getQuads(null)) {
            builder.addUnculledFace(MaterialTextures.apply(quad, skin.placeholder(), sprite, swaps));
        }
        for (Direction direction : Direction.values()) {
            for (BakedQuad quad : part.getQuads(direction)) {
                builder.addCulledFace(direction, MaterialTextures.apply(quad, skin.placeholder(), sprite, swaps));
            }
        }
        return new SimpleModelWrapper(builder.build(), part.useAmbientOcclusion(), part.particleMaterial());
    }

    @Override
    public net.minecraft.client.resources.model.sprite.Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        Identifier material = materialOf(level.getModelData(pos));
        return new net.minecraft.client.resources.model.sprite.Material.Baked(MaterialTextures.spriteOf(material), false);
    }

    @Override
    public net.minecraft.client.resources.model.sprite.Material.Baked particleMaterial() {
        return base.particleMaterial();
    }

    @Override
    @BakedQuad.MaterialFlags
    public int materialFlags() {
        return base.materialFlags();
    }
}
