package io.github.dirnei.mc.servus.alchemy.hollow;

import io.github.dirnei.mc.servus.alchemy.datagen.gamedata.GameDataExportProvider;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyComponents;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import java.util.Optional;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * A Seed of a metal: one item for every vein, naming its vein in the {@code servus_alchemy:vein} component. It is
 * named after that vein ("Seed of Gold"); without a vein, or with one that is not loaded, it is an unknown seed.
 */
public class VeinSeedItem extends Item {

    private static final String KEY = "item.servus_alchemy.seed";

    public VeinSeedItem(Properties properties) {
        super(properties);
    }

    /** A Seed of {@code vein}. */
    public static ItemStack of(Identifier vein) {
        return template(vein).create();
    }

    /** A Seed of {@code vein} as a recipe result. */
    public static ItemStackTemplate template(Identifier vein) {
        return new ItemStackTemplate(ServusAlchemyItems.SEED.get(),
                DataComponentPatch.builder().set(ServusAlchemyComponents.VEIN.get(), vein).build());
    }

    /** The vein {@code stack} plants, empty when it is no Seed or names no vein. */
    public static Optional<Identifier> vein(ItemStack stack) {
        return stack.getItem() instanceof VeinSeedItem ? Optional.ofNullable(stack.get(ServusAlchemyComponents.VEIN.get())) : Optional.empty();
    }

    /** The vein's name from {@code vein.<namespace>.<path>}, else its path in title case. */
    public static Component veinName(Identifier vein) {
        return Component.translatableWithFallback("vein." + vein.getNamespace() + "." + vein.getPath(),
                GameDataExportProvider.titleCase(vein.getPath()));
    }

    @Override
    public Component getName(ItemStack stack) {
        return vein(stack).filter(VeinSeedItem::loaded)
                .<Component>map(vein -> Component.translatable(KEY + ".of", veinName(vein)))
                .orElseGet(() -> Component.translatable(KEY + ".unknown"));
    }

    /** Whether the server loaded {@code vein}: its data on the server, what it sent on a client. */
    public static boolean loaded(Identifier vein) {
        return VeinData.INSTANCE.get(vein).isPresent() || ClientVeins.get(vein).isPresent();
    }
}
