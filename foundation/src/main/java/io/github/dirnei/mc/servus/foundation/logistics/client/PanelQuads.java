package io.github.dirnei.mc.servus.foundation.logistics.client;

import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctShapes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

/**
 * Bakes the quads of one duct panel: the boxes of {@link DuctShapes#panel}
 * with, on every box face, the sprite and tint of the panel block's own quads
 * facing that way, one baked quad per source quad so overlays like the grass
 * side survive. UVs are the box's pixel extent, as vanilla picks them for a
 * block model element. Results are cached per block state, side and
 * hollowness; the cache lives as long as the duct model, which is rebuilt on
 * every resource reload. A panel is drawn in one render type of its block
 * (see {@link #renderType}), so quads are never emitted twice.
 */
final class PanelQuads {

    /** Tint indices of panel quads start here, clear of the ring range at {@code DuctParts.RING_TINT_BASE}. */
    static final int PANEL_TINT_BASE = 200;
    /** Room per side for the panel block's own tint indices. */
    static final int TINTS_PER_SIDE = 16;

    private static final FaceBakery BAKERY = new FaceBakery();

    private record Key(BlockState state, Direction side, boolean hollow) {
    }

    /** Vertex colour of see-through panels: white at half alpha. */
    private static final int SEE_THROUGH = 0x80FFFFFF;

    private final Map<Key, List<BakedQuad>> cache = new ConcurrentHashMap<>();
    private final Map<Key, List<BakedQuad>> seeThrough = new ConcurrentHashMap<>();

    /** The render type a panel of the block is drawn in: the first one its model uses, solid without any. */
    static RenderType renderType(BlockState state, RandomSource rand) {
        ChunkRenderTypeSet types = modelOf(state).getRenderTypes(state, rand, ModelData.EMPTY);
        for (RenderType type : types) {
            return type;
        }
        return RenderType.solid();
    }

    /** The quads of a panel of {@code state} on {@code side}. */
    List<BakedQuad> quads(BlockState state, Direction side, boolean hollow) {
        return cache.computeIfAbsent(new Key(state, side, hollow), PanelQuads::bake);
    }

    /** The same quads at half alpha, for the translucent pass while the player reveals ducts. */
    List<BakedQuad> seeThroughQuads(BlockState state, Direction side, boolean hollow) {
        return seeThrough.computeIfAbsent(new Key(state, side, hollow),
                key -> QuadTransformers.applyingColor(SEE_THROUGH).process(quads(state, side, hollow)));
    }

    private static BakedModel modelOf(BlockState state) {
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
    }

    private static List<BakedQuad> bake(Key key) {
        BakedModel model = modelOf(key.state());
        // A fixed seed keeps blocks with random variants stable across rebuilds.
        RandomSource rand = RandomSource.create(42);
        List<BakedQuad> quads = new ArrayList<>();
        for (DuctShapes.Box box : DuctShapes.panel(key.side().ordinal(), key.hollow())) {
            for (Direction face : Direction.values()) {
                List<BakedQuad> sources = sources(model, key.state(), face, rand);
                if (sources.isEmpty()) {
                    quads.add(bakeFace(box, face, model.getParticleIcon(ModelData.EMPTY), -1));
                    continue;
                }
                for (BakedQuad source : sources) {
                    int tint = source.isTinted()
                            ? PANEL_TINT_BASE + key.side().ordinal() * TINTS_PER_SIDE + Math.floorMod(source.getTintIndex(), TINTS_PER_SIDE)
                            : -1;
                    quads.add(bakeFace(box, face, source.getSprite(), tint));
                }
            }
        }
        return List.copyOf(quads);
    }

    /** The block's quads facing a direction: its culled face quads, else its unculled ones facing that way. */
    private static List<BakedQuad> sources(BakedModel model, BlockState state, Direction face, RandomSource rand) {
        rand.setSeed(42);
        List<BakedQuad> culled = model.getQuads(state, face, rand, ModelData.EMPTY, null);
        if (!culled.isEmpty()) {
            return culled;
        }
        rand.setSeed(42);
        List<BakedQuad> facing = new ArrayList<>();
        for (BakedQuad quad : model.getQuads(state, null, rand, ModelData.EMPTY, null)) {
            if (quad.getDirection() == face) {
                facing.add(quad);
            }
        }
        return facing;
    }

    private static BakedQuad bakeFace(DuctShapes.Box b, Direction face, net.minecraft.client.renderer.texture.TextureAtlasSprite sprite,
                                      int tint) {
        float[] uvs = switch (face) {
            case DOWN -> new float[] {b.x0(), 16 - b.z1(), b.x1(), 16 - b.z0()};
            case UP -> new float[] {b.x0(), b.z0(), b.x1(), b.z1()};
            case NORTH -> new float[] {16 - b.x1(), 16 - b.y1(), 16 - b.x0(), 16 - b.y0()};
            case SOUTH -> new float[] {b.x0(), 16 - b.y1(), b.x1(), 16 - b.y0()};
            case WEST -> new float[] {b.z0(), 16 - b.y1(), b.z1(), 16 - b.y0()};
            case EAST -> new float[] {16 - b.z1(), 16 - b.y1(), 16 - b.z0(), 16 - b.y0()};
        };
        BlockElementFace element = new BlockElementFace(null, tint, "", new BlockFaceUV(uvs, 0));
        return BAKERY.bakeQuad(new Vector3f(b.x0(), b.y0(), b.z0()), new Vector3f(b.x1(), b.y1(), b.z1()), element, sprite, face,
                BlockModelRotation.X0_Y0, null, true);
    }
}
