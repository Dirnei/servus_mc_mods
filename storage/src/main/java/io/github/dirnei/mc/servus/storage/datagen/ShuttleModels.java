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
 * body spans 2 to 14, the drive 0.5 to 15.5; only a tool reaches further. The
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
        spreader(provider);
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
        wheels(four, 4, 3, 9);

        BlockModelBuilder six = model(provider, "drive_1", WHEEL);
        wheels(six, 3.5F, 2.25F, 6.25F, 10.25F);

        BlockModelBuilder tracks = model(provider, "drive_2", TRACK);
        tracks(tracks);

        BlockModelBuilder racing = model(provider, "drive_3", TRACK);
        tracks(racing);
        // Two exhaust stacks at the back corners, beside the hold.
        box(racing, 1, 3, 2, 2, 13, 3, COPPER);
        box(racing, 14, 3, 2, 15, 13, 3, COPPER);
    }

    /**
     * Wheels of the given diameter starting at each z, on both sides. The
     * shuttle is drawn at 0.8 scale around the block centre, so model x
     * (t - 8) / 0.8 + 8 lands on block pixel t: the treads at 2..4 and 12..14
     * stand on the rail heads 3..5 and 11..13 (block 3.2..4.8 and 11.2..12.8)
     * under the chassis. Outside the chassis (x 1..2 and 14..15) each wheel
     * shows a rounded disc taller than the chassis bottom with an iron hub, so
     * it reads as a wheel from the side. Every wheel bottom is model y 0, the
     * top of the heads.
     */
    private static void wheels(BlockModelBuilder model, float diameter, float... zs) {
        float hub = 1;
        for (float z : zs) {
            float mid = diameter / 2;
            for (float[] side : new float[][] {{1, 2, 0.5F, 1, 2, 4}, {14, 15, 15, 15.5F, 12, 14}}) {
                // The disc: a full height core and a slightly inset full length band round its corners.
                box(model, side[0], 0, z + 0.5F, side[1], diameter, z + diameter - 0.5F, WHEEL);
                box(model, side[0] + 0.1F, 0.5F, z, side[1] - 0.1F, diameter - 0.5F, z + diameter, WHEEL);
                box(model, side[2], mid - hub / 2, z + mid - hub / 2, side[3], mid + hub / 2, z + mid + hub / 2, IRON);
                // The tread under the chassis, on the head.
                box(model, side[4], 0, z + 0.5F, side[5], 2, z + diameter - 0.5F, WHEEL);
            }
        }
    }

    /** Tracks at 1.75..4.25 and 11.75..14.25, exactly over the heads 3..5 and 11..13 at 0.8 scale, with iron road wheel hubs. */
    private static void tracks(BlockModelBuilder model) {
        box(model, 1.75F, 0, 2, 4.25F, 3, 14, TRACK);
        box(model, 11.75F, 0, 2, 14.25F, 3, 14, TRACK);
        for (float z : new float[] {3.5F, 7.5F, 11.5F}) {
            box(model, 1.25F, 1, z, 1.75F, 2, z + 1, IRON);
            box(model, 14.25F, 1, z, 14.75F, 2, z + 1, IRON);
        }
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

    /** A hopper box on two arms at the front, a funnel under it and a spreader bar across the front, wider than the body. */
    private static void spreader(BlockStateProvider provider) {
        String hopper = "minecraft:block/hopper_outside";
        String bone = "minecraft:block/bone_block_side";
        BlockModelBuilder model = model(provider, "tool_spreader", hopper);
        box(model, 2, 3, 13, 3, 6, 15, IRON);
        box(model, 13, 3, 13, 14, 6, 15, IRON);
        // The hopper box with its rim.
        box(model, 3, 6, 14, 13, 11, 19, hopper);
        box(model, 2.5F, 11, 13.5F, 13.5F, 12, 19.5F, IRON);
        // The funnel down to the spinner.
        box(model, 5, 3, 15, 11, 6, 18, hopper);
        box(model, 7, 1.5F, 16, 9, 3, 17, IRON);
        // The spreader bar across the front.
        box(model, -2, 0.5F, 15.5F, 18, 1.5F, 17.5F, bone);
        box(model, -2, 1.5F, 16, -1, 3, 17, IRON);
        box(model, 17, 1.5F, 16, 18, 3, 17, IRON);
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
