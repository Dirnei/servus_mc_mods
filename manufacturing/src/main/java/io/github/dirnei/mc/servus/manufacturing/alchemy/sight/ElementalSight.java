package io.github.dirnei.mc.servus.manufacturing.alchemy.sight;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Elemental Sight: the effect that shows what the elementals do inside their element, and the elixir that gives it. */
public final class ElementalSight {

    /** How long one elixir lasts: three minutes. */
    public static final int DURATION = 3600;

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, ServusManufacturingIds.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ServusManufacturingIds.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> EFFECT = EFFECTS.register("elemental_sight",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xA8D8F0) {
            });

    public static final DeferredItem<ElixirOfSightItem> ELIXIR = ITEMS.registerItem("elixir_of_sight", ElixirOfSightItem::new,
            ElixirOfSightItem::properties);

    private ElementalSight() {
    }

    /** Whether the player sees with Elemental Sight right now. */
    public static boolean active(Player player) {
        return player != null && player.hasEffect(EFFECT);
    }

    public static void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}
