package io.github.dirnei.mc.servus.foundation.wood.client;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.wood.Wood;
import java.util.Map;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Wraps the block state and item models of every registered wood block in a {@link WoodModel}. */
@EventBusSubscriber(modid = ServusFoundationIds.MOD_ID, value = Dist.CLIENT)
public final class WoodModels {

    private WoodModels() {
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        for (Map.Entry<ModelResourceLocation, BakedModel> entry : event.getModels().entrySet()) {
            if (Wood.isWoodBlock(entry.getKey().id()) && !(entry.getValue() instanceof WoodModel)) {
                entry.setValue(new WoodModel(entry.getValue()));
            }
        }
    }
}
