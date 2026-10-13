package io.github.dirnei.mc.servus.foundation.logistics.client;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.adapter.AdapterContent;
import io.github.dirnei.mc.servus.foundation.adapter.AdapterScreen;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctParts;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlockEntities;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsMenus;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

/** Client only mod bus hooks. */
@EventBusSubscriber(modid = ServusFoundationIds.MOD_ID, value = Dist.CLIENT)
public final class ServusLogisticsClient {

    private ServusLogisticsClient() {
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ServusLogisticsMenus.UTILITY_DUCT.get(), UtilityDuctScreen::new);
        event.register(AdapterContent.MENU.get(), AdapterScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ServusLogisticsBlockEntities.FLUID_TANK.get(), FluidTankRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterBlockTints(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(DuctBlockColor.sources(), ServusLogisticsBlocks.UTILITY_DUCT.get());
    }

    /** One standalone model key per duct part, so the parts are loaded and baked with the block models. */
    private static final Map<String, StandaloneModelKey<BlockStateModelPart>> PART_KEYS = new HashMap<>();

    private static StandaloneModelKey<BlockStateModelPart> partKey(String name) {
        return PART_KEYS.computeIfAbsent(name, n -> new StandaloneModelKey<>(() -> DuctParts.id(n).toString()));
    }

    /** Loads the part models the dynamic duct model is composed from. */
    @SubscribeEvent
    public static void onRegisterStandalone(ModelEvent.RegisterStandalone event) {
        for (String name : DuctParts.allNames()) {
            event.register(partKey(name), SimpleUnbakedStandaloneModel.simpleModelWrapper(DuctParts.id(name)));
        }
    }

    /** Wraps every duct block state model so it composes parts and panels can take over. */
    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        ModelBakery.BakingResult result = event.getBakingResult();
        Map<String, BlockStateModelPart> parts = new HashMap<>();
        for (String name : DuctParts.allNames()) {
            BlockStateModelPart part = result.standaloneModels().get(partKey(name));
            if (part != null) {
                parts.put(name, part);
            }
        }
        Map<BlockState, BlockStateModel> models = result.blockStateModels();
        for (BlockState state : ServusLogisticsBlocks.UTILITY_DUCT.get().getStateDefinition().getPossibleStates()) {
            BlockStateModel model = models.get(state);
            if (model != null && !(model instanceof UtilityDuctModel)) {
                models.put(state, new UtilityDuctModel(model, parts));
            }
        }
    }
}
