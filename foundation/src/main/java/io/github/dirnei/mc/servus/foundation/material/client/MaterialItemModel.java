package io.github.dirnei.mc.servus.foundation.material.client;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * The item model of a material block item: the block model's quads with the
 * placeholder retextured onto the sprite of the stack's material. Item model
 * definitions of material blocks name the skin's item model type, such as
 * {@code servus_foundation:material}.
 */
public final class MaterialItemModel implements ItemModel {

    private final QuadCollection quads;
    private final ModelRenderProperties properties;
    private final Matrix4fc transformation;
    private final Vector3fc[] extents;
    private final Map<Identifier, List<BakedQuad>> byMaterial = new ConcurrentHashMap<>();
    private final MaterialSkin skin;

    private MaterialItemModel(QuadCollection quads, ModelRenderProperties properties, Matrix4fc transformation, MaterialSkin skin) {
        this.quads = quads;
        this.properties = properties;
        this.transformation = transformation;
        this.skin = skin;
        this.extents = CuboidItemModelWrapper.computeExtents(quads.getAll());
    }

    private List<BakedQuad> quadsFor(Identifier material) {
        return byMaterial.computeIfAbsent(material, key -> {
            TextureAtlasSprite sprite = MaterialTextures.spriteOf(key);
            List<BakedQuad> result = new ArrayList<>();
            for (BakedQuad quad : quads.getAll()) {
                result.add(MaterialTextures.apply(quad, skin.placeholder(), sprite, Map.of()));
            }
            return List.copyOf(result);
        });
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver, ItemDisplayContext displayContext,
                       @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        Identifier material = skin.itemMaterial(item);
        output.appendModelIdentityElement(this);
        output.appendModelIdentityElement(material);
        ItemStackRenderState.LayerRenderState layer = output.newLayer();
        layer.setExtents(() -> extents);
        layer.setLocalTransform(transformation);
        properties.applyToLayer(layer, displayContext);
        layer.prepareQuadList().addAll(quadsFor(material));
    }

    /** Read by the skin's item codec, see {@link MaterialSkin#itemCodec()}. */
    public record Unbaked(Identifier model, MaterialSkin skin) implements ItemModel.Unbaked {

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(model);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            ModelBaker baker = context.blockModelBaker();
            ResolvedModel resolved = baker.getModel(model);
            TextureSlots slots = resolved.getTopTextureSlots();
            QuadCollection quads = resolved.bakeTopGeometry(slots, baker, BlockModelRotation.IDENTITY);
            ModelRenderProperties properties = ModelRenderProperties.fromResolvedModel(baker, resolved, slots);
            return new MaterialItemModel(quads, properties, transformation, skin);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return skin.itemCodec();
        }
    }
}
