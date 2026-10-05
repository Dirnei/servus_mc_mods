package io.github.dirnei.mc.servus.logistics.client;

import io.github.dirnei.mc.servus.logistics.ServusLogisticsIds;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsMenus;
import java.util.Map;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
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

    /** Wraps every duct block state model so facades can take over. */
    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        for (Map.Entry<ModelResourceLocation, BakedModel> entry : models.entrySet()) {
            ModelResourceLocation key = entry.getKey();
            if (key.id().getNamespace().equals(ServusLogisticsIds.MOD_ID)
                    && key.id().getPath().equals("utility_duct")
                    && !key.variant().equals("inventory")
                    && !(entry.getValue() instanceof UtilityDuctModel)) {
                entry.setValue(new UtilityDuctModel(entry.getValue()));
            }
        }
    }
}
