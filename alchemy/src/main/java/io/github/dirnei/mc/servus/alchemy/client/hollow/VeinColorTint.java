package io.github.dirnei.mc.servus.alchemy.client.hollow;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.hollow.ClientVeins;
import io.github.dirnei.mc.servus.alchemy.hollow.VeinData;
import io.github.dirnei.mc.servus.alchemy.hollow.VeinSeedItem;
import io.github.dirnei.mc.servus.alchemy.hollow.VeinSyncPayload.ShownVein;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Item tint {@code servus_alchemy:vein_color}: the colour of the vein a Seed names, else {@code default}. */
public record VeinColorTint(int defaultColor) implements ItemTintSource {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "vein_color");

    public static final MapCodec<VeinColorTint> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ExtraCodecs.RGB_COLOR_CODEC.optionalFieldOf("default", VeinData.DEFAULT_COLOR).forGetter(VeinColorTint::defaultColor)
    ).apply(instance, VeinColorTint::new));

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        return ARGB.opaque(VeinSeedItem.vein(stack).flatMap(ClientVeins::get).map(ShownVein::color).orElse(defaultColor));
    }

    @Override
    public MapCodec<VeinColorTint> type() {
        return MAP_CODEC;
    }
}
