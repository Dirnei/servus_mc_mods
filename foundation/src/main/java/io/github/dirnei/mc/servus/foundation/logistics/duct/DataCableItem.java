package io.github.dirnei.mc.servus.foundation.logistics.duct;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import io.github.dirnei.mc.servus.foundation.data.CableTier;
import io.github.dirnei.mc.servus.foundation.data.CableTiersManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * A tier of the data line: places or adds the data line like any line item
 * and sets the duct's tier to this item. Its stats (bandwidth, simplex or
 * duplex) come from {@code cable_tiers.json} keyed by this item's id; the duct
 * body skin and the ring colour show the tier on the duct (Data Cable white,
 * Duplex gray, Fast black). Other mods may register more tiers.
 */
public class DataCableItem extends LineItem {

    private final int ringColour;
    private final String skin;

    /**
     * @param ringColour the RGB tint of the data line's rings
     * @param skin the suffix of the duct body textures of this tier ({@code duct_data_<skin>}),
     *             empty for the plain {@code duct_data} textures
     */
    public DataCableItem(Block block, int ringColour, String skin, Properties properties) {
        super(block, Line.DATA, properties);
        this.ringColour = ringColour;
        this.skin = skin;
    }

    /** The tier id: this item's registry id. */
    public Identifier tierId() {
        return BuiltInRegistries.ITEM.getKey(this);
    }

    /** The RGB colour of the data line's rings on a duct of this tier. */
    public int ringColour() {
        return ringColour;
    }

    /**
     * On a duct whose data line has a lower tier, replaces the tier in place
     * (sides and connections stay), uses up one cable and drops the old one.
     * A lower or equal tier, or a sneak click, behaves like any line item.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        boolean sneaking = player != null && player.isSecondaryUseActive();
        if (!sneaking && level.getBlockEntity(pos) instanceof UtilityDuctBlockEntity duct && duct.has(Line.DATA)
                && higherThan(duct.dataTierId())) {
            if (!level.isClientSide()) {
                Item old = duct.lineItem(Line.DATA);
                duct.setDataTier(tierId());
                if (player == null || !player.getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }
                Block.popResource(level, pos, new ItemStack(old));
            }
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }

    /** The texture suffix of the duct body in this tier, empty for the base textures. */
    public String skin() {
        return skin;
    }

    /** Whether this tier carries more than the given one. */
    public boolean higherThan(Identifier other) {
        CableTiersManager tiers = CableTiersManager.INSTANCE;
        return tiers.tier(tierId()).capacity() > tiers.tier(other).capacity();
    }

    /** Adds the data line in this tier. */
    @Override
    protected boolean addTo(UtilityDuctBlockEntity duct) {
        return duct.addDataLine(tierId());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        CableTier tier = CableTiersManager.INSTANCE.tier(tierId());
        tooltip.accept(Component.translatable(tier.duplex() ? "tooltip.servus_foundation.data_cable.duplex"
                : "tooltip.servus_foundation.data_cable.simplex", (int) Math.round(tier.itemsPerSecond())));
    }
}
