package io.github.dirnei.mc.servus.brewery.beer;

import io.github.dirnei.mc.servus.delicacies.food.DelicaciesDataMaps;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;

/**
 * A full half litre glass of beer. It is drunk like a potion and leaves its empty glass (the {@code USE_REMAINDER}
 * the caller sets with {@link Item.Properties#usingConvertsTo}). What it restores comes from the Delicacies food data
 * map; without an entry it cannot be drunk. Each drink posts a {@link BeerDrunkEvent} on the server.
 */
public class BeerItem extends Item {

    public BeerItem(Properties properties) {
        super(properties.component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        FoodProperties food = DelicaciesDataMaps.food(player.getItemInHand(hand));
        if (food == null) {
            return InteractionResult.PASS;
        }
        if (!player.canEat(food.canAlwaysEat())) {
            return InteractionResult.FAIL;
        }
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        FoodProperties food = DelicaciesDataMaps.food(stack);
        Consumable consumable = stack.get(DataComponents.CONSUMABLE);
        if (food != null && consumable != null) {
            food.onConsume(level, entity, stack, consumable);
        }
        if (!level.isClientSide() && entity instanceof Player player) {
            NeoForge.EVENT_BUS.post(new BeerDrunkEvent(player, stack.copy()));
        }
        return super.finishUsingItem(stack, level, entity);
    }
}
