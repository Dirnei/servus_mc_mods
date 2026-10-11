package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

/**
 * The rack's baked model: frame quads pass through, board quads are
 * retextured onto the sprite of the wood the rack remembers. The block reads
 * the wood from its model data; the item model resolves a copy with the
 * stack's wood through its overrides. A resource reload builds new wrappers,
 * which drops every cache.
 */
public final class HeavyRackModel implements IDynamicBakedModel {

    /** The board texture the datagen models use; quads with this sprite show the wood. */
    public static final Identifier BOARD = Identifier.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "block/heavy_rack_board");

    private record QuadKey(Identifier wood, @Nullable BlockState state, @Nullable Direction side, @Nullable RenderType renderType) {
    }

    private final BakedModel base;
    /** The wood of an item model resolved for a stack, or null to read the model data. */
    private final @Nullable Identifier fixedWood;
    private final Map<QuadKey, List<BakedQuad>> quads = new ConcurrentHashMap<>();
    private final Map<Identifier, HeavyRackModel> itemModels = new ConcurrentHashMap<>();
    private final ItemOverrides overrides = new ItemOverrides() {
        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            Identifier wood = HeavyRackBlockEntity.woodOf(stack);
            return itemModels.computeIfAbsent(wood, key -> new HeavyRackModel(base, key));
        }
    };

    public HeavyRackModel(BakedModel base) {
        this(base, null);
    }

    private HeavyRackModel(BakedModel base, @Nullable Identifier fixedWood) {
        this.base = base;
        this.fixedWood = fixedWood;
    }

    private Identifier woodOf(ModelData data) {
        if (fixedWood != null) {
            return fixedWood;
        }
        Identifier wood = data.get(HeavyRackBlockEntity.WOOD);
        return wood != null ? wood : HeavyRackBlockEntity.DEFAULT_WOOD;
    }

    /** The particle sprite of the planks block's default model. */
    private static TextureAtlasSprite spriteOf(Identifier wood) {
        BlockState planks = BuiltInRegistries.BLOCK.getValue(wood).defaultBlockState();
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(planks).getParticleIcon(ModelData.EMPTY);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                    ModelData extraData, @Nullable RenderType renderType) {
        Identifier wood = woodOf(extraData);
        return quads.computeIfAbsent(new QuadKey(wood, state, side, renderType), key -> {
            TextureAtlasSprite sprite = spriteOf(wood);
            List<BakedQuad> result = new ArrayList<>();
            for (BakedQuad quad : base.getQuads(state, side, rand, ModelData.EMPTY, renderType)) {
                result.add(quad.getSprite().contents().name().equals(BOARD) ? retexture(quad, sprite) : quad);
            }
            return List.copyOf(result);
        });
    }

    /** Copies a quad with every vertex UV moved from its sprite to the same spot on another. */
    private static BakedQuad retexture(BakedQuad quad, TextureAtlasSprite sprite) {
        TextureAtlasSprite old = quad.getSprite();
        int[] vertices = quad.getVertices().clone();
        for (int i = 0; i < 4; i++) {
            int offset = i * IQuadTransformer.STRIDE + IQuadTransformer.UV0;
            float u = Float.intBitsToFloat(vertices[offset]);
            float v = Float.intBitsToFloat(vertices[offset + 1]);
            vertices[offset] = Float.floatToRawIntBits(sprite.getU(old.getUOffset(u)));
            vertices[offset + 1] = Float.floatToRawIntBits(sprite.getV(old.getVOffset(v)));
        }
        return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), sprite, quad.isShade(), quad.hasAmbientOcclusion());
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return base.getRenderTypes(state, rand, data);
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
        return base.getRenderTypes(stack, fabulous);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        return spriteOf(woodOf(data));
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
        return fixedWood == null ? overrides : ItemOverrides.EMPTY;
    }
}
