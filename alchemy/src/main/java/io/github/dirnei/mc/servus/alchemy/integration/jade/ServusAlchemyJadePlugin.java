package io.github.dirnei.mc.servus.alchemy.integration.jade;

import io.github.dirnei.mc.servus.alchemy.CalcinerBlock;
import io.github.dirnei.mc.servus.alchemy.CalcinerBlockEntity;
import io.github.dirnei.mc.servus.alchemy.Principle;
import io.github.dirnei.mc.servus.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.alchemy.Principles;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorBlock;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorBlockEntity;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorCrownBlock;
import io.github.dirnei.mc.servus.alchemy.calciner.CalcinerPartBlock;
import io.github.dirnei.mc.servus.alchemy.hollow.GnomeHollowBlock;
import io.github.dirnei.mc.servus.alchemy.hollow.GnomeHollowBlockEntity;
import io.github.dirnei.mc.servus.alchemy.sight.ElementalSight;
import io.github.dirnei.mc.servus.alchemy.calciner.CalcinerPorts;
import io.github.dirnei.mc.servus.alchemy.calciner.CalcinerShellBlock;
import io.github.dirnei.mc.servus.alchemy.purifier.PurifierBlock;
import io.github.dirnei.mc.servus.alchemy.purifier.PurifierBlockEntity;
import io.github.dirnei.mc.servus.alchemy.purifier.PurifierSideBlock;
import io.github.dirnei.mc.servus.alchemy.purifier.PurifierStructure;
import io.github.dirnei.mc.servus.alchemy.structure.CopperDuctBlock;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Shows what the alchemy blocks hold: a formed Calciner from any of its blocks (matter, fuel,
 * progress, the three principles) and the Purifier (Ash, water, Salt, progress). A Gnome Hollow shows as Stone to a viewer
 * without Elemental Sight and as itself, with its vein and store, to one with it. Only Jade loads this class.
 */
@WailaPlugin(ServusAlchemyIds.MOD_ID)
public final class ServusAlchemyJadePlugin implements IWailaPlugin {

    private static final List<Class<? extends Block>> BLOCKS = List.of(CalcinerBlock.class, CalcinerPartBlock.class, CalcinerShellBlock.class,
            CopperDuctBlock.class);

    @Override
    public void register(IWailaCommonRegistration registration) {
        BLOCKS.forEach(block -> registration.registerBlockDataProvider(CalcinerProvider.INSTANCE, block));
        registration.registerBlockDataProvider(PurifierProvider.INSTANCE, PurifierBlock.class);
        registration.registerBlockDataProvider(PurifierProvider.INSTANCE, PurifierSideBlock.class);
        registration.registerBlockDataProvider(AthanorProvider.INSTANCE, AthanorBlock.class);
        registration.registerBlockDataProvider(HollowProvider.INSTANCE, GnomeHollowBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        // Jade draws tooltips in client providers apart from the data providers; each pair shares its uid.
        IBlockComponentProvider calciner = new ClientProvider(CalcinerProvider.UID, CalcinerProvider.INSTANCE::appendTooltip);
        IBlockComponentProvider purifier = new ClientProvider(PurifierProvider.UID, PurifierProvider.INSTANCE::appendTooltip);
        BLOCKS.forEach(block -> registration.registerBlockComponent(calciner, block));
        registration.registerBlockComponent(purifier, PurifierBlock.class);
        registration.registerBlockComponent(purifier, PurifierSideBlock.class);
        registration.registerBlockComponent(new ClientProvider(AthanorProvider.UID, AthanorProvider.INSTANCE::appendTooltip), AthanorBlock.class);
        registration.registerBlockComponent(new ClientProvider(HollowProvider.UID, HollowProvider.INSTANCE::appendTooltip), GnomeHollowBlock.class);
        registration.addRayTraceCallback((hit, accessor, original) -> {
            if (accessor instanceof BlockAccessor block && block.getBlock() instanceof GnomeHollowBlock && !ElementalSight.active(block.getPlayer())) {
                return registration.blockAccessor().from(block).blockState(Blocks.STONE.defaultBlockState()).blockEntity(() -> null).build();
            }
            return accessor;
        });
        // The space above a formed Athanor, where its item lies, shows the Athanor.
        registration.addRayTraceCallback((hit, accessor, original) -> {
            if (accessor instanceof BlockAccessor block && block.getBlock() instanceof AthanorCrownBlock) {
                BlockPos below = block.getPosition().below();
                return registration.blockAccessor().from(block).blockState(block.getLevel().getBlockState(below))
                        .blockEntity(block.getLevel().getBlockEntity(below)).hit(block.getHitResult().withPosition(below)).build();
            }
            return accessor;
        });
    }

    private static CompoundTag saveStack(BlockAccessor accessor, ItemStack stack) {
        return ValueIo.saveStack(accessor.getLevel().registryAccess(), stack);
    }

    private static ItemStack parseStack(BlockAccessor accessor, CompoundTag tag) {
        return ValueIo.parseStack(accessor.getLevel().registryAccess(), tag);
    }

    private static Component stackText(ItemStack stack) {
        return stack.isEmpty() ? Component.translatable("jade.servus_alchemy.calciner.empty")
                : Component.translatable("jade.servus_alchemy.calciner.stack", stack.getCount(), stack.getHoverName());
    }

    /** A factor as "2" or "1.5". */
    private static String factorText(double factor) {
        return factor == Math.rint(factor) ? Long.toString(Math.round(factor)) : String.format(Locale.ROOT, "%.1f", factor);
    }

    /** Game ticks as minutes and seconds, "8:00". */
    private static String clockText(int ticks) {
        int seconds = ticks / 20;
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    private static Component principleName(Principle principle) {
        return new FluidStack(PrincipleFluids.fluid(principle), 1).getHoverName();
    }

    /** Draws the tooltip of one data provider on the client. */
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

    /** The Athanor has no GUI: its stage, its work and what its tanks hold. */
    private enum AthanorProvider implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        private static final Identifier UID = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "athanor");

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof AthanorBlockEntity athanor)) {
                return;
            }
            CompoundTag tag = new CompoundTag();
            tag.putString("Tier", athanor.tierName());
            tag.putBoolean("Formed", athanor.formed());
            tag.put("Input", saveStack(accessor, athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_INPUT)));
            tag.put("Output", saveStack(accessor, athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_OUTPUT)));
            tag.putInt("Progress", athanor.progressPercent());
            Principles held = athanor.principles();
            for (Principle principle : Principle.values()) {
                tag.putInt(principle.id(), held.get(principle));
            }
            data.put("Athanor", tag);
        }

        /** The stage of a tier name in words; a datapack tier without a translation shows its file name. */
        private static Component stageName(String tier) {
            return Component.translatableWithFallback("gui.servus_alchemy.athanor.stage." + tier, tier);
        }

        void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains("Athanor")) {
                return;
            }
            CompoundTag tag = data.getCompoundOrEmpty("Athanor");
            tooltip.add(Component.translatable(tag.getBooleanOr("Formed", false) ? "jade.servus_alchemy.athanor.stage"
                    : "jade.servus_alchemy.athanor.unformed", stageName(tag.getStringOr("Tier", ""))));
            ItemStack input = parseStack(accessor, tag.getCompoundOrEmpty("Input"));
            ItemStack output = parseStack(accessor, tag.getCompoundOrEmpty("Output"));
            tooltip.add(Component.translatable("jade.servus_alchemy.athanor.input", stackText(input)));
            tooltip.add(Component.translatable("jade.servus_alchemy.athanor.output", stackText(output)));
            tooltip.add(Component.translatable("jade.servus_alchemy.calciner.progress", tag.getIntOr("Progress", 0)));
            for (Principle principle : Principle.values()) {
                tooltip.add(Component.translatable("jade.servus_alchemy.athanor.principle", tag.getIntOr(principle.id(), 0),
                        principleName(principle)));
            }
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }

    /** The Gnome Hollow under Elemental Sight: its vein, ripeness or gnome, its store and its principles. */
    private enum HollowProvider implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        private static final Identifier UID = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "gnome_hollow");

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof GnomeHollowBlockEntity hollow) || !ElementalSight.active(accessor.getPlayer())) {
                return;
            }
            CompoundTag tag = new CompoundTag();
            hollow.veinDef().ifPresent(vein -> {
                tag.put("Yield", saveStack(accessor, vein.yield()));
                tag.putInt("Steps", vein.growth().steps());
            });
            tag.putInt("Ripeness", hollow.growth().ripeness);
            tag.putBoolean("Bound", hollow.growth().bound);
            tag.putInt("Stored", hollow.store().count());
            tag.putInt("Capacity", hollow.store().capacity());
            if (hollow.boostTicksLeft() > 0) {
                tag.putDouble("GiftFactor", hollow.boostFactor());
                tag.putInt("GiftTicks", hollow.boostTicksLeft());
            }
            tag.putInt("Tank", hollow.tanks().capacity());
            for (Principle principle : Principle.values()) {
                tag.putInt(principle.id(), hollow.tanks().amount(principle));
            }
            data.put("Hollow", tag);
        }

        void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains("Hollow")) {
                return;
            }
            CompoundTag tag = data.getCompoundOrEmpty("Hollow");
            ItemStack yield = parseStack(accessor, tag.getCompoundOrEmpty("Yield"));
            if (yield.isEmpty()) {
                tooltip.add(Component.translatable("jade.servus_alchemy.gnome_hollow.no_vein"));
            } else {
                tooltip.add(Component.translatable("jade.servus_alchemy.gnome_hollow.vein", yield.getHoverName()));
                int steps = tag.getIntOr("Steps", 0);
                int ripeness = tag.getIntOr("Ripeness", 0);
                if (tag.getBooleanOr("Bound", false)) {
                    tooltip.add(Component.translatable("jade.servus_alchemy.gnome_hollow.gnome"));
                } else if (ripeness >= steps) {
                    tooltip.add(Component.translatable("jade.servus_alchemy.gnome_hollow.ripe"));
                } else {
                    tooltip.add(Component.translatable("jade.servus_alchemy.gnome_hollow.ripeness", ripeness, steps));
                }
            }
            int giftTicks = tag.getIntOr("GiftTicks", 0);
            if (giftTicks > 0) {
                tooltip.add(Component.translatable("jade.servus_alchemy.gnome_hollow.gift", factorText(tag.getDoubleOr("GiftFactor", 1)),
                        clockText(giftTicks)));
            }
            tooltip.add(Component.translatable("jade.servus_alchemy.gnome_hollow.store", tag.getIntOr("Stored", 0), tag.getIntOr("Capacity", 0)));
            int tank = tag.getIntOr("Tank", 0);
            for (Principle principle : Principle.values()) {
                tooltip.add(Component.translatable("jade.servus_alchemy.calciner.principle", tag.getIntOr(principle.id(), 0), tank,
                        principleName(principle)));
            }
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }

    private enum PurifierProvider implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        private static final Identifier UID = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "purifier");

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            BlockPos vat = PurifierStructure.vatOf(accessor.getBlockState(), accessor.getPosition()).orElse(accessor.getPosition());
            if (!(accessor.getLevel().getBlockEntity(vat) instanceof PurifierBlockEntity purifier)) {
                return;
            }
            CompoundTag tag = new CompoundTag();
            tag.putInt("Items", purifier.inventory().getStackInSlot(PurifierBlockEntity.SLOT_INPUT).getCount());
            tag.putInt("Residue", purifier.inventory().getStackInSlot(PurifierBlockEntity.SLOT_RESIDUE).getCount());
            tag.putInt("Water", purifier.water().getFluidAmount());
            tag.putInt("Salt", purifier.salt().getFluidAmount());
            tag.putInt("Capacity", purifier.water().getCapacity());
            tag.putInt("Progress", purifier.progressPercent());
            data.put("Purifier", tag);
        }

        void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains("Purifier")) {
                return;
            }
            CompoundTag tag = data.getCompoundOrEmpty("Purifier");
            int capacity = tag.getIntOr("Capacity", 0);
            tooltip.add(Component.translatable("jade.servus_alchemy.purifier.items", tag.getIntOr("Items", 0)));
            tooltip.add(Component.translatable("jade.servus_alchemy.purifier.water", tag.getIntOr("Water", 0), capacity));
            tooltip.add(Component.translatable("jade.servus_alchemy.calciner.principle", tag.getIntOr("Salt", 0), capacity,
                    principleName(Principle.SALT)));
            tooltip.add(Component.translatable("jade.servus_alchemy.purifier.residue", tag.getIntOr("Residue", 0)));
            tooltip.add(Component.translatable("jade.servus_alchemy.calciner.progress", tag.getIntOr("Progress", 0)));
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }

    private enum CalcinerProvider implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        private static final Identifier UID = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "calciner");

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            CalcinerBlockEntity calciner = CalcinerPorts.calciner(accessor.getLevel(), accessor.getPosition(), accessor.getBlockState());
            if (calciner == null) {
                return;
            }
            CompoundTag tag = new CompoundTag();
            tag.put("Input", saveStack(accessor, calciner.inventory().getStackInSlot(CalcinerBlockEntity.SLOT_INPUT)));
            tag.put("Fuel", saveStack(accessor, calciner.inventory().getStackInSlot(CalcinerBlockEntity.SLOT_FUEL)));
            tag.putInt("Progress", calciner.progressPercent());
            tag.putInt("Ash", calciner.ashCount());
            tag.putInt("AshRoom", calciner.ashRoom());
            data.put("Calciner", tag);
        }

        void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains("Calciner")) {
                return;
            }
            CompoundTag tag = data.getCompoundOrEmpty("Calciner");
            ItemStack input = parseStack(accessor, tag.getCompoundOrEmpty("Input"));
            ItemStack fuel = parseStack(accessor, tag.getCompoundOrEmpty("Fuel"));
            tooltip.add(Component.translatable("jade.servus_alchemy.calciner.input", stackText(input)));
            tooltip.add(Component.translatable("jade.servus_alchemy.calciner.fuel", stackText(fuel)));
            tooltip.add(Component.translatable("jade.servus_alchemy.calciner.progress", tag.getIntOr("Progress", 0)));
            tooltip.add(Component.translatable("jade.servus_alchemy.calciner.ash", tag.getIntOr("Ash", 0),
                    tag.getIntOr("AshRoom", 0)));
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
