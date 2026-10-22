package io.github.dirnei.mc.servus.foundation.material.client;

import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.model.quad.MutableQuad;

/** Retextures the placeholder quads of material models onto a material sprite, shared by the block and item material models. */
public final class MaterialTextures {

    private MaterialTextures() {
    }

    /** A texture of the block atlas. */
    static TextureAtlasSprite atlasSprite(Identifier texture) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(texture);
    }

    /** The particle sprite of the material block's model; {@code material} names a block. */
    public static TextureAtlasSprite spriteOf(Identifier material) {
        return Minecraft.getInstance().getModelManager().getBlockStateModelSet()
                .getParticleMaterial(BuiltInRegistries.BLOCK.getValue(material).defaultBlockState()).sprite();
    }

    /** The quad on the material sprite when it shows the placeholder, on a swap's sprite when one applies, else itself. */
    static BakedQuad apply(BakedQuad quad, Identifier placeholder, TextureAtlasSprite sprite, Map<Identifier, Identifier> swaps) {
        Identifier name = quad.materialInfo().sprite().contents().name();
        if (name.equals(placeholder)) {
            return retexture(quad, sprite);
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
