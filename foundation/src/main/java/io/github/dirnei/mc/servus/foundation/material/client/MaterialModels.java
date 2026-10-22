package io.github.dirnei.mc.servus.foundation.material.client;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.material.Material;
import io.github.dirnei.mc.servus.foundation.material.MaterialKind;
import java.util.Map;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;

/** Wraps the block state models of every registered material block in a {@link MaterialModel} and registers the item model type. */
@EventBusSubscriber(modid = ServusFoundationIds.MOD_ID, value = Dist.CLIENT)
public final class MaterialModels {

    private MaterialModels() {
    }

    /** Wraps the block state models of every registered material block with the skin of its kind. */
    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<BlockState, BlockStateModel> models = event.getBakingResult().blockStateModels();
        for (Map.Entry<BlockState, BlockStateModel> entry : models.entrySet()) {
            Identifier blockId = BuiltInRegistries.BLOCK.getKey(entry.getKey().getBlock());
            MaterialKind kind = Material.kindOf(blockId);
            if (kind != null && !(entry.getValue() instanceof MaterialModel)) {
                entry.setValue(new MaterialModel(entry.getValue(), kind));
            }
        }
    }

    /** The item model type material block items name in their item model definitions. */
    @SubscribeEvent
    public static void onRegisterItemModels(RegisterItemModelsEvent event) {
        event.register(MaterialSkin.ITEM_MODEL_TYPE, MaterialSkin.KIND_ITEM_CODEC);
    }
}
