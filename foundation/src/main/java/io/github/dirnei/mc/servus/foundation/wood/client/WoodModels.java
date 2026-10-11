package io.github.dirnei.mc.servus.foundation.wood.client;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.wood.Wood;
import java.util.Map;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Wraps the block state and item models of every registered wood block in a {@link WoodModel}. */
@EventBusSubscriber(modid = ServusFoundationIds.MOD_ID, value = Dist.CLIENT)
public final class WoodModels {

    private WoodModels() {
    }

    /** Wraps the block state models of every registered wood block. */
    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<BlockState, BlockStateModel> models = event.getBakingResult().blockStateModels();
        for (Map.Entry<BlockState, BlockStateModel> entry : models.entrySet()) {
            Identifier blockId = BuiltInRegistries.BLOCK.getKey(entry.getKey().getBlock());
            if (Wood.isWoodBlock(blockId) && !(entry.getValue() instanceof WoodModel)) {
                entry.setValue(new WoodModel(entry.getValue()));
            }
        }
    }

    /** The item model type wood block items name in their item model definitions. */
    @SubscribeEvent
    public static void onRegisterItemModels(RegisterItemModelsEvent event) {
        event.register(WoodSkin.WOOD.itemModelType(), WoodSkin.WOOD.itemCodec());
    }
}
