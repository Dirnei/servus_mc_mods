package io.github.dirnei.mc.servus.manufacturing.deposit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * What a deposit block yields per extraction. Loaded from
 * {@code data/<namespace>/deposit/<name>.json}.
 *
 * @param block the deposit block
 * @param yield the stack one extraction produces
 */
public record DepositDefinition(Block block, ItemStack yield) {

    public static final Codec<DepositDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block").forGetter(DepositDefinition::block),
            ItemStack.CODEC.fieldOf("yield").forGetter(DepositDefinition::yield)
    ).apply(instance, DepositDefinition::new));

    /** A fresh copy of the yield, safe to hand out. */
    public ItemStack copyYield() {
        return yield.copy();
    }
}
