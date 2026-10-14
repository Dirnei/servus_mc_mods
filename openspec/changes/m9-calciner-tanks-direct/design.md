# Design

## Context

After `m9-calciner-compact`, `CalcinerBlockEntity` keeps a `PrincipleTanks` in give mode with one capacity for all three principles (`fluid_capacity` 4000 in `machine/calciner.json`). `canWork` checks `tanks.contents().fits(output, capacity)`. `finishWork` inserts the whole output. Every `PUSH_INTERVAL` ticks `CalcinerPorts.push` fills the Sulfur and Mercury Fluid Tanks of the structure from those tanks through `Capabilities.Fluid.BLOCK`. `CalcinerPorts.fluid` gives a drain-only view of Mercury at the Receiver and of Sulfur at the lower duct, and is registered in `ServusManufacturingEvents`. `CalcinerPartBlock.useItemOn` lets a fluid container use that port. Salt stays in the Salt tank and the Ash Pan counts it as `salt / saltPerAsh` Ash, so 4000 / 100 gives 40 Ash.

`BurnerMachineBlockEntity` calls `canWork()` every tick and `finishWork()` only on a tick where `canWork()` was true.

The Receiver's flask (`AlchemyModels.receiver`) uses `minecraft:block/glass` with automatic UVs, so the faces of a box from 4 to 12 sample the clear middle of the texture.

## Goals / Non-Goals

**Goals:**
- Sulfur and Mercury have one home: the structure's Fluid Tanks.
- The Ash Pan holds a stack of Ash, its room in data.
- A visible, connected Receiver: beak, condenser, tap, drip.

**Non-Goals:**
- Touching `PrincipleTanks` behaviour for the Coagulator, Gnome Hollow or Purifier.

## Decisions

**No internal Sulfur or Mercury.** `canWork` asks the Sulfur and Mercury tanks, through `Capabilities.Fluid.BLOCK` and the existing `FluidView`, whether a simulated fill takes the recipe's whole Sulfur and Mercury (a zero amount always fits). `finishWork` fills them for real. It runs on the same tick as a passing `canWork`, on the server thread, so nothing can take the room in between. A cycle that finds no room waits with its progress, like the full Ash Pan does today. The push timer, `PUSH_INTERVAL` and `CalcinerPorts.push` go away. Alternative: keep a small buffer for robustness. Rejected because it brings back the second home for the same fluid, which is what the user asked to drop.

**Salt as its own amount.** The Calciner keeps `int salt` in mB instead of a `PrincipleTanks`. Recipes give Salt in mB and one Ash is worth `saltPerAsh()` mB, so whole Ash stay `salt / saltPerAsh` (`AshCount`, unchanged). The room is `ashRoom × saltPerAsh()` mB. `ashRoom` comes from a new optional field `item_capacity` on Foundation `MachineStats` (codec, `Partial.over`, factories with 0). `machine/calciner.json` gets `"item_capacity": 64` and drops `fluid_capacity`. A pure helper `AshCount.fits(salt, extra, ashRoom, saltPerAsh)` with JUnit tests decides the room. Alternative: a hard coded stack of 64. Rejected because rooms are balance numbers and live in data.

**Ports.** `CalcinerPorts.fluid` and `principle` go, together with their `Capabilities.Fluid.BLOCK` registration and the fluid container branch in `CalcinerPartBlock.useItemOn`. A bucket on the Receiver then falls through to the default use and does nothing. The Ash Pan item port and the empty hand use stay; they read `ashCount()` and `takeAsh()`.

**Sync and look.**
- `getUpdateTag` sends `Salt` and `AshRoom` instead of the principle tanks.
- `DrawnFill` gets an overload for one amount and room, so the ash heap still syncs in sixteenths.
- `CalcinerRenderer` drops the Receiver fill and draws the ash heap by `salt / room`. The drip and puddle code stays.
- Jade sends `Ash` and `AshRoom` and shows `jade.servus_manufacturing.calciner.ash` ("%s / %s Ash"). The principle lines and `Capacity` go.

**Receiver model (preview variant R5).** Model pixels, front facing north, Alembic one block to +x:
- Unformed: a cut copper base 4..12 by 0..1, the copper drum 5..11 by 1..7 by 5..11 with a cut copper lid 4.5..11.5 by 7..8, and a neck 7..9 by 8..9.
- Formed (`ao(false)` as today):
  - the copper drum 5..11 by 4..10 by 5..11
  - a cut copper lid 4.5..11.5 by 10..11 and a neck 7..9 by 11..12
  - four cut copper legs 1 by 1 at x and z 4.6 and 10.4, from y -4 up into the drum to y 4.5, with explicit UVs like today
  - a cut copper cone 6.5..9.5 by 3..4, the tap 7..9 by 1.5..3 and its collar 6.5..9.5 by 1..2
- The formed Alembic's beak shortens from x -4 to x -5, so it ends 1 px inside the drum side at receiver x 11 (into the drum, not on its face).
- The drum uses `minecraft:block/copper_block`, the rest `cut_copper`; the glass texture key goes.

**Face check.** Two pairs share a plane:
- The lid bottom y 10 and the drum top y 10 face opposite ways, and the drum top is hidden: edge contact.
- The cone top y 4 and the drum bottom y 4 likewise.

Everything else is clear:
- The legs pass into the drum with side faces at 4.6..5.6 and 10.4..11.4, never on the drum faces 5 and 11.
- The neck sits on the lid at y 11: edge contact.
- The tap top at y 3 meets the cone bottom at y 3 with opposite faces.
- The beak ends inside the drum.

**Texts.** The Journal entry (`WorkshopCategory`) says the Receiver is a copper condenser whose tap drips Mercury into the basin and the right tank, that the Ash Pan holds a stack of Ash, and that the Calciner waits when the Ash Pan or a tank is full. `docs/.../manufacturing.md` replaces the stale Calciner line (side panel, 4,000 mB) with the structure, the tanks and the 64 Ash. `GameDataExportProvider` exports `item_capacity` when it is set.

**PLAN.md.** This settles no open decision. The M9 list gets this follow up after `m9-calciner-compact`.

**Pure logic vs Minecraft.** `AshCount.fits` is pure and JUnit tested. The tank checks, ports, sync, Jade and models are covered by the Calciner GameTests (rewritten where they inserted into the old tanks) and by the user's play check.

## Risks / Trade-offs

- [A tank removed mid-cycle unforms the structure] → Unchanged behaviour: the Calciner stops while unformed and keeps its Salt, input and fuel.
- [Old saves lose Sulfur and Mercury sitting in the old buffer] → Unreleased, no migration.
- [A pack leaves `item_capacity` out or sets it to 0] → The Calciner would never work, so it treats a room below 1 as one stack (64).
