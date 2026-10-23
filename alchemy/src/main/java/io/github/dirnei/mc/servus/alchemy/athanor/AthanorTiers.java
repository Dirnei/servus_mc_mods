package io.github.dirnei.mc.servus.alchemy.athanor;

import io.github.dirnei.mc.servus.alchemy.structure.Facing;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/** Which stage of the Great Work an Athanor's structure stands for. Pure Java. */
public final class AthanorTiers {

    /** Tier 0 needs nothing: the Athanor alone. */
    public static final String NIGREDO = "nigredo";

    /**
     * A block a tier needs, relative to the Athanor.
     *
     * @param block block id, such as {@code servus_alchemy:salt_crystal_block}
     */
    public record BlockAt(int dx, int dy, int dz, String block) {
    }

    /**
     * One tier of the structure.
     *
     * @param name   stage name, such as {@code albedo}
     * @param number 1 and up; higher tiers need the blocks of every lower tier too
     * @param color  RGB of its flame
     * @param blocks what must stand around the Athanor
     */
    public record TierDef(String name, int number, int color, List<BlockAt> blocks) {

        public TierDef {
            if (number < 1) {
                throw new IllegalArgumentException("Tier numbers start at 1: " + name);
            }
            blocks = List.copyOf(blocks);
        }
    }

    private AthanorTiers() {
    }

    /**
     * The block {@code block}, written for an Athanor whose front faces north, turned for one facing {@code facing}:
     * it keeps its place left or right and in front or behind, seen from the Athanor's front.
     */
    public static BlockAt turn(BlockAt block, Facing facing) {
        int right = -block.dx();
        int back = block.dz();
        int rx = facing.rightX();
        int rz = facing.rightZ();
        return new BlockAt(right * rx + back * rz, block.dy(), right * rz - back * rx, block.block());
    }

    /**
     * The highest tier whose blocks, and those of every lower tier, all match around an Athanor facing {@code facing};
     * 0 when none does. {@code blockAt} gets each block already turned with the front.
     */
    public static int tierOf(List<TierDef> tiers, Facing facing, Function<BlockAt, String> blockAt) {
        int reached = 0;
        for (TierDef tier : tiers.stream().sorted(Comparator.comparingInt(TierDef::number)).toList()) {
            boolean complete = tier.blocks().stream().map(block -> turn(block, facing))
                    .allMatch(block -> block.block().equals(blockAt.apply(block)));
            if (!complete) {
                break;
            }
            reached = tier.number();
        }
        return reached;
    }

    /** The name of a tier number, {@link #NIGREDO} for 0 or an unknown number. */
    public static String nameOf(List<TierDef> tiers, int number) {
        return tiers.stream().filter(tier -> tier.number() == number).map(TierDef::name).findFirst().orElse(NIGREDO);
    }
}
