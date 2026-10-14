package io.github.dirnei.mc.servus.alchemy;

import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import java.util.EnumMap;
import java.util.Map;
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

/** Salt, Sulfur and Mercury as fluids: type, source, flowing form, liquid block and bucket each. */
public final class PrincipleFluids {

    public static final DeferredRegister<FluidType> TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, ServusAlchemyIds.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, ServusAlchemyIds.MOD_ID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ServusAlchemyIds.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ServusAlchemyIds.MOD_ID);

    /** Everything one principle registers. */
    public record Entry(Principle principle,
                        DeferredHolder<FluidType, FluidType> type,
                        DeferredHolder<Fluid, BaseFlowingFluid.Source> source,
                        DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing,
                        DeferredBlock<LiquidBlock> block,
                        DeferredItem<BucketItem> bucket) {
    }

    private static final Map<Principle, Entry> ENTRIES = new EnumMap<>(Principle.class);

    static {
        for (Principle principle : Principle.values()) {
            ENTRIES.put(principle, create(principle));
        }
    }

    private PrincipleFluids() {
    }

    private static Entry create(Principle principle) {
        String name = principle.id();
        DeferredHolder<FluidType, FluidType> type = TYPES.register(name, () -> new FluidType(FluidType.Properties.create()
                .descriptionId("fluid_type." + ServusAlchemyIds.MOD_ID + "." + name)
                .canConvertToSource(false)
                .density(1200)
                .viscosity(1500)));
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
        return new Entry(principle, type, source, flowing, block, bucket);
    }

    public static Entry entry(Principle principle) {
        return ENTRIES.get(principle);
    }

    /** The principle a fluid is, or {@code null} for any other fluid. */
    public static Principle principleOf(Fluid fluid) {
        for (Entry entry : ENTRIES.values()) {
            if (fluid.isSame(entry.source().get())) {
                return entry.principle();
            }
        }
        return null;
    }

    public static Fluid fluid(Principle principle) {
        return ENTRIES.get(principle).source().get();
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}
