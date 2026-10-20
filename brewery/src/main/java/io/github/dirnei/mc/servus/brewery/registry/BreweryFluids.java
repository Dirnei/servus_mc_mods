package io.github.dirnei.mc.servus.brewery.registry;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** The worts the Brew Kettle brews: type, source, flowing form, liquid block and bucket each. */
public final class BreweryFluids {

    public static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, ServusBreweryIds.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, ServusBreweryIds.MOD_ID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ServusBreweryIds.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ServusBreweryIds.MOD_ID);

    /**
     * Everything one wort registers.
     *
     * @param color the wort's tint, as RGB
     */
    public record Wort(String id, int color,
                       DeferredHolder<FluidType, FluidType> type,
                       DeferredHolder<Fluid, BaseFlowingFluid.Source> source,
                       DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing,
                       DeferredBlock<LiquidBlock> block,
                       DeferredItem<BucketItem> bucket) {
    }

    public static final Wort PALE_WORT = create("pale_wort", 0xC8902A);
    public static final Wort DARK_WORT = create("dark_wort", 0x4A2410);
    public static final Wort WHEAT_WORT = create("wheat_wort", 0xD8A848);

    public static final List<Wort> WORTS = List.of(PALE_WORT, DARK_WORT, WHEAT_WORT);

    private BreweryFluids() {
    }

    private static Wort create(String name, int color) {
        DeferredHolder<FluidType, FluidType> type = TYPES.register(name, () -> new FluidType(FluidType.Properties.create()
                .descriptionId("fluid_type." + ServusBreweryIds.MOD_ID + "." + name)
                .canConvertToSource(false)
                .density(1050)
                .viscosity(1200)));
        // The properties need the source, flowing form, block and bucket before they exist, hence the holders.
        BaseFlowingFluid.Properties[] properties = new BaseFlowingFluid.Properties[1];
        DeferredHolder<Fluid, BaseFlowingFluid.Source> source = FLUIDS.register(name, () -> new BaseFlowingFluid.Source(properties[0]));
        DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing =
                FLUIDS.register("flowing_" + name, () -> new BaseFlowingFluid.Flowing(properties[0]));
        DeferredBlock<LiquidBlock> block = BLOCKS.registerBlock(name, props -> new LiquidBlock(source.get(), props),
                () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WATER));
        DeferredItem<BucketItem> bucket = ITEMS.registerItem(name + "_bucket", props -> new BucketItem(source.get(), props),
                () -> new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));
        properties[0] = new BaseFlowingFluid.Properties(type, source, flowing).block(block).bucket(bucket)
                .slopeFindDistance(2).levelDecreasePerBlock(2);
        return new Wort(name, color, type, source, flowing, block, bucket);
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}
