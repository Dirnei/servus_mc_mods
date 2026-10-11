package io.github.dirnei.mc.servus.foundation.wood.client;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.foundation.wood.Wood;
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
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * The item model of a wood block item: the block model's quads with the
 * boards retextured onto the planks of the stack's wood. Since 1.21.4 items
 * have their own model type; item model definitions of wood blocks name
 * {@code servus_foundation:wood} (see the model datagen).
 */
public final class WoodItemModel implements ItemModel {

    /** The registered type id. */
    public static final Identifier TYPE = Identifier.fromNamespaceAndPath("servus_foundation", "wood");

    private final QuadCollection quads;
    private final ModelRenderProperties properties;
    private final Matrix4fc transformation;
    private final Vector3fc[] extents;
    private final Map<Identifier, List<BakedQuad>> byWood = new ConcurrentHashMap<>();

    private WoodItemModel(QuadCollection quads, ModelRenderProperties properties, Matrix4fc transformation) {
        this.quads = quads;
        this.properties = properties;
        this.transformation = transformation;
        this.extents = CuboidItemModelWrapper.computeExtents(quads.getAll());
    }

    private List<BakedQuad> quadsFor(Identifier wood) {
        return byWood.computeIfAbsent(wood, key -> {
            TextureAtlasSprite planks = WoodTextures.spriteOf(key);
            List<BakedQuad> result = new ArrayList<>();
            for (BakedQuad quad : quads.getAll()) {
                result.add(WoodTextures.apply(quad, planks, Map.of()));
            }
            return List.copyOf(result);
        });
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver, ItemDisplayContext displayContext,
                       @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        Identifier wood = Wood.of(item);
        output.appendModelIdentityElement(this);
        output.appendModelIdentityElement(wood);
        ItemStackRenderState.LayerRenderState layer = output.newLayer();
        layer.setExtents(() -> extents);
        layer.setLocalTransform(transformation);
        properties.applyToLayer(layer, displayContext);
        layer.prepareQuadList().addAll(quadsFor(wood));
    }

    /** {@code {"type": "servus_foundation:wood", "model": "<model>"}}. */
    public record Unbaked(Identifier model) implements ItemModel.Unbaked {

        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Identifier.CODEC.fieldOf("model").forGetter(Unbaked::model)).apply(i, Unbaked::new));

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
            return new WoodItemModel(quads, properties, transformation);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
