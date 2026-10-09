package io.github.dirnei.mc.servus.manufacturing.datagen;

import com.klikli_dev.modonomicon.api.datagen.MultiblockProvider;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout.Role;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerStructure;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

/**
 * The multiblocks the journal draws, written from the same layout that forming checks, so the
 * schematic in the book always shows the structure the game accepts.
 */
public final class ServusManufacturingMultiblockProvider extends MultiblockProvider {

    /** The id of the Calciner schematic, used by the journal's multiblock page. */
    public static final ResourceLocation CALCINER = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "calciner");

    /** The Calciner in the schematic; {@code 0} is the anchor on the ground. */
    private static final char CALCINER_SYMBOL = 'C';
    private static final String GROUND = "*";

    ServusManufacturingMultiblockProvider(PackOutput output) {
        super(output, ServusManufacturingIds.MOD_ID);
    }

    @Override
    public void buildMultiblocks() {
        // The wall is one block deep: each layer, top to bottom, is a single row. Visualize anchors the
        // center on the block the player clicks, so the center is a ground row under the Ash Pan and the
        // build stands on that ground instead of sinking into it. Forming checks with CalcinerLayout itself.
        DenseMultiblockBuilder calciner = new DenseMultiblockBuilder();
        for (String row : CalcinerLayout.patternRows()) {
            calciner.layer(row.replace(Role.CALCINER.symbol(), CALCINER_SYMBOL));
        }
        calciner.layer(GROUND + "0" + GROUND);
        for (Role role : Role.values()) {
            calciner.block(role == Role.CALCINER ? CALCINER_SYMBOL : role.symbol(), () -> CalcinerStructure.expected(role));
        }
        calciner.display('0', () -> Blocks.SMOOTH_STONE);
        calciner.display(GROUND.charAt(0), () -> Blocks.SMOOTH_STONE);
        add(CALCINER, calciner.build(false, 1));
    }
}
