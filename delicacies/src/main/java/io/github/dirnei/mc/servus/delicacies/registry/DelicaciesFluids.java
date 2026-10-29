package io.github.dirnei.mc.servus.delicacies.registry;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
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

/** Murky Water, drained from an aquarium: type, source, flowing form, liquid block and bucket. */
public final class DelicaciesFluids {

    public static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, ServusDelicaciesIds.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, ServusDelicaciesIds.MOD_ID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ServusDelicaciesIds.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ServusDelicaciesIds.MOD_ID);

    public static final String MURKY_WATER_ID = "murky_water";
    /** The brown green tint of Murky Water, as RGB. */
    public static final int MURKY_WATER_COLOR = 0x5A6B3A;

    public static final DeferredHolder<FluidType, FluidType> MURKY_WATER_TYPE = TYPES.register(MURKY_WATER_ID,
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type." + ServusDelicaciesIds.MOD_ID + "." + MURKY_WATER_ID)
                    .canConvertToSource(false)
                    .canExtinguish(true)
                    .supportsBoating(true)
                    .canSwim(true)
                    .canDrown(true)
                    .density(1020)
                    .viscosity(1100)));

    // The properties need the source, flowing form, block and bucket before they exist, hence the holder array.
    private static final BaseFlowingFluid.Properties[] PROPERTIES = new BaseFlowingFluid.Properties[1];

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> MURKY_WATER =
            FLUIDS.register(MURKY_WATER_ID, () -> new BaseFlowingFluid.Source(PROPERTIES[0]));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_MURKY_WATER =
            FLUIDS.register("flowing_" + MURKY_WATER_ID, () -> new BaseFlowingFluid.Flowing(PROPERTIES[0]));
    public static final DeferredBlock<LiquidBlock> MURKY_WATER_BLOCK = BLOCKS.registerBlock(MURKY_WATER_ID,
            properties -> new LiquidBlock(MURKY_WATER.get(), properties), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WATER));
    public static final DeferredItem<BucketItem> MURKY_WATER_BUCKET = ITEMS.registerItem(MURKY_WATER_ID + "_bucket",
            properties -> new BucketItem(MURKY_WATER.get(), properties),
            () -> new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));

    static {
        PROPERTIES[0] = new BaseFlowingFluid.Properties(MURKY_WATER_TYPE, MURKY_WATER, FLOWING_MURKY_WATER)
                .block(MURKY_WATER_BLOCK).bucket(MURKY_WATER_BUCKET).slopeFindDistance(2).levelDecreasePerBlock(2);
    }

    private DelicaciesFluids() {
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}
