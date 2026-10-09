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

    /** The Calciner in the schematic; {@code 0}, the anchor, is the Ash Pan on the ground. */
    private static final char CALCINER_SYMBOL = 'C';

    ServusManufacturingMultiblockProvider(PackOutput output) {
        super(output, ServusManufacturingIds.MOD_ID);
    }

    @Override
    public void buildMultiblocks() {
        // The wall is one block deep: each layer, top to bottom, is a single row. Visualize anchors the
        // center one block above the block the player clicks, so the Ash Pan at the bottom is the center
        // and the build stands on the clicked ground. Forming checks with CalcinerLayout itself.
        DenseMultiblockBuilder calciner = new DenseMultiblockBuilder();
        for (String row : CalcinerLayout.patternRows()) {
            calciner.layer(row.replace(Role.CALCINER.symbol(), CALCINER_SYMBOL).replace(Role.ASH_PAN.symbol(), '0'));
        }
        for (Role role : Role.values()) {
            if (role == Role.AIR) {
                // A space is air in Modonomicon's pattern already.
                continue;
            }
            char symbol = switch (role) {
                case CALCINER -> CALCINER_SYMBOL;
                case ASH_PAN -> '0';
                default -> role.symbol();
            };
            calciner.block(symbol, () -> CalcinerStructure.expected(role));
        }
        add(CALCINER, calciner.build(false, 1));
    }
}
