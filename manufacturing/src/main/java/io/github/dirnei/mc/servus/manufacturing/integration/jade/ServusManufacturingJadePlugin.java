package io.github.dirnei.mc.servus.manufacturing.integration.jade;

import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorCrownBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principles;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorBlock;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinerBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinerBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.CopperDuctBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerPartBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerPorts;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerShellBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.purifier.PurifierSideBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.purifier.PurifierStructure;
import io.github.dirnei.mc.servus.manufacturing.alchemy.purifier.PurifierBlock;
import net.minecraft.core.BlockPos;
import io.github.dirnei.mc.servus.manufacturing.alchemy.purifier.PurifierBlockEntity;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
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
 * progress, the three principles) and the Purifier (Ash, water, Salt, progress). Only Jade loads this class.
 */
@WailaPlugin(ServusManufacturingIds.MOD_ID)
public final class ServusManufacturingJadePlugin implements IWailaPlugin {

    private static final List<Class<? extends Block>> BLOCKS = List.of(CalcinerBlock.class, CalcinerPartBlock.class, CalcinerShellBlock.class,
            CopperDuctBlock.class);

    @Override
    public void register(IWailaCommonRegistration registration) {
        BLOCKS.forEach(block -> registration.registerBlockDataProvider(CalcinerProvider.INSTANCE, block));
        registration.registerBlockDataProvider(PurifierProvider.INSTANCE, PurifierBlock.class);
        registration.registerBlockDataProvider(PurifierProvider.INSTANCE, PurifierSideBlock.class);
        registration.registerBlockDataProvider(AthanorProvider.INSTANCE, AthanorBlock.class);
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
        return stack.isEmpty() ? Component.translatable("jade.servus_manufacturing.calciner.empty")
                : Component.translatable("jade.servus_manufacturing.calciner.stack", stack.getCount(), stack.getHoverName());
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

        private static final Identifier UID = Identifier.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "athanor");

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

        void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains("Athanor")) {
                return;
            }
            CompoundTag tag = data.getCompoundOrEmpty("Athanor");
            tooltip.add(Component.translatable(tag.getBooleanOr("Formed", false) ? "jade.servus_manufacturing.athanor.stage"
                    : "jade.servus_manufacturing.athanor.unformed", tag.getStringOr("Tier", "")));
            ItemStack input = parseStack(accessor, tag.getCompoundOrEmpty("Input"));
            ItemStack output = parseStack(accessor, tag.getCompoundOrEmpty("Output"));
            tooltip.add(Component.translatable("jade.servus_manufacturing.athanor.input", stackText(input)));
            tooltip.add(Component.translatable("jade.servus_manufacturing.athanor.output", stackText(output)));
            tooltip.add(Component.translatable("jade.servus_manufacturing.calciner.progress", tag.getIntOr("Progress", 0)));
            for (Principle principle : Principle.values()) {
                tooltip.add(Component.translatable("jade.servus_manufacturing.athanor.principle", tag.getIntOr(principle.id(), 0),
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

        private static final Identifier UID = Identifier.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "purifier");

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
            tooltip.add(Component.translatable("jade.servus_manufacturing.purifier.items", tag.getIntOr("Items", 0)));
            tooltip.add(Component.translatable("jade.servus_manufacturing.purifier.water", tag.getIntOr("Water", 0), capacity));
            tooltip.add(Component.translatable("jade.servus_manufacturing.calciner.principle", tag.getIntOr("Salt", 0), capacity,
                    principleName(Principle.SALT)));
            tooltip.add(Component.translatable("jade.servus_manufacturing.purifier.residue", tag.getIntOr("Residue", 0)));
            tooltip.add(Component.translatable("jade.servus_manufacturing.calciner.progress", tag.getIntOr("Progress", 0)));
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }

    private enum CalcinerProvider implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        private static final Identifier UID = Identifier.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "calciner");

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
            tag.putInt("Capacity", calciner.tanks().capacity());
            for (Principle principle : Principle.values()) {
                tag.putInt(principle.id(), calciner.tanks().amount(principle));
            }
            tag.putInt("Ash", calciner.ashCount());
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
            tooltip.add(Component.translatable("jade.servus_manufacturing.calciner.input", stackText(input)));
            tooltip.add(Component.translatable("jade.servus_manufacturing.calciner.fuel", stackText(fuel)));
            tooltip.add(Component.translatable("jade.servus_manufacturing.calciner.progress", tag.getIntOr("Progress", 0)));
            int capacity = tag.getIntOr("Capacity", 0);
            tooltip.add(Component.translatable("jade.servus_manufacturing.purifier.items", tag.getIntOr("Ash", 0)));
            for (Principle principle : Principle.values()) {
                if (principle == Principle.SALT) {
                    continue;
                }
                tooltip.add(Component.translatable("jade.servus_manufacturing.calciner.principle",
                        tag.getIntOr(principle.id(), 0), capacity, principleName(principle)));
            }
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
