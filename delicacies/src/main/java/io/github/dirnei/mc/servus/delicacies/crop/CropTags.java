package io.github.dirnei.mc.servus.delicacies.crop;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** The common {@code c:} item tags of the Delicacies crops. */
public final class CropTags {

    private static final String COMMON = "c";

    private CropTags() {
    }

    /** {@code c:crops/<crop>}, holding the crop's harvest. */
    public static TagKey<Item> crop(CropKind crop) {
        return common(crop.cropTagPath());
    }

    /** {@code c:seeds/<crop>}, holding the crop's seed item when it has one. */
    public static TagKey<Item> seed(CropKind crop) {
        return common(crop.seedTagPath());
    }

    private static TagKey<Item> common(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(COMMON, path));
    }
}
