package io.github.dirnei.mc.servus.logistics.client;

import io.github.dirnei.mc.servus.logistics.ServusLogisticsIds;
import io.github.dirnei.mc.servus.logistics.duct.DuctParts;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsBlockEntities;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsMenus;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client only mod bus hooks. */
@EventBusSubscriber(modid = ServusLogisticsIds.MOD_ID, value = Dist.CLIENT)
public final class ServusLogisticsClient {

    private ServusLogisticsClient() {
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ServusLogisticsMenus.UTILITY_DUCT.get(), UtilityDuctScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ServusLogisticsBlockEntities.FLUID_TANK.get(), FluidTankRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(new DuctBlockColor(), ServusLogisticsBlocks.UTILITY_DUCT.get());
    }

    /** The duct part model names: three centres and an arm per direction and line set. */
    private static List<String> partNames() {
        List<String> names = new ArrayList<>();
        for (int lines : new int[] {DuctParts.ITEM, DuctParts.ENERGY, DuctParts.BOTH}) {
            names.add(DuctParts.centerName(lines));
            for (Direction direction : Direction.values()) {
                names.add(DuctParts.armName(direction, lines));
            }
        }
        names.add(DuctParts.centerName(DuctParts.BOTH, Direction.Axis.X));
        names.add(DuctParts.centerName(DuctParts.BOTH, Direction.Axis.Y));
        names.add(DuctParts.centerName(DuctParts.BOTH, null));
        return names;
    }

    /** Loads the part models the dynamic duct model is composed from. */
    @SubscribeEvent
    public static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        for (String name : partNames()) {
            event.register(ModelResourceLocation.standalone(DuctParts.id(name)));
        }
    }

    /** Wraps every duct block state model so it composes parts and facades can take over. */
    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        BakedModel missing = models.get(ModelBakery.MISSING_MODEL_VARIANT);
        Map<String, BakedModel> parts = new HashMap<>();
        for (String name : partNames()) {
            BakedModel part = models.get(ModelResourceLocation.standalone(DuctParts.id(name)));
            if (part != null && part != missing) {
                parts.put(name, part);
            }
        }
        for (Map.Entry<ModelResourceLocation, BakedModel> entry : models.entrySet()) {
            ModelResourceLocation key = entry.getKey();
            if (key.id().getNamespace().equals(ServusLogisticsIds.MOD_ID)
                    && key.id().getPath().equals("utility_duct")
                    && !key.variant().equals("inventory")
                    && !(entry.getValue() instanceof UtilityDuctModel)) {
                entry.setValue(new UtilityDuctModel(entry.getValue(), parts));
            }
        }
    }
}
