# Proposal

## Why

The Burner Pump is a small generic machine box with a furnace front, and nothing about it says how fire turns into pumped water. The first practical steam engine, Newcomen's beam engine, was exactly that: a mine pump driven by a boiler, a cylinder and a rocking beam. Giving the Burner Pump that shape makes it readable at a glance and gives it the size a steam machine needs. The Electric Pump keeps its compact body, which also makes the two pumps easy to tell apart.

This is a play feel follow-up to PLAN.md M9 item 1, `m9-foundation-utilities`, which moved the pumps to Foundation. It is added to the M9 list as item 1b.

## What Changes

- The Burner Pump becomes a tiny beam engine one block wide and two blocks tall. One item places both blocks, like a door. Placing needs room above, and breaking either half removes both and drops one Burner Pump.
- The lower half holds everything the pump does today: the firebox front, the boiler, the fuel slot, the water buffer, the GUI, the water source check below it and the pipe stubs. Water and fuel use the back and the two sides of the lower half.
- The upper half carries the chimney, the cylinder and the beam pivot. It has no inventory, no buffer and no connections. Right clicking it or using the carrot on it acts on the lower half.
- While the pump burns fuel and works, the beam rocks and the pump rod moves with it, and smoke rises from the chimney top. When idle, the beam stands still and no smoke rises.
- The Burner Pump no longer has a top outlet and no pipe stub on top. The Electric Pump is unchanged, including its top outlet.
- Both pump items place on top of the water source the player looks at, like a lily pad, instead of sinking to the pond floor.
- PLAN.md gets M9 item 1b and the pump paragraph describes the Burner Pump's new look. The docs page for Foundation is updated the same way.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `foundation/pumps`: the Burner Pump gets a two block steam engine body with placement and breaking rules, the beam animation and chimney smoke; the faces where it gives water, takes fuel and shows stubs exclude its top; the compact body and the top outlet now apply to the Electric Pump only.

## Non-goals

- No change to pump rates, buffer size, fuel use, the water source rule or recipes. All balance stays in the existing datapack JSON.
- No new multiblock and no forming step. The two halves come from one item.
- No change to the Electric Pump's look.
- No sounds for the engine. A chuffing sound can follow after a play check.
- No steam as a fluid, no boiler that needs water, no engine that outputs rotation or power.

## Impact

- `foundation` module, package `machine/pump`: `BurnerPumpBlock` gains a half property, two block placement and removal, per half shapes, and gives water and fuel only on its lower half's back and sides. `PumpBlock` keeps the shared stub logic.
- `foundation` datagen: new multipart models for both halves, a standalone beam and rod model, an item model showing the whole engine, and a loot table that drops only from the lower half.
- `foundation` client: a new block entity renderer for the beam and rod replaces the side marker renderer on the Burner Pump, plus smoke particles from the block.
- `PumpGameTests` and `MachineBaseGameTests`: helpers place both halves; the tests that read the buffer through the top face read it from a side.
- `PLAN.md` M9 list and pumps paragraph, `docs/src/content/docs/mods/foundation.md`.
- No API, capability type or data format changes. Other mods still see `IItemHandler` and `IFluidHandler` on the pump's lower half.
