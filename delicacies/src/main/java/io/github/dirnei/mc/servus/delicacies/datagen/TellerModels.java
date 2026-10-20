package io.github.dirnei.mc.servus.delicacies.datagen;

import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlocks;
import io.github.dirnei.mc.servus.delicacies.teller.MealBlock;
import io.github.dirnei.mc.servus.delicacies.teller.MealKind;
import io.github.dirnei.mc.servus.delicacies.teller.MealPart;
import io.github.dirnei.mc.servus.delicacies.teller.MealParts;
import io.github.dirnei.mc.servus.delicacies.teller.MealPortions;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ConfiguredModel;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelFile;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelRotations;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;

/**
 * The Plate and one model per plated meal and portion count, built facing north from {@link MealParts}: the stepped
 * plate and the food boxes on it. Faces take their UVs from their position; the meals turn by their facing and the
 * items show the full meal.
 */
final class TellerModels {

    private TellerModels() {
    }

    static void register(BlockStateProvider provider) {
        ModelFile plate = model(provider, "plate", MealParts.PLATE);
        provider.simpleBlockWithItem(ServusDelicaciesBlocks.PLATE.get(), plate);
        for (MealKind kind : MealKind.values()) {
            ModelFile[] models = new ModelFile[MealPortions.FULL + 1];
            for (int left = 1; left <= MealPortions.FULL; left++) {
                List<MealPart> parts = new ArrayList<>(MealParts.PLATE);
                parts.addAll(MealParts.food(kind, left));
                models[left] = model(provider, kind.id() + "_" + left, parts);
            }
            MealBlock block = ServusDelicaciesBlocks.meal(kind);
            provider.getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                    .modelFile(models[state.getValue(MealBlock.PORTIONS)])
                    .rotationY(ModelRotations.rotationY(state.getValue(MealBlock.FACING))).build());
            provider.simpleBlockItem(block, models[MealPortions.FULL]);
        }
    }

    /** A model of {@code parts}; each texture key names a block texture of the mod. Bottoms on the table cull against it. */
    private static ModelFile model(BlockStateProvider provider, String name, List<MealPart> parts) {
        BlockModelBuilder model = provider.models().withExistingParent(name, provider.mcLoc("block/block"))
                .texture("particle", provider.modLoc("block/plate"));
        for (MealPart part : parts) {
            model.texture(part.top(), provider.modLoc("block/" + part.top()));
            model.texture(part.side(), provider.modLoc("block/" + part.side()));
            float[] b = part.box();
            BlockModelBuilder.ElementBuilder element = model.element().from(b[0], b[1], b[2]).to(b[3], b[4], b[5]);
            for (MealPart.Face face : MealPart.Face.values()) {
                if (!part.shows(face)) {
                    continue;
                }
                Direction direction = Direction.valueOf(face.name());
                var built = element.face(direction)
                        .texture("#" + (direction == Direction.UP ? part.top() : part.side()));
                if (direction == Direction.DOWN && b[1] == 0) {
                    built.cullface(Direction.DOWN);
                }
                built.end();
            }
        }
        return model;
    }
}
