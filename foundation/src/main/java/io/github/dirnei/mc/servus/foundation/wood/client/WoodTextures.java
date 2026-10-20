package io.github.dirnei.mc.servus.foundation.wood.client;

import io.github.dirnei.mc.servus.foundation.wood.Wood;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.model.quad.MutableQuad;

/** Retextures the board quads of wood models onto a planks sprite, shared by the block and item wood models. */
public final class WoodTextures {

    private WoodTextures() {
    }

    /** A texture of the block atlas. */
    static TextureAtlasSprite atlasSprite(Identifier texture) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(texture);
    }

    /** The particle sprite of the planks block's model. */
    public static TextureAtlasSprite spriteOf(Identifier wood) {
        return Minecraft.getInstance().getModelManager().getBlockStateModelSet()
                .getParticleMaterial(Wood.block(wood).defaultBlockState()).sprite();
    }

    /** The quad on the wood's planks sprite when it shows a board, on a swap's sprite when one applies, else itself. */
    static BakedQuad apply(BakedQuad quad, Identifier board, TextureAtlasSprite planks, Map<Identifier, Identifier> swaps) {
        Identifier name = quad.materialInfo().sprite().contents().name();
        if (name.equals(board)) {
            return retexture(quad, planks);
        }
        Identifier swap = swaps.get(name);
        return swap == null ? quad : retexture(quad, atlasSprite(swap));
    }

    /** Copies a quad with every vertex UV moved from its sprite to the same spot on another. */
    static BakedQuad retexture(BakedQuad quad, TextureAtlasSprite sprite) {
        BakedQuad.MaterialInfo info = quad.materialInfo();
        return new MutableQuad().setFrom(quad).setSpriteAndMoveUv(sprite, info.layer(), info.itemRenderType()).toBakedQuad();
    }
}
