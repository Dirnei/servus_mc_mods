package io.github.dirnei.mc.servus.storage.datagen;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;

/**
 * Datagen for the shuttle parts under {@code models/shuttle/}: a body with its
 * hold per tier, a drive per Speed look step, a Handling part per step and a
 * model per tool. The item models of the three tiers are children of their
 * body; the client composes the rest per stack. Model +z is the front. The
 * body spans 2 to 14, the drive 1 to 15; only a tool reaches further. The
 * left side of the hold (x = 2, y 5 to 8, z 5 to 11) stays free for the name
 * plate. Textures are vanilla placeholders.
 */
final class ShuttleModels {

    private static final String CHASSIS = "minecraft:block/smooth_stone";
    private static final String WHEEL = "minecraft:block/coal_block";
    private static final String TRACK = "minecraft:block/blackstone";
    private static final String COPPER = "minecraft:block/copper_block";
    private static final String ARM = "minecraft:block/yellow_terracotta";
    private static final String IRON = "minecraft:block/iron_block";

    private ShuttleModels() {
    }

    static void shuttle(BlockStateProvider provider) {
        body(provider, 1, "minecraft:block/oak_planks");
        body(provider, 2, IRON);
        body(provider, 3, "minecraft:block/diamond_block");
        drives(provider);
        handling(provider);
        harvester(provider);
        provider.itemModels().withExistingParent("shuttle", part(provider, "body_mk1"));
        provider.itemModels().withExistingParent("shuttle_mk2", part(provider, "body_mk2"));
        provider.itemModels().withExistingParent("shuttle_mk3", part(provider, "body_mk3"));
    }

    private static void body(BlockStateProvider provider, int tier, String hold) {
        BlockModelBuilder model = model(provider, "body_mk" + tier, hold);
        box(model, 2, 2, 2, 14, 4, 14, CHASSIS);
        if (tier == 1) {
            // An open crate: four walls, the cargo shows inside.
            box(model, 2, 4, 2, 14, 9, 3, hold);
            box(model, 2, 4, 13, 14, 9, 14, hold);
            box(model, 2, 4, 3, 3, 9, 13, hold);
            box(model, 13, 4, 3, 14, 9, 13, hold);
            return;
        }
        box(model, 2, 4, 2, 14, 10, 14, hold);
        // A darker lid band.
        box(model, 2, 10, 2, 14, 11, 14, CHASSIS);
        if (tier == 3) {
            String gold = "minecraft:block/gold_block";
            box(model, 2, 11, 2, 4, 12, 4, gold);
            box(model, 12, 11, 2, 14, 12, 4, gold);
            box(model, 2, 11, 12, 4, 12, 14, gold);
            box(model, 12, 11, 12, 14, 12, 14, gold);
        }
    }

    private static void drives(BlockStateProvider provider) {
        BlockModelBuilder four = model(provider, "drive_0", WHEEL);
        wheels(four, 3, 10);

        BlockModelBuilder six = model(provider, "drive_1", WHEEL);
        wheels(six, 3, 6.5F, 10);

        BlockModelBuilder tracks = model(provider, "drive_2", TRACK);
        tracks(tracks);

        BlockModelBuilder racing = model(provider, "drive_3", TRACK);
        tracks(racing);
        // Two exhaust stacks at the back corners, beside the hold.
        box(racing, 1, 3, 2, 2, 13, 3, COPPER);
        box(racing, 14, 3, 2, 15, 13, 3, COPPER);
    }

    private static void wheels(BlockModelBuilder model, float... zs) {
        for (float z : zs) {
            box(model, 1, 0, z, 2, 3, z + 3, WHEEL);
            box(model, 14, 0, z, 15, 3, z + 3, WHEEL);
        }
    }

    private static void tracks(BlockModelBuilder model) {
        box(model, 1, 0, 2, 2.5F, 3, 14, TRACK);
        box(model, 13.5F, 0, 2, 15, 3, 14, TRACK);
    }

    private static void handling(BlockStateProvider provider) {
        // A loading mast with two forks at the back.
        BlockModelBuilder mast = model(provider, "handling_1", ARM);
        mastAndForks(mast, 9);

        // The mast grows and carries a crane beam with a hook over the hold.
        BlockModelBuilder crane = model(provider, "handling_2", ARM);
        mastAndForks(crane, 13);
        box(crane, 7, 12, 1, 9, 13, 10, ARM);
        box(crane, 7.5F, 10, 9, 8.5F, 12, 10, IRON);
    }

    private static void mastAndForks(BlockModelBuilder model, float height) {
        box(model, 3, 1, 1, 4, height, 2, ARM);
        box(model, 12, 1, 1, 13, height, 2, ARM);
        box(model, 4, 3, 1, 12, 4, 2, ARM);
        box(model, 5, 0.5F, 0, 6, 1.5F, 2, IRON);
        box(model, 10, 0.5F, 0, 11, 1.5F, 2, IRON);
    }

    private static void harvester(BlockStateProvider provider) {
        String reel = "minecraft:block/hay_block_side";
        BlockModelBuilder model = model(provider, "tool_harvester", reel);
        // Arms from the body to the reel axle.
        box(model, 2, 3, 13, 3, 6, 17, IRON);
        box(model, 13, 3, 13, 14, 6, 17, IRON);
        // The reel across the front, wider than the body.
        box(model, -2, 4, 15, 18, 6, 17, reel);
        box(model, -2, 6, 15.5F, 18, 7, 16.5F, reel);
        box(model, -2, 3, 15.5F, 18, 4, 16.5F, reel);
        // The cutter bar along the ground.
        box(model, -2, 0.5F, 15, 18, 1.5F, 18, "minecraft:block/iron_bars");
    }

    private static ResourceLocation part(BlockStateProvider provider, String name) {
        return provider.modLoc("shuttle/" + name);
    }

    private static BlockModelBuilder model(BlockStateProvider provider, String name, String particle) {
        return provider.models().getBuilder("shuttle/" + name)
                .parent(new ModelFile.UncheckedModelFile("minecraft:block/block"))
                .texture("particle", particle);
    }

    /**
     * One box with the texture on every face. UVs start at the corner of the
     * sprite and are as big as the face, capped at the sprite, so parts that
     * reach past the block never sample the atlas beside the sprite.
     */
    private static void box(BlockModelBuilder model, float x1, float y1, float z1, float x2, float y2, float z2, String texture) {
        String key = texture.substring(texture.lastIndexOf('/') + 1);
        model.texture(key, texture);
        float dx = Math.min(16, x2 - x1);
        float dy = Math.min(16, y2 - y1);
        float dz = Math.min(16, z2 - z1);
        model.element().from(x1, y1, z1).to(x2, y2, z2).allFaces((direction, face) -> {
            float u;
            float v;
            if (direction.getAxis() == Direction.Axis.Y) {
                u = dx;
                v = dz;
            } else if (direction.getAxis() == Direction.Axis.X) {
                u = dz;
                v = dy;
            } else {
                u = dx;
                v = dy;
            }
            face.texture("#" + key).uvs(0, 0, u, v);
        }).end();
    }
}
