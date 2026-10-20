package io.github.dirnei.mc.servus.delicacies.datagen;

import io.github.dirnei.mc.servus.delicacies.meat.LeberkaeseBlock;
import io.github.dirnei.mc.servus.delicacies.meat.LoafSlices;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlocks;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ConfiguredModel;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelFile;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelRotations;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

/**
 * The placed Leberkaese loaf, one model per cut state built facing north: crust on top, darker crust on the sides, and
 * the pink cut face on the west end once a slice is off. Faces take their UVs from their position.
 */
final class MeatModels {

    private MeatModels() {
    }

    static void register(BlockStateProvider provider) {
        Identifier crust = provider.modLoc("block/leberkaese_crust");
        Identifier side = provider.modLoc("block/leberkaese_side");
        Identifier inside = provider.modLoc("block/leberkaese_inside");
        ModelFile[] models = new ModelFile[LoafSlices.SLICES];
        for (int cut = 0; cut <= LoafSlices.MAX_CUT; cut++) {
            BlockModelBuilder loaf = provider.models().withExistingParent("leberkaese_cut" + cut, provider.mcLoc("block/block"))
                    .texture("particle", crust).texture("crust", crust).texture("side", side)
                    .texture("end", cut == 0 ? side : inside);
            float[] b = LoafSlices.box(cut);
            BlockModelBuilder.ElementBuilder element = loaf.element().from(b[0], b[1], b[2]).to(b[3], b[4], b[5]);
            for (Direction direction : Direction.values()) {
                String texture = switch (direction) {
                    case UP -> "#crust";
                    case WEST -> "#end";
                    default -> "#side";
                };
                element.face(direction).texture(texture).end();
            }
            models[cut] = loaf;
        }
        provider.getVariantBuilder(ServusDelicaciesBlocks.LEBERKAESE.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(models[state.getValue(LeberkaeseBlock.CUT)])
                .rotationY(ModelRotations.rotationY(state.getValue(LeberkaeseBlock.FACING))).build());
    }
}
