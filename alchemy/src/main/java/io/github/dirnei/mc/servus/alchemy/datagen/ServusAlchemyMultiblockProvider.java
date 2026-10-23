package io.github.dirnei.mc.servus.alchemy.datagen;

import com.klikli_dev.modonomicon.api.datagen.MultiblockProvider;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorLayout;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTierData;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTiers;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.AthanorRitual;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.AthanorRitualData;
import io.github.dirnei.mc.servus.alchemy.calciner.CalcinerLayout;
import io.github.dirnei.mc.servus.alchemy.calciner.CalcinerLayout.Role;
import io.github.dirnei.mc.servus.alchemy.calciner.CalcinerStructure;
import io.github.dirnei.mc.servus.alchemy.coagulator.CoagulatorLayout;
import io.github.dirnei.mc.servus.alchemy.coagulator.CoagulatorStructure;
import io.github.dirnei.mc.servus.alchemy.purifier.PurifierLayout;
import io.github.dirnei.mc.servus.alchemy.purifier.PurifierStructure;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The multiblocks the journal draws, written from the same layout that forming checks, so the
 * schematic in the book always shows the structure the game accepts.
 */
public final class ServusAlchemyMultiblockProvider extends MultiblockProvider {

    /** The id of the Calciner schematic, used by the journal's multiblock page. */
    public static final Identifier CALCINER = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "calciner");
    /** The id of the Purifier schematic. */
    public static final Identifier PURIFIER = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "purifier");
    /** The id of the Coagulator schematic. */
    public static final Identifier COAGULATOR = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "coagulator");

    private static final String AIR = "minecraft:air";

    /** The Calciner in the schematic; {@code 0}, the anchor, is the Ash Pan on the ground. */
    private static final char CALCINER_SYMBOL = 'C';

    ServusAlchemyMultiblockProvider(PackOutput output) {
        super(output, ServusAlchemyIds.MOD_ID);
    }

    @Override
    public void buildMultiblocks() {
        calciner();
        coagulator();
        purifier();
        athanors();
    }

    /**
     * The wall is one block deep: each layer, top to bottom, is a single row. Visualize anchors the
     * center one block above the clicked block, so the Ash Pan at the bottom is the center and the
     * build stands on the clicked ground.
     */
    private void calciner() {
        DenseMultiblockBuilder calciner = new DenseMultiblockBuilder();
        for (String row : CalcinerLayout.patternRows()) {
            calciner.layer(row.replace(Role.CALCINER.symbol(), CALCINER_SYMBOL).replace(Role.ASH_PAN.symbol(), '0'));
        }
        for (Role role : Role.values()) {
            char symbol = switch (role) {
                case CALCINER -> CALCINER_SYMBOL;
                case ASH_PAN -> '0';
                default -> role.symbol();
            };
            calciner.block(symbol, () -> CalcinerStructure.expected(role));
        }
        add(CALCINER, calciner.build(false, 1));
    }

    /** The Coagulator is the bottom middle, so it is the center and stands on the clicked ground. */
    private void coagulator() {
        DenseMultiblockBuilder coagulator = new DenseMultiblockBuilder();
        for (String row : CoagulatorLayout.patternRows()) {
            coagulator.layer(row);
        }
        for (CoagulatorLayout.Role role : CoagulatorLayout.Role.values()) {
            coagulator.block(role.symbol(), () -> CoagulatorStructure.expected(role));
        }
        add(COAGULATOR, coagulator.build(false, 1));
    }

    /** The log of any wood is the bottom middle, so it is the anchor; anything may stand beside the Vat. */
    private void purifier() {
        DenseMultiblockBuilder purifier = new DenseMultiblockBuilder();
        for (String row : PurifierLayout.patternRows()) {
            purifier.layer(row.replace(PurifierLayout.Role.LOG.symbol(), '0'));
        }
        for (PurifierLayout.Role role : List.of(PurifierLayout.Role.VAT, PurifierLayout.Role.BASIN, PurifierLayout.Role.CROCK)) {
            purifier.block(role.symbol(), () -> PurifierStructure.shown(role));
        }
        purifier.tag('0', BlockTags.LOGS, () -> Blocks.OAK_LOG);
        add(PURIFIER, purifier.build(false, 1));
    }

    /**
     * The Athanor on its hearth, then each shipped tier with every tier below it. A block a shipped ritual grows shows as the
     * block the player places for it; a block a ritual grows out of air is left out.
     */
    private void athanors() {
        athanor("nigredo", List.of());
        List<AthanorRitual.Turn> turns = AthanorRitualData.SHIPPED.stream().map(AthanorRitualData::readShipped)
                .flatMap(ritual -> ritual.turns().stream()).toList();
        List<AthanorTiers.BlockAt> blocks = new ArrayList<>();
        for (AthanorTiers.TierDef tier : AthanorTierData.SHIPPED.stream().map(AthanorTierData::readShipped)
                .sorted(Comparator.comparingInt(AthanorTiers.TierDef::number)).toList()) {
            tier.blocks().stream().map(block -> placedFor(block, turns)).filter(block -> !block.block().equals(AIR)).forEach(blocks::add);
            athanor(tier.name(), List.copyOf(blocks));
        }
    }

    /** What the player places for {@code block}: the block a ritual turns into it there, else the block itself. */
    private static AthanorTiers.BlockAt placedFor(AthanorTiers.BlockAt block, List<AthanorRitual.Turn> turns) {
        return turns.stream()
                .filter(turn -> turn.to().equals(block))
                .map(AthanorRitual.Turn::from)
                .findFirst()
                .orElse(block);
    }

    /** The id of the schematic of the Athanor at {@code stage}, such as {@code nigredo} or {@code albedo}. */
    public static Identifier athanor(String stage) {
        return Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "athanor_" + stage);
    }

    /**
     * The anchor {@code 0} is the middle of the bottom layer, so the build stands on the clicked ground: the hearth
     * centre for Nigredo, the open middle of the salt ring from Albedo on, which takes any block.
     */
    private void athanor(String stage, List<AthanorTiers.BlockAt> extra) {
        AthanorLayout.Schematic schematic = AthanorLayout.schematic(extra);
        DenseMultiblockBuilder builder = new DenseMultiblockBuilder();
        for (List<String> layer : schematic.layers()) {
            builder.layer(layer.toArray(String[]::new));
        }
        schematic.mapping().forEach((symbol, entry) -> {
            int properties = entry.indexOf('[');
            if (entry.equals(AthanorLayout.ANY)) {
                builder.any(symbol);
            } else if (properties >= 0) {
                Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(entry.substring(0, properties)));
                builder.blockstateproperty(symbol, () -> block, entry.substring(properties));
            } else {
                builder.block(symbol, () -> BuiltInRegistries.BLOCK.getValue(Identifier.parse(entry)));
            }
        });
        add(athanor(stage), builder.build(false, 1));
    }
}
