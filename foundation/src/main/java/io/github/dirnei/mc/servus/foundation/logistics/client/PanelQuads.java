package io.github.dirnei.mc.servus.foundation.logistics.client;

import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctShapes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.quad.MutableQuad;

/**
 * Bakes the quads of one duct panel: the boxes of {@link DuctShapes#panel}
 * with, on every box face, the sprite, render layer and tint of the panel
 * block's own quads facing that way, one baked quad per source quad so
 * overlays like the grass side survive. UVs follow the box's position, as
 * vanilla picks them for a block model element. Results are cached per block
 * state, side and hollowness; the cache lives as long as the duct model, which
 * is rebuilt on every resource reload.
 */
final class PanelQuads {

    /** Tint indices of panel quads start here, clear of the ring range at {@code DuctParts.RING_TINT_BASE}. */
    static final int PANEL_TINT_BASE = 200;
    /** Room per side for the panel block's own tint indices. */
    static final int TINTS_PER_SIDE = 16;

    private record Key(BlockState state, Direction side, boolean hollow) {
    }

    /** Vertex colour of see-through panels: white at half alpha. */
    private static final int SEE_THROUGH = 0x80FFFFFF;

    private final Map<Key, List<BakedQuad>> cache = new ConcurrentHashMap<>();
    private final Map<Key, List<BakedQuad>> seeThrough = new ConcurrentHashMap<>();

    /** The quads of a panel of {@code state} on {@code side}. */
    List<BakedQuad> quads(BlockState state, Direction side, boolean hollow) {
        return cache.computeIfAbsent(new Key(state, side, hollow), PanelQuads::bake);
    }

    /** The same quads at half alpha in the translucent layer, while the player reveals ducts. */
    List<BakedQuad> seeThroughQuads(BlockState state, Direction side, boolean hollow) {
        return seeThrough.computeIfAbsent(new Key(state, side, hollow), key -> {
            List<BakedQuad> result = new ArrayList<>();
            MutableQuad quad = new MutableQuad();
            for (BakedQuad source : quads(state, side, hollow)) {
                quad.setFrom(source);
                quad.setColor(SEE_THROUGH);
                quad.setSprite(source.materialInfo().sprite(), ChunkSectionLayer.TRANSLUCENT, Sheets.translucentBlockItemSheet());
                result.add(quad.toBakedQuad());
            }
            return List.copyOf(result);
        });
    }

    private static BlockStateModel modelOf(BlockState state) {
        return Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
    }

    private static List<BakedQuad> bake(Key key) {
        BlockStateModel model = modelOf(key.state());
        List<BakedQuad> quads = new ArrayList<>();
        for (DuctShapes.Box box : DuctShapes.panel(key.side().ordinal(), key.hollow())) {
            for (Direction face : Direction.values()) {
                List<BakedQuad> sources = sources(model, face);
                if (sources.isEmpty()) {
                    quads.add(bakeFace(box, face, model.particleMaterial(), -1));
                    continue;
                }
                for (BakedQuad source : sources) {
                    int sourceTint = source.materialInfo().tintIndex();
                    int tint = sourceTint >= 0
                            ? PANEL_TINT_BASE + key.side().ordinal() * TINTS_PER_SIDE + Math.floorMod(sourceTint, TINTS_PER_SIDE)
                            : -1;
                    quads.add(bakeFace(box, face, source, tint));
                }
            }
        }
        return List.copyOf(quads);
    }

    /** The block's quads facing a direction: its culled face quads, else its unculled ones facing that way. */
    private static List<BakedQuad> sources(BlockStateModel model, Direction face) {
        // A fixed seed keeps blocks with random variants stable across rebuilds.
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(42), parts);
        List<BakedQuad> culled = new ArrayList<>();
        List<BakedQuad> facing = new ArrayList<>();
        for (BlockStateModelPart part : parts) {
            culled.addAll(part.getQuads(face));
            for (BakedQuad quad : part.getQuads(null)) {
                if (quad.direction() == face) {
                    facing.add(quad);
                }
            }
        }
        return culled.isEmpty() ? facing : culled;
    }

    private static MutableQuad boxFace(DuctShapes.Box b, Direction face) {
        return new MutableQuad().setCubeFace(face, b.x0() / 16F, b.y0() / 16F, b.z0() / 16F,
                b.x1() / 16F, b.y1() / 16F, b.z1() / 16F);
    }

    /** A box face with the sprite and layer of a source quad. */
    private static BakedQuad bakeFace(DuctShapes.Box b, Direction face, BakedQuad source, int tint) {
        BakedQuad.MaterialInfo info = source.materialInfo();
        return boxFace(b, face)
                .setSprite(info.sprite(), info.layer(), info.itemRenderType())
                .bakeUvsFromPosition()
                .setTintIndex(tint)
                .setShade(true)
                .toBakedQuad();
    }

    /** A box face with the block's particle sprite, for blocks without a quad facing that way. */
    private static BakedQuad bakeFace(DuctShapes.Box b, Direction face, Material.Baked material, int tint) {
        return boxFace(b, face)
                .setSprite(material)
                .bakeUvsFromPosition()
                .setTintIndex(tint)
                .setShade(true)
                .toBakedQuad();
    }
}
