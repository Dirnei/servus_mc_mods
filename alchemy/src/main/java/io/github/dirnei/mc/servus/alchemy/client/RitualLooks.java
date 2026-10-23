package io.github.dirnei.mc.servus.alchemy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTiers.BlockAt;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.RitualTimeline;
import io.github.dirnei.mc.servus.alchemy.athanor.ritual.RubedoObeliskBlock;
import io.github.dirnei.mc.servus.alchemy.coagulator.VatBlock;
import io.github.dirnei.mc.servus.alchemy.coagulator.VatCrystal;
import io.github.dirnei.mc.servus.foundation.client.SubmitBuffers;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jspecify.annotations.Nullable;

/** How a block a ritual turns looks while it grows, by the id of the block it becomes. */
public final class RitualLooks {

    /**
     * Where a running ritual stands for one turned block.
     *
     * @param t     ticks since the offering
     * @param index the turn's place in the ritual
     * @param count how many turns the ritual has
     * @param time  the game time, for what moves on with it
     */
    public record Moment(RitualTimeline timeline, float t, int index, int count, float time) {

        /** How far the stream's power has filled the block, 0 to 1. */
        public float fill() {
            return timeline.fill(t, index);
        }

        /** How far the block has grown, 0 to 1. */
        public float growth() {
            return timeline.growth(t, index, count);
        }
    }

    /** Draws a block growing into {@code to}; the pose starts at its block corner. */
    @FunctionalInterface
    public interface Look {

        /** @param pos the block's position in the world */
        void draw(PoseStack poseStack, SubmitBuffers buffers, Level level, BlockPos pos, BlockAt to, Moment moment);

        /** How high above the block's floor its stream lands, in blocks. */
        default double landing() {
            return 0.75;
        }
    }

    private static final Map<String, Look> LOOKS = new HashMap<>();

    static {
        register("servus_alchemy:principle_vat", RitualLooks::vat);
        register("servus_alchemy:rubedo_obelisk", new Look() {
            @Override
            public void draw(PoseStack poseStack, SubmitBuffers buffers, Level level, BlockPos pos, BlockAt to, Moment moment) {
                obelisk(poseStack, buffers, level, pos, to, moment);
            }

            @Override
            public double landing() {
                return 1.05;
            }
        });
    }

    private RitualLooks() {
    }

    public static void register(String block, Look look) {
        LOOKS.put(block, look);
    }

    /** The look of a block growing into {@code block}, or null when it has none. */
    public static @Nullable Look of(String block) {
        return LOOKS.get(block);
    }

    private static VatCrystal crystal(String name) {
        return Arrays.stream(VatCrystal.values()).filter(each -> each.getSerializedName().equals(name))
                .findFirst().orElse(VatCrystal.CITRINE);
    }

    /**
     * A plain vat fills with the crystal's liquid and the cluster grows out of it. A vat already grown turns its crystal's
     * colour into the new one, after its crystal answered the rising offering with a pulse; its own renderer draws it.
     */
    private static void vat(PoseStack poseStack, SubmitBuffers buffers, Level level, BlockPos pos, BlockAt to, Moment moment) {
        VatCrystal target = crystal(to.state().getOrDefault(VatBlock.CRYSTAL.getName(), VatCrystal.CITRINE.getSerializedName()));
        BlockState state = level.getBlockState(pos);
        VatCrystal grown = state.hasProperty(VatBlock.CRYSTAL) ? state.getValue(VatBlock.CRYSTAL) : VatCrystal.NONE;
        if (grown.grown()) {
            float shift = moment.timeline().recolor(moment.t());
            float pulse = 0.35F * moment.timeline().answer(moment.t());
            int body = RitualTimeline.mix(RitualTimeline.mix(grown.body(), target.body(), shift), 0xFFFFFF, pulse);
            int color = RitualTimeline.mix(RitualTimeline.mix(grown.color(), target.color(), shift), 0xFFFFFF, pulse);
            VatTints.put(pos, body, color, moment.time());
            return;
        }
        CrystalDrawing.liquid(poseStack, buffers, moment.fill(), target.color());
        CrystalDrawing.cluster(poseStack, buffers, pos, moment.growth(), target.body());
    }

    /** The Obsidian cracks with glowing veins and the obelisk rises out of it; the upper half has no look of its own. */
    private static void obelisk(PoseStack poseStack, SubmitBuffers buffers, Level level, BlockPos pos, BlockAt to, Moment moment) {
        if (!DoubleBlockHalf.LOWER.getSerializedName().equals(to.state().get(RubedoObeliskBlock.HALF.getName()))) {
            return;
        }
        float cracks = moment.timeline().cracks(moment.t(), moment.index());
        float glow = cracks * (0.7F + 0.3F * (float) Math.sin(moment.time() * 0.2F + ObeliskDrawing.phase(pos)));
        ObeliskDrawing.cracks(poseStack, buffers, glow, 16, cracks, ObeliskDrawing.openSides(level, pos));
        ObeliskDrawing.rising(poseStack, buffers, moment.timeline().rise(moment.t(), moment.index()), moment.time(),
                ObeliskDrawing.phase(pos));
    }
}
