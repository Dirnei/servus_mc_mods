package io.github.dirnei.mc.servus.foundation.logistics.duct;

import io.github.dirnei.mc.servus.foundation.data.CableTier;
import io.github.dirnei.mc.servus.foundation.data.CableTiersManager;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/**
 * A tier of the data line: places or adds the data line like any line item
 * and sets the duct's tier to this item. Its stats (bandwidth, simplex or
 * duplex) come from {@code cable_tiers.json} keyed by this item's id; the ring
 * colour shows the tier on the duct. Other mods may register more tiers.
 */
public class DataCableItem extends LineItem {

    private final int ringColour;

    public DataCableItem(Block block, int ringColour, Properties properties) {
        super(block, Line.DATA, properties);
        this.ringColour = ringColour;
    }

    /** The tier id: this item's registry id. */
    public ResourceLocation tierId() {
        return BuiltInRegistries.ITEM.getKey(this);
    }

    /** The RGB colour of the data line's rings on a duct of this tier. */
    public int ringColour() {
        return ringColour;
    }

    /** Adds the data line in this tier. */
    @Override
    protected boolean addTo(UtilityDuctBlockEntity duct) {
        return duct.addDataLine(tierId());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        CableTier tier = CableTiersManager.INSTANCE.tier(tierId());
        tooltip.add(Component.translatable(tier.duplex() ? "tooltip.servus_foundation.data_cable.duplex"
                : "tooltip.servus_foundation.data_cable.simplex", (int) Math.round(tier.itemsPerSecond())));
    }
}
