package io.github.dirnei.mc.servus.shuttles.fluidrack;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/**
 * The chat report of the Data Potato on a Fluid Rack: the zone, bays, fluid
 * and capacity of its whole section and whether it is docked. Server thread only.
 */
public final class FluidRackReport {

    private static final String KEY = "message.servus_shuttles.potato.fluid_rack_";

    private FluidRackReport() {
    }

    public static List<Component> of(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof FluidRackBlockEntity rack)) {
            return List.of();
        }
        int bays = FluidRackBlock.section(level, pos).size();
        FluidRackSectionHandler.Content content = rack.sectionHandler().content();
        List<Component> report = new ArrayList<>();
        report.add(rack.zone().isEmpty() ? Component.translatable(KEY + "no_zone") : Component.translatable(KEY + "zone", rack.zone()));
        report.add(Component.translatable(KEY + "bays", bays));
        report.add(content.fluid().isEmpty()
                ? Component.translatable(KEY + "empty", content.capacity())
                : Component.translatable(KEY + "fluid", content.fluid().getHoverName(), content.fluid().getAmount(), content.capacity()));
        report.add(Component.translatable(rack.standalone() ? KEY + "standalone" : KEY + "docked"));
        return report;
    }
}
