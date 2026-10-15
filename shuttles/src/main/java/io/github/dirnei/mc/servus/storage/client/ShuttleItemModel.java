package io.github.dirnei.mc.servus.storage.client;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.shuttle.module.ModuleSpec;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleItem;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleLook;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleModuleItem;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleModules;
import java.util.ArrayList;
import java.util.HashMap;
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
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * The item model of a shuttle tier: every stack shows the tier's body, the
 * drive and Handling part of its look steps and its tool, composed from the
 * part models under {@code models/shuttle/}. The rail renderer draws the
 * shuttle's item, so the rails and every inventory show the same look. Item
 * model definitions of the shuttle items name {@link #TYPE}.
 */
public final class ShuttleItemModel implements ItemModel {

    /** The registered item model type. */
    public static final Identifier TYPE = Identifier.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "shuttle");

    private static final int DRIVE_STEPS = 4;
    private static final int HANDLING_STEPS = 3;

    private record Look(List<BakedQuad> quads, Vector3fc[] extents) {
    }

    private final Map<String, List<BakedQuad>> parts;
    private final ModelRenderProperties properties;
    private final Matrix4fc transformation;
    private final Map<ShuttleLook.Key, Look> composed = new ConcurrentHashMap<>();

    private ShuttleItemModel(Map<String, List<BakedQuad>> parts, ModelRenderProperties properties, Matrix4fc transformation) {
        this.parts = parts;
        this.properties = properties;
        this.transformation = transformation;
    }

    /** Every part name: the bodies, drive and Handling steps and the tool of every registered module. */
    private static List<String> partNames() {
        List<String> names = new ArrayList<>();
        for (int tier = 1; tier <= 3; tier++) {
            names.add("body_mk" + tier);
        }
        for (int step = 0; step < DRIVE_STEPS; step++) {
            names.add("drive_" + step);
        }
        for (int step = 1; step < HANDLING_STEPS; step++) {
            names.add("handling_" + step);
        }
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof ShuttleModuleItem module) {
                names.add("tool_" + module.spec().id());
            }
        }
        return names;
    }

    private static Identifier part(String name) {
        return Identifier.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "shuttle/" + name);
    }

    private Look compose(ShuttleLook.Key key) {
        List<String> names = new ArrayList<>();
        names.add("body_mk" + Math.max(1, Math.min(3, key.tier())));
        names.add("drive_" + Math.min(DRIVE_STEPS - 1, key.speedStep()));
        if (key.handlingStep() > 0) {
            names.add("handling_" + Math.min(HANDLING_STEPS - 1, key.handlingStep()));
        }
        if (!key.tool().isEmpty()) {
            names.add("tool_" + key.tool());
        }
        List<BakedQuad> quads = new ArrayList<>();
        for (String name : names) {
            quads.addAll(parts.getOrDefault(name, List.of()));
        }
        return new Look(List.copyOf(quads), CuboidItemModelWrapper.computeExtents(quads));
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver, ItemDisplayContext displayContext,
                       @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        ShuttleModules modules = ShuttleModules.of(item);
        String tool = modules.toolSpec().map(ModuleSpec::id).orElse("");
        ShuttleLook.Key key = ShuttleLook.Key.of(ShuttleItem.tier(item), modules.speed(), modules.handling(), tool);
        Look look = composed.computeIfAbsent(key, this::compose);
        output.appendModelIdentityElement(this);
        output.appendModelIdentityElement(key);
        ItemStackRenderState.LayerRenderState layer = output.newLayer();
        layer.setExtents(look::extents);
        layer.setLocalTransform(transformation);
        properties.applyToLayer(layer, displayContext);
        layer.prepareQuadList().addAll(look.quads());
    }

    /** {@code {"type": "servus_storage:shuttle", "model": "<model>"}}; the model gives transforms and particle. */
    public record Unbaked(Identifier model) implements ItemModel.Unbaked {

        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Identifier.CODEC.fieldOf("model").forGetter(Unbaked::model)).apply(i, Unbaked::new));

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(model);
            for (String name : partNames()) {
                resolver.markDependency(part(name));
            }
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            ModelBaker baker = context.blockModelBaker();
            ResolvedModel base = baker.getModel(model);
            ModelRenderProperties properties = ModelRenderProperties.fromResolvedModel(baker, base, base.getTopTextureSlots());
            Map<String, List<BakedQuad>> parts = new HashMap<>();
            for (String name : partNames()) {
                ResolvedModel resolved = baker.getModel(part(name));
                TextureSlots slots = resolved.getTopTextureSlots();
                parts.put(name, List.copyOf(resolved.bakeTopGeometry(slots, baker, BlockModelRotation.IDENTITY).getAll()));
            }
            return new ShuttleItemModel(parts, properties, transformation);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
