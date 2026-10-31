package io.github.dirnei.mc.servus.shuttles.integration.jade;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.locker.BoxInfo;
import io.github.dirnei.mc.servus.shuttles.locker.LockerGlance;
import io.github.dirnei.mc.servus.shuttles.locker.LockerLayout;
import io.github.dirnei.mc.servus.shuttles.locker.ParcelLockerBlock;
import io.github.dirnei.mc.servus.shuttles.locker.ParcelLockerBlockEntity;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.JadeIds;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.JadeUI;

/**
 * Shows the Parcel Locker by its label, the looked at box with who ordered it
 * and how far the order is, and from the screen or dock every taken box. The
 * viewer's own box lists its items. Only Jade loads this class.
 */
@WailaPlugin(ServusShuttlesIds.MOD_ID)
public final class ServusShuttlesJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(LockerProvider.INSTANCE, ParcelLockerBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(new ClientProvider(LockerProvider.UID, LockerProvider.INSTANCE::appendTooltip), ParcelLockerBlock.class);
    }

    /** Draws the tooltip of a data provider on the client, under the same uid. */
    private record ClientProvider(Identifier uid, Tooltip tooltip) implements IBlockComponentProvider {

        @Override
        public void appendTooltip(ITooltip lines, BlockAccessor accessor, IPluginConfig config) {
            tooltip.append(lines, accessor, config);
        }

        @Override
        public Identifier getUid() {
            return uid;
        }
    }

    @FunctionalInterface
    private interface Tooltip {
        void append(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config);
    }

    private enum LockerProvider implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        private static final Identifier UID = Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "parcel_locker");
        private static final String DATA = "ParcelLocker";

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            ParcelLockerBlockEntity locker = ParcelLockerBlock.locker(accessor.getLevel(), accessor.getPosition());
            if (locker == null) {
                return;
            }
            double hitY = (accessor.getHitResult().getLocation().y - accessor.getPosition().getY()) * LockerLayout.BLOCK;
            List<LockerGlance.Line> lines = LockerGlance.lines(locker, accessor.getBlockState().getValue(ParcelLockerBlock.PART), hitY,
                    accessor.getPlayer().getUUID());
            data.put(DATA, LockerGlance.write(lines, accessor.getLevel().registryAccess()));
        }

        void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            ParcelLockerBlockEntity locker = ParcelLockerBlock.locker(accessor.getLevel(), accessor.getPosition());
            if (locker != null) {
                tooltip.replace(JadeIds.CORE_OBJECT_NAME, IThemeHelper.get().title(locker.label()));
            }
            CompoundTag data = accessor.getServerData();
            if (!data.contains(DATA)) {
                return;
            }
            List<LockerGlance.Line> lines = LockerGlance.read(data.getCompoundOrEmpty(DATA), accessor.getLevel().registryAccess());
            if (lines.isEmpty()) {
                tooltip.add(Component.translatable("jade.servus_shuttles.parcel_locker.all_free"));
            }
            for (LockerGlance.Line line : lines) {
                tooltip.add(BoxInfo.describe(line.box(), line.owner(), line.state()));
                for (ItemStack stack : line.items()) {
                    tooltip.add(JadeUI.smallItem(stack.copyWithCount(1)));
                    tooltip.append(Component.translatable("jade.servus_shuttles.parcel_locker.item", stack.getCount(), stack.getHoverName()));
                }
            }
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
