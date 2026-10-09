package io.github.dirnei.mc.servus.manufacturing.integration.jade;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinerBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinerBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerDuctBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerPartBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerPorts;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerShellBlock;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
 * Shows what a formed Calciner holds when the player looks at any of its blocks: the matter, the
 * fuel, the progress and the three principles. Only Jade loads this class.
 */
@WailaPlugin(ServusManufacturingIds.MOD_ID)
public final class CalcinerJadePlugin implements IWailaPlugin {

    private static final List<Class<? extends Block>> BLOCKS = List.of(CalcinerBlock.class, CalcinerPartBlock.class, CalcinerShellBlock.class,
            CalcinerDuctBlock.class);

    @Override
    public void register(IWailaCommonRegistration registration) {
        BLOCKS.forEach(block -> registration.registerBlockDataProvider(Provider.INSTANCE, block));
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        BLOCKS.forEach(block -> registration.registerBlockComponent(Provider.INSTANCE, block));
    }

    private enum Provider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;

        private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "calciner");

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            CalcinerBlockEntity calciner = CalcinerPorts.calciner(accessor.getLevel(), accessor.getPosition(), accessor.getBlockState());
            if (calciner == null) {
                return;
            }
            CompoundTag tag = new CompoundTag();
            tag.put("Input", calciner.inventory().getStackInSlot(CalcinerBlockEntity.SLOT_INPUT).saveOptional(accessor.getLevel().registryAccess()));
            tag.put("Fuel", calciner.inventory().getStackInSlot(CalcinerBlockEntity.SLOT_FUEL).saveOptional(accessor.getLevel().registryAccess()));
            tag.putInt("Progress", calciner.progressPercent());
            tag.putInt("Capacity", calciner.tanks().capacity());
            for (Principle principle : Principle.values()) {
                tag.putInt(principle.id(), calciner.tanks().amount(principle));
            }
            data.put("Calciner", tag);
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains("Calciner")) {
                return;
            }
            CompoundTag tag = data.getCompound("Calciner");
            ItemStack input = ItemStack.parseOptional(accessor.getLevel().registryAccess(), tag.getCompound("Input"));
            ItemStack fuel = ItemStack.parseOptional(accessor.getLevel().registryAccess(), tag.getCompound("Fuel"));
            tooltip.add(Component.translatable("jade.servus_manufacturing.calciner.input", stackText(input)));
            tooltip.add(Component.translatable("jade.servus_manufacturing.calciner.fuel", stackText(fuel)));
            tooltip.add(Component.translatable("jade.servus_manufacturing.calciner.progress", tag.getInt("Progress")));
            int capacity = tag.getInt("Capacity");
            for (Principle principle : Principle.values()) {
                FluidStack stack = new FluidStack(PrincipleFluids.fluid(principle), 1);
                tooltip.add(Component.translatable("jade.servus_manufacturing.calciner.principle",
                        tag.getInt(principle.id()), capacity, stack.getHoverName()));
            }
        }

        private static Component stackText(ItemStack stack) {
            return stack.isEmpty() ? Component.translatable("jade.servus_manufacturing.calciner.empty")
                    : Component.translatable("jade.servus_manufacturing.calciner.stack", stack.getCount(), stack.getHoverName());
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }
}
