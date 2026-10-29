package io.github.dirnei.mc.servus.delicacies.aquarium;

import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/** The fish an aquarium keeps: the bucket that pours one in and the raw fish the catch gives. */
public enum FishSpecies {
    COD(Items.COD_BUCKET, Items.COD),
    SALMON(Items.SALMON_BUCKET, Items.SALMON);

    private final Item bucket;
    private final Item rawFish;

    FishSpecies(Item bucket, Item rawFish) {
        this.bucket = bucket;
        this.rawFish = rawFish;
    }

    public Item bucket() {
        return bucket;
    }

    public Item rawFish() {
        return rawFish;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component displayName() {
        return Component.translatable("aquarium.servus_delicacies.species." + id());
    }

    /** The species whose bucket {@code stack} is, or null. */
    public static @Nullable FishSpecies ofBucket(ItemStack stack) {
        for (FishSpecies species : values()) {
            if (stack.is(species.bucket)) {
                return species;
            }
        }
        return null;
    }

    /** The species with {@code ordinal}, or null for any other number. */
    public static @Nullable FishSpecies byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : null;
    }
}
