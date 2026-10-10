package io.github.dirnei.mc.servus.manufacturing.datagen;

import com.klikli_dev.modonomicon.api.datagen.MultiblockProvider;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout;
import io.github.dirnei.mc.servus.manufacturing.alchemy.coagulator.CoagulatorLayout;
import io.github.dirnei.mc.servus.manufacturing.alchemy.coagulator.CoagulatorStructure;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout.Role;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerStructure;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorLayout;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorTierData;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorTiers;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import io.github.dirnei.mc.servus.manufacturing.alchemy.purifier.PurifierLayout;
import io.github.dirnei.mc.servus.manufacturing.alchemy.purifier.PurifierStructure;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;

/**
 * The multiblocks the journal draws, written from the same layout that forming checks, so the
 * schematic in the book always shows the structure the game accepts.
 */
public final class ServusManufacturingMultiblockProvider extends MultiblockProvider {

    /** The id of the Calciner schematic, used by the journal's multiblock page. */
    public static final ResourceLocation CALCINER = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "calciner");
    /** The id of the Purifier schematic. */
    public static final ResourceLocation PURIFIER = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "purifier");
    /** The id of the Coagulator schematic. */
    public static final ResourceLocation COAGULATOR = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "coagulator");

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

        // The Coagulator is the bottom middle, so it is the center and stands on the clicked ground.
        DenseMultiblockBuilder coagulator = new DenseMultiblockBuilder();
        for (String row : CoagulatorLayout.patternRows()) {
            coagulator.layer(row);
        }
        for (CoagulatorLayout.Role role : CoagulatorLayout.Role.values()) {
            coagulator.block(role.symbol(), () -> CoagulatorStructure.expected(role));
        }
        add(COAGULATOR, coagulator.build(false, 1));

        // The Purifier: the Basin is the bottom middle, so it is the anchor; planks and logs of any wood.
        DenseMultiblockBuilder purifier = new DenseMultiblockBuilder();
        for (String row : PurifierLayout.patternRows()) {
            purifier.layer(row.replace(PurifierLayout.Role.BASIN.symbol(), '0'));
        }
        purifier.block(PurifierLayout.Role.VAT.symbol(), () -> PurifierStructure.shown(PurifierLayout.Role.VAT));
        purifier.block('0', () -> PurifierStructure.shown(PurifierLayout.Role.BASIN));
        purifier.tag(PurifierLayout.Role.PLANKS.symbol(), BlockTags.PLANKS, () -> Blocks.OAK_PLANKS);
        purifier.tag(PurifierLayout.Role.LOGS.symbol(), BlockTags.LOGS, () -> Blocks.OAK_LOG);
        add(PURIFIER, purifier.build(false, 1));

        // The Athanor on its hearth, then each shipped tier with every tier below it.
        athanor("nigredo", List.of());
        List<AthanorTiers.BlockAt> blocks = new ArrayList<>();
        for (AthanorTiers.TierDef tier : AthanorTierData.SHIPPED.stream().map(AthanorTierData::readShipped)
                .sorted(java.util.Comparator.comparingInt(AthanorTiers.TierDef::number)).toList()) {
            blocks.addAll(tier.blocks());
            athanor(tier.name(), List.copyOf(blocks));
        }
    }

    /** The id of the schematic of the Athanor at {@code stage}, such as {@code nigredo} or {@code albedo}. */
    public static ResourceLocation athanor(String stage) {
        return ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "athanor_" + stage);
    }

    private void athanor(String stage, List<AthanorTiers.BlockAt> extra) {
        AthanorLayout.Schematic schematic = AthanorLayout.schematic(extra);
        DenseMultiblockBuilder builder = new DenseMultiblockBuilder();
        for (List<String> layer : schematic.layers()) {
            builder.layer(layer.toArray(String[]::new));
        }
        schematic.mapping().forEach((symbol, id) -> builder.block(symbol, () -> BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id))));
        add(athanor(stage), builder.build(false, 1));
    }
}
