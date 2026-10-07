# Design

## Context

`BurnerExtractorBlockEntity` extends `BurnerMachineBlockEntity`. `canWork()` asks whether the output slot can take the deposit yield, `finishWork()` inserts the yield into the output slot. Fuel is consumed from slot 0 by the base class with `ItemStack.getBurnTime(RecipeType.SMELTING)`, one item at a time when the previous burn ends. Balance numbers come from `MachineStats`, filled from `data/<ns>/machine/<id>.json` through `MachineStats.Partial`. The side handler exposes the fuel slot as insert only, so automation can never pull the reserve out. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- Route each yield to the fuel slot or the output slot with one small pure rule.
- Keep the change inside the extractor and the stats record; the base burner class stays as it is.
- Make the target a datapack number with a code default.

**Non-Goals:**
- A generic self feeding feature on the burner base class. Only the extractor produces fuel.
- Any GUI change. The fuel slot already shows the reserve.

## Decisions

### D1: Reserve target lives in MachineStats as `fuel_reserve`
Add `fuelReserve` to `MachineStats` and `Optional<Integer> fuelReserve` with `optionalFieldOf("fuel_reserve")` to `MachineStats.Partial`, defaulting to 0 in every factory. The extractor's code default becomes a burner with reserve 4, and `burner_extractor.json` states `"fuel_reserve": 4`. Alternative: a field in `DepositDefinition`. Rejected because the reserve is a machine property, not a deposit property, and the deposit file already describes the yield only.

### D2: Pure routing rule `FuelReserve`
New final class `machine/extractor/FuelReserve.java` with one static method and no Minecraft imports:

```
static boolean wantsYield(int target, boolean yieldIsFuel, boolean slotEmptyOrSameItem, int slotCount)
```

Returns true when `target > 0 && yieldIsFuel && slotEmptyOrSameItem && slotCount < target`. The block entity computes the three facts from the world (`yield.getBurnTime(RecipeType.SMELTING) > 0`, `fuel.isEmpty() || ItemStack.isSameItemSameComponents(fuel, yield)`, `fuel.getCount()`) and calls the rule. Unit tested in `manufacturing/src/test/java/.../machine/extractor/FuelReserveTest.java`. Alternative: inline the condition in the block entity. Rejected because the project rule asks for pure logic to be JUnit covered, and the rule is the only place the behavior can go wrong.

### D3: Routing in `canWork()` and `finishWork()`
`canWork()` becomes "reserve wants the yield OR output slot can take it". `finishWork()` inserts into the fuel slot when the reserve wants the yield, else into the output slot. Insertion into the fuel slot uses the raw `inventory.insertItem(SLOT_FUEL, ...)`, which accepts anything, like the output insert does today. Both facts are evaluated in the same tick so they cannot disagree.

### D4: No change to fuel consumption
`BurnerMachineBlockEntity.consumeFuel()` keeps taking one item from the fuel slot when the burn ends. With a reserve of 4 and coal at 1600 ticks a coal extractor consumes 1 per 1600 ticks and mines 1 per 80 ticks, so the reserve refills in one work period and the net output rate drops by one twentieth. That is the intended cost of self sufficiency.

## NeoForge APIs

- `ItemStack.getBurnTime(RecipeType<?>)` for the fuel test, the same call the base class uses (NeoForge 21.1 `IItemStackExtension#getBurnTime`).
- `ItemStack.isSameItemSameComponents` from vanilla 1.21.1.
- `ItemStackHandler.insertItem` from `net.neoforged.neoforge.items`.
- `GameTest` and `GameTestHelper` from vanilla, as used in `ServusManufacturingGameTests`.

## Pure versus Minecraft

- Pure Java, JUnit: `FuelReserve.wantsYield`.
- Minecraft, GameTest: an extractor on a coal deposit lit with one coal holds four coal in the fuel slot and has coal in the output slot after a set number of ticks; an extractor on an iron deposit never gets raw iron in the fuel slot.
- Manual: place on coal, drop in one coal, watch the fuel slot fill to four and the output start afterwards.

## PLAN.md open decisions

None settled. The Tier 1 machine table line for the Burner Extractor gains the reserve note.

## Risks / Trade-offs

- [The reference block GameTest changes timing if the coal extractor starts without fuel] → It starts with 64 coal, above the target, so nothing is routed to the fuel slot and the test is unaffected.
- [A player wants every mined coal in the output] → Setting `fuel_reserve` to 0 in a datapack disables the reserve.
- [A stack of a different fuel in the slot keeps the reserve from forming] → Accepted. The player chose that fuel; once it burns out the slot is empty and the reserve forms.
