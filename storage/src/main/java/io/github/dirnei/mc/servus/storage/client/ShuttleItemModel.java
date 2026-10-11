package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.shuttle.module.ModuleSpec;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleItem;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleLook;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleModules;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

/**
 * The item model of a shuttle tier: it resolves every stack to a model
 * composed of the tier's body, the drive and Handling part of its look steps
 * and its tool. The rail renderer draws the shuttle's item, so the rails and
 * every inventory show the same look. A resource reload bakes a new wrapper,
 * which drops the cache.
 */
public final class ShuttleItemModel extends BakedModelWrapper<BakedModel> {

    /** The shuttle item models this wraps, by item path. */
    public static final Set<String> ITEMS = Set.of("shuttle", "shuttle_mk2", "shuttle_mk3");

    private static final int DRIVE_STEPS = 4;
    private static final int HANDLING_STEPS = 3;

    private final Map<ShuttleLook.Key, BakedModel> composed = new ConcurrentHashMap<>();
    private final ItemOverrides overrides = new ItemOverrides() {
        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            ShuttleModules modules = ShuttleModules.of(stack);
            String tool = modules.toolSpec().map(ModuleSpec::id).orElse("");
            ShuttleLook.Key key = ShuttleLook.Key.of(ShuttleItem.tier(stack), modules.speed(), modules.handling(), tool);
            return composed.computeIfAbsent(key, ShuttleItemModel.this::compose);
        }
    };

    public ShuttleItemModel(BakedModel base) {
        super(base);
    }

    /** Every part model, registered as standalone models so they bake. */
    public static List<ModelResourceLocation> parts(Iterable<String> tools) {
        List<ModelResourceLocation> parts = new ArrayList<>();
        for (int tier = 1; tier <= 3; tier++) {
            parts.add(part("body_mk" + tier));
        }
        for (int step = 0; step < DRIVE_STEPS; step++) {
            parts.add(part("drive_" + step));
        }
        for (int step = 1; step < HANDLING_STEPS; step++) {
            parts.add(part("handling_" + step));
        }
        for (String tool : tools) {
            parts.add(part("tool_" + tool));
        }
        return parts;
    }

    private static ModelResourceLocation part(String name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "shuttle/" + name));
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }

    private BakedModel compose(ShuttleLook.Key key) {
        ModelManager models = Minecraft.getInstance().getModelManager();
        BakedModel missing = models.getMissingModel();
        List<BakedModel> parts = new ArrayList<>();
        parts.add(models.getModel(part("body_mk" + Math.max(1, Math.min(3, key.tier())))));
        parts.add(models.getModel(part("drive_" + Math.min(DRIVE_STEPS - 1, key.speedStep()))));
        if (key.handlingStep() > 0) {
            parts.add(models.getModel(part("handling_" + Math.min(HANDLING_STEPS - 1, key.handlingStep()))));
        }
        if (!key.tool().isEmpty()) {
            parts.add(models.getModel(part("tool_" + key.tool())));
        }
        parts.removeIf(model -> model == missing);
        return new Composed(originalModel, parts);
    }

    /** The parts of one look as one model; the base gives transforms, particle and render types. */
    private static final class Composed extends BakedModelWrapper<BakedModel> {

        private final Map<Direction, List<BakedQuad>> sided = new EnumMap<>(Direction.class);
        private final List<BakedQuad> unsided;

        Composed(BakedModel base, List<BakedModel> parts) {
            super(base);
            RandomSource random = RandomSource.create(42L);
            List<BakedQuad> loose = new ArrayList<>();
            for (BakedModel part : parts) {
                loose.addAll(part.getQuads(null, null, random));
            }
            unsided = List.copyOf(loose);
            for (Direction side : Direction.values()) {
                List<BakedQuad> quads = new ArrayList<>();
                for (BakedModel part : parts) {
                    quads.addAll(part.getQuads(null, side, random));
                }
                sided.put(side, List.copyOf(quads));
            }
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
            return side == null ? unsided : sided.get(side);
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                        ModelData extraData, @Nullable RenderType renderType) {
            return getQuads(state, side, rand);
        }

        @Override
        public ItemOverrides getOverrides() {
            return ItemOverrides.EMPTY;
        }

        /** The wrapper would hand the pass to the base, which draws the body alone. */
        @Override
        public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
            return List.of(this);
        }
    }
}
