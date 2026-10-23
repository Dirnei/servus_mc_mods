package io.github.dirnei.mc.servus.alchemy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTiers.BlockAt;
import io.github.dirnei.mc.servus.alchemy.coagulator.VatBlock;
import io.github.dirnei.mc.servus.alchemy.coagulator.VatCrystal;
import io.github.dirnei.mc.servus.foundation.client.SubmitBuffers;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

/** How a block a ritual turns looks while it grows, by the id of the block it becomes. */
public final class RitualLooks {

    /** Draws a block growing into {@code to}; the pose starts at its block corner. */
    @FunctionalInterface
    public interface Look {

        /**
         * @param pos    the block's position in the world
         * @param fill   how far the stream's power has filled it, 0 to 1
         * @param growth how far it has grown, 0 to 1
         */
        void draw(PoseStack poseStack, SubmitBuffers buffers, BlockPos pos, BlockAt to, float fill, float growth);
    }

    private static final Map<String, Look> LOOKS = new HashMap<>();

    static {
        register("servus_alchemy:principle_vat", RitualLooks::vat);
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

    /** A vat fills with the crystal's liquid and the cluster grows out of it. */
    private static void vat(PoseStack poseStack, SubmitBuffers buffers, BlockPos pos, BlockAt to, float fill, float growth) {
        String name = to.properties().getOrDefault(VatBlock.CRYSTAL.getName(), VatCrystal.CITRINE.getSerializedName());
        VatCrystal crystal = Arrays.stream(VatCrystal.values()).filter(each -> each.getSerializedName().equals(name))
                .findFirst().orElse(VatCrystal.CITRINE);
        CrystalDrawing.liquid(poseStack, buffers, fill, crystal.color());
        CrystalDrawing.cluster(poseStack, buffers, pos, growth, crystal.body());
    }
}
