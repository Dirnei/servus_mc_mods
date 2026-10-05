package io.github.dirnei.mc.servus.logistics.client;

import io.github.dirnei.mc.servus.logistics.ServusLogisticsIds;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
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
}
