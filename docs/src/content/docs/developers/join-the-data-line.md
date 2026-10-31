---
title: Join the data line
description: What a machine of another mod implements to become a producer and an upkeep member of the Servus data line, with a worked Java example.
---

This page shows how a machine of your mod joins the data line of **Servus: Foundation %foundation_version%**, so a Shuttles Crafting CPU can order from it and a warehouse keeps its fuel stocked. The example follows how our own machines do it, for example the Prelum of Servus: Alchemy.

## What happens at run time

1. A player places a Utility Duct with the data line against your machine.
2. On the next tick the duct reads your machine's capabilities on the face it touches. Your machine becomes a member with the roles `PRODUCER` and, if it keeps fuel, `UPKEEP`.
3. A player requests an item in a Storage Terminal that is not in stock. The CPU reads `recipes()` of every producer on the line and plans the steps.
4. For a step on your machine, the warehouse sends the inputs as a delivery into the face of your machine that touches the duct. If your machine is a packet endpoint, it is asked for room first.
5. Your machine works as it always does. The CPU sees it through `active()` while it runs.
6. The CPU takes the outputs out of your machine, through the same face, and sends them to a Packing Station, which puts them away.
7. Every 40 ticks the line checks your upkeep and asks a warehouse to send fuel once at least your batch is missing. Your block keeps the open request and saves it.
8. The warehouse gives your machine a number, and players can name it (see [Machine names](#machine-names)).

Each producer runs one step at a time.

## What your machine must offer

| What | How | Why |
|---|---|---|
| Recipes | `ServusFoundationCapabilities.PRODUCTION` with a `ProductionProvider` | makes it a producer |
| Fuel level (optional) | `ServusFoundationCapabilities.UPKEEP` with an `UpkeepHolder` | makes it an upkeep member |
| An item door | `Capabilities.Item.BLOCK` on the face that touches the duct, or `ServusFoundationCapabilities.PACKET_INLET` | inputs and fuel go in here, outputs come out here |
| Room before items arrive (optional) | `ServusFoundationCapabilities.PACKET_ENDPOINT` with a `PacketEndpoint` | deliveries send only what you grant, and you can say why you take nothing; see [The handshake](/servus_mc_mods/developers/deliveries/#the-handshake) |
| A number and a name (optional) | your block entity implements `NamedMachine` | players read your machine by name in terminals and messages |

The item door matters most:

- It must **accept every input** of the recipes you report, and the upkeep item.
- It must **give out the outputs**. Deliveries take them through the same face.
- It must put arriving upkeep items into the right slot, such as your fuel slot.

If your pipe facing handler cannot do all of this (for example inputs only on top and outputs only on the bottom), register `PACKET_INLET` with a handler that can. The data line uses `PACKET_INLET` before `Capabilities.Item`, and pipes and hoppers never see it.

## Worked example

A grinder from your mod. It has its own recipe type with a `SizedIngredient` input, a result stack and a time, an item handler, and a coal fuel slot.

```java
package com.example.yourmod.grinder;

import io.github.dirnei.mc.servus.foundation.production.ProductionProvider;
import io.github.dirnei.mc.servus.foundation.production.ProductionRecipe;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.data.UpkeepRequest;
import io.github.dirnei.mc.servus.foundation.production.UpkeepHolder;
import io.github.dirnei.mc.servus.foundation.production.UpkeepRequestCodec;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class GrinderBlockEntity extends BlockEntity {

    /** What the data line may order: one production recipe per grinding recipe. */
    private final ProductionProvider production = new ProductionProvider() {
        @Override
        public List<ProductionRecipe> recipes() {
            if (level == null) {
                return List.of();
            }
            return ServusRecipes.all(level, YourRecipes.GRINDING.get()).stream()
                    .map(GrinderBlockEntity::productionRecipe)
                    .flatMap(Optional::stream)
                    .toList();
        }

        @Override
        public Optional<Identifier> active() {
            return currentRecipe().map(holder -> holder.id().identifier());
        }
    };

    /** The upkeep request on its way, saved with the block. */
    private UpkeepRequest upkeepRequest;

    /** Keeps 16 coal in the fuel slot, independent of any order, and asks again once 8 are missing. */
    private final UpkeepHolder upkeep = new UpkeepHolder() {
        @Override
        public ItemStack upkeepItem() {
            return new ItemStack(Items.COAL);
        }

        @Override
        public int upkeepLevel() {
            return 16;
        }

        @Override
        public int upkeepBatch() {
            return 8;
        }

        @Override
        public int upkeepCount() {
            return fuelCount();
        }

        @Override
        public UpkeepRequest upkeepRequest() {
            return upkeepRequest;
        }

        @Override
        public void upkeepRequest(UpkeepRequest request) {
            upkeepRequest = request;
            setChanged();
        }
    };

    public GrinderBlockEntity(BlockPos pos, BlockState state) {
        super(YourBlockEntities.GRINDER.get(), pos, state);
    }

    /** One grinding recipe as the data line sees it: exact stacks, one batch, its time. */
    private static Optional<ProductionRecipe> productionRecipe(RecipeHolder<GrindingRecipe> holder) {
        GrindingRecipe recipe = holder.value();
        return ServusRecipes.items(recipe.input()).stream().findFirst()
                .map(input -> new ProductionRecipe(holder.id().identifier(), List.of(input),
                        List.of(recipe.result()), recipe.time()));
    }

    public ProductionProvider production() {
        return production;
    }

    public UpkeepHolder upkeep() {
        return upkeep;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        UpkeepRequestCodec.save(output, upkeepRequest);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        upkeepRequest = UpkeepRequestCodec.load(input);
    }

    // Your machine's own code, unchanged: the recipe that runs, the coal in the
    // fuel slot, and items(), the item handler that pipes already use. Save your
    // own data in saveAdditional and loadAdditional as well.
}
```

`ServusRecipes.items(SizedIngredient)` lists every item the ingredient accepts, each with the ingredient's count. The example reports only the first one, as the Prelum does. If any of several items should be orderable, report one `ProductionRecipe` per item, each with its own unique id.

Register the capabilities like any other block capability:

```java
package com.example.yourmod;

import com.example.yourmod.grinder.GrinderBlockEntity;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = YourMod.MOD_ID)
public final class YourCapabilities {

    private YourCapabilities() {
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, YourBlockEntities.GRINDER.get(),
                (grinder, side) -> grinder.items());
        event.registerBlockEntity(ServusFoundationCapabilities.PRODUCTION, YourBlockEntities.GRINDER.get(),
                (grinder, side) -> grinder.production());
        event.registerBlockEntity(ServusFoundationCapabilities.UPKEEP, YourBlockEntities.GRINDER.get(),
                (grinder, side) -> grinder.upkeep());
    }
}
```

The providers ignore `side` on purpose. The CPU and the upkeep check call `getCapability(..., pos, null)`, so a provider that returns `null` for a `null` side is never ordered from.

## Checklist

- `recipes()` lists only what the machine can run **right now**. It is called often, so cache it if building the list is expensive. `SingleInputRecipes` in the same package does this for one-input recipe types.
- `active()` returns the id of a recipe from `recipes()` while one runs, else empty.
- The item door on the duct face accepts the inputs and the upkeep item and gives out the outputs.
- Arriving upkeep ends up where `upkeepCount()` counts it. The request is closed when that count has risen by the amount asked.
- `upkeepCount()` counts only `upkeepItem()`. If your slot may hold another fuel, name that fuel as `upkeepItem()` while it is there, as the Foundation burners do.
- The open `UpkeepRequest` is saved with your block and loaded again, so a restart does not ask twice.
- When your block starts or stops offering a capability, call `level.invalidateCapabilities(pos)`.
- Everything runs on the server thread.

## Other members

The same pattern works for the other roles:

- **A block on the line that neither makes nor holds items**, such as a terminal, exposes `DATA_LINK`. `DataLink` is an empty marker interface, so the block entity itself can be the provider.
- **A block that passes the line on to its neighbours**, like a cable, uses a `DataCarrier` (see [Data line concepts](/servus_mc_mods/developers/data-line/#nodes-and-the-network)).
- **A block that limits how fast items enter or leave it** exposes `TRANSFER_GATE` (see [Deliveries](/servus_mc_mods/developers/deliveries/#transfer-gates)).
- **Storage** can expose `STOCK_PROVIDER` and `STOCK_SINK` and becomes a `STOCK` member. Note that Shuttles %shuttles_version% plans only with the stock of its own warehouses, so a foreign storage member is not counted as stock by the CPU or the terminal.

## Machine names

In Shuttles %shuttles_version% a warehouse numbers every machine on its data line that implements `io.github.dirnei.mc.servus.foundation.naming.NamedMachine`, and the Production Terminal, CPU messages and the Data Potato show its label: "Grinder #2", or with a name "Ore Grinder (Grinder #2)". The number is per kind and per warehouse. A machine without `NamedMachine` is shown by its block name.

1. Register a `MachineKind` once while your mod is constructed. Its id names the lang key of the numbered label, `machine.<namespace>.<path>`, which takes the number as `%s`:

```java
public static final MachineKind GRINDER = MachineKinds.register(new MachineKind(
        Identifier.fromNamespaceAndPath(YourMod.MOD_ID, "grinder"), YourBlocks.GRINDER));
```

```json
"machine.yourmod.grinder": "Grinder #%s"
```

2. Let the block entity implement `NamedMachine` with a `MachineNameSlot`, and forward the save, load, update tag and component hooks through `NamedMachines`, so the number and name survive saving, syncing and being picked up:

```java
public class GrinderBlockEntity extends BlockEntity implements NamedMachine {

    private final MachineNameSlot nameSlot = new MachineNameSlot(this, YourMachineKinds.GRINDER);

    @Override
    public MachineNameSlot nameSlot() {
        return nameSlot;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        NamedMachines.save(this, output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        NamedMachines.load(this, input);
    }

    // Likewise getUpdateTag (NamedMachines.writeUpdate), collectImplicitComponents (collect),
    // applyImplicitComponents (apply) and removeComponentsFromTag (removeFromTag).
}
```

3. To let players rename it, have your menu implement `MachineNameMenu` (return the machine from `namedMachine()`) and send `new RenameMachinePayload(typed)` to the server from your screen. Foundation registers the payload and renames the machine of the open menu.

A multiblock whose data line port is a block without a block entity implements `NamedMachinePart` on that block and points to the block entity that holds the name. `MachineLabels.at(level, pos)` gives the label of whatever stands at a position.

## Data Potato report (optional)

A block class that implements `io.github.dirnei.mc.servus.foundation.item.PotatoInspectable` answers the Data Potato with chat lines from `potatoReport(Level, BlockPos, BlockState, Vec3)`. It is called on the server only and must not change anything. The block must return `PASS` from `useItemOn` for the potato, so the potato gets the click.

## Testing

`/servus_foundation data move <from> <to> <item> <count>` (ops only) starts a delivery between two members and reports its id or why it failed. Use it to check that your item door takes and gives the right items.
