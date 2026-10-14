package io.github.dirnei.mc.servus.alchemy.client.sight;

import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.sight.ElementalSight;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/**
 * What only Elemental Sight shows. Renderers draw after translucent blocks, and only while the local player
 * has the sight; without it they are never called.
 */
@EventBusSubscriber(modid = ServusAlchemyIds.MOD_ID, value = Dist.CLIENT)
public final class RevealRenderers {

    /** Draws something hidden in the world. */
    @FunctionalInterface
    public interface RevealRenderer {

        void render(SubmitCustomGeometryEvent event);
    }

    private static final List<RevealRenderer> RENDERERS = new CopyOnWriteArrayList<>();

    private RevealRenderers() {
    }

    public static void add(RevealRenderer renderer) {
        RENDERERS.add(renderer);
    }

    @SubscribeEvent
    public static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        if (RENDERERS.isEmpty()) {
            return;
        }
        if (!ElementalSight.active(Minecraft.getInstance().player)) {
            return;
        }
        for (RevealRenderer renderer : RENDERERS) {
            renderer.render(event);
        }
    }
}
