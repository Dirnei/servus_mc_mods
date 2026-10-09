package io.github.dirnei.mc.servus.manufacturing.datagen;

import com.klikli_dev.modonomicon.api.datagen.MultiblockProvider;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout.Role;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerStructure;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

/**
 * The multiblocks the journal draws, written from the same layout that forming checks, so the
 * schematic in the book always shows the structure the game accepts.
 */
public final class ServusManufacturingMultiblockProvider extends MultiblockProvider {

    /** The id of the Calciner schematic, used by the journal's multiblock page. */
    public static final ResourceLocation CALCINER = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "calciner");

    ServusManufacturingMultiblockProvider(PackOutput output) {
        super(output, ServusManufacturingIds.MOD_ID);
    }

    @Override
    public void buildMultiblocks() {
        // The wall is one block deep: each layer, top to bottom, is a single row.
        DenseMultiblockBuilder calciner = new DenseMultiblockBuilder();
        for (String row : CalcinerLayout.patternRows()) {
            calciner.layer(row);
        }
        for (Role role : Role.values()) {
            calciner.block(role.symbol(), () -> CalcinerStructure.expected(role));
        }
        add(CALCINER, calciner);
    }
}
