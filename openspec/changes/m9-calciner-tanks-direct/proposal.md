# Proposal

## Why

PLAN.md M9, a follow up to `m9-calciner-compact` from the play check. Three things are off in the compact Calciner:
- The Receiver's glass flask is invisible: its faces sample the clear middle of the glass texture, so the Alembic's beak ends in the air and the tap floats over the basin.
- The Ash Pan holds only 40 Ash, because the Salt shares the 4,000 mB principle buffer with Sulfur and Mercury, while Ash is a solid that should hold a stack.
- The Calciner fills its own two Fluid Tanks, but it still keeps a 4,000 mB internal buffer for Sulfur and Mercury, with fluid ports at the Receiver and the duct that only drain what has not reached the tanks yet. Two places for the same fluid is confusing.

## What Changes

- The Calciner puts Sulfur and Mercury straight into its two Fluid Tanks. It starts a cycle only when both tanks have room for the recipe's Sulfur and Mercury and the Ash Pan has room for its Salt. No Sulfur or Mercury waits inside the Calciner any more.
- The Ash Pan holds a stack of Ash (64 in the example data) as a solid, independent of the tanks. The number lives in the Calciner's datapack JSON.
- The Receiver and the Copper Duct expose no fluid capability any more, and an empty bucket on the Receiver does nothing. Fluids are taken from the tanks.
- The Receiver becomes a copper condenser: a small copper drum on four copper legs in the basin. The Alembic's beak enters its side, and a tap under it drips Mercury into the basin while the Calciner works. This holds formed and unformed. There is no flask and no Mercury fill drawn in it.
- Jade shows the Calciner's input, fuel, progress and Ash as a count out of its room. The tanks show their own fluid.
- The Journal book and the docs describe the condenser, the tanks and the 64 Ash.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `manufacturing/calciner`: "Calciner burns fuel" checks the tanks' and the Ash Pan's room instead of three internal tanks.
- `manufacturing/calciner-structure`: the look of the Receiver (condenser, no flask), the principle ports (no fluid capability on the Receiver and duct, Salt as Ash with its own room), buckets at the ports (none at the Receiver) and Jade (Ash count).

## Impact

- `CalcinerBlockEntity`: Salt as its own amount with an Ash room, no `PrincipleTanks`; work checks and fills the tank capabilities through `CalcinerPorts`.
- `CalcinerPorts`: no fluid port, fill at the end of a cycle instead of a push timer.
- Foundation `MachineStats`: one optional field for an item buffer room, read from `machine/calciner.json`, which drops `fluid_capacity`.
- `CalcinerRenderer`: no Mercury fill in the Receiver. The Jade plugin, the fluid capability registration and the bucket handling at the Receiver change too.
- `AlchemyModels`: Receiver models formed and unformed. Journal book text, `docs/.../manufacturing.md`.
- `CalcinerGameTests`: tests that inserted into the internal tanks change to tank and Ash checks.
- Saves with Sulfur or Mercury in the old buffer lose it; nothing is released, so there is no migration.

## Non-goals

- Changes to the Coagulator, which keeps its own principle tanks.
- Other Calciner looks, the layout or forming.
- A GUI for the Calciner.
