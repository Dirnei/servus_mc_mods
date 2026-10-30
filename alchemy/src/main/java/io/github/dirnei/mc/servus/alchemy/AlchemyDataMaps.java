package io.github.dirnei.mc.servus.alchemy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/** The data maps of Servus: Alchemy. */
@EventBusSubscriber(modid = ServusAlchemyIds.MOD_ID)
public final class AlchemyDataMaps {

    /** How long one bedding item keeps a Venter Equinus warm, in work ticks. */
    public record Bedding(int warmth) {

        public static final Codec<Bedding> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("warmth").forGetter(Bedding::warmth)
        ).apply(instance, Bedding::new));
    }

    /** Which items warm the Venter Equinus' bed, from {@code data/<ns>/data_maps/item/bedding.json}. Synced to clients. */
    public static final DataMapType<Item, Bedding> BEDDING = DataMapType
            .builder(Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "bedding"), Registries.ITEM, Bedding.CODEC)
            .synced(Bedding.CODEC, false)
            .build();

    private AlchemyDataMaps() {
    }

    /** The work ticks one {@code stack} keeps the bed warm, 0 when it is no bedding. */
    public static int warmth(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        Bedding bedding = stack.typeHolder().getData(BEDDING);
        return bedding == null ? 0 : bedding.warmth();
    }

    @SubscribeEvent
    public static void onRegisterDataMaps(RegisterDataMapTypesEvent event) {
        event.register(BEDDING);
    }
}
