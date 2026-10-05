# Proposal

## Why

The playtest found that machines accepting and pushing items on every side makes tight builds unpredictable: an extractor fed a hopper above it, and neighbours swap items nobody intended. Players need to say per side what a machine does. The user decided this is configured in the machine GUI, not with the wrench. Not a PLAN.md milestone item by name, but it falls under M3's balance and polish pass and Logistics' per side modes build on the same types.

## What Changes

- Pure Java `SideMode` (Auto, Input, Output, Both, Disabled), `RelativeSide` (front, back, left, right, top, bottom relative to a facing) and `SideConfig` in Servus Lib, with JUnit tests.
- Every Manufacturing machine gets a side configuration. Automation sees one item handler per side: Input and Both sides accept, Output and Both sides give, Disabled sides do nothing, Auto keeps today's behavior. Output push only goes through sides that allow output.
- A side panel in every machine GUI: a 3 by 3 grid of the six relative sides left of the inventory, click cycles the mode, colors and a tooltip show it. The click travels through the vanilla menu button mechanism.
- Sneak wrench dismantling keeps the block entity data on the dropped item (side config, selected recipe, owner, energy), so a machine can be moved without reconfiguring. Breaking with a pickaxe drops a fresh machine.

## Capabilities

### New Capabilities
- `lib/side-config`: the side modes, relative sides and their rules, shared by all Servus mods.

### Modified Capabilities
- `manufacturing/machines`: adds the side configuration behavior and the GUI panel.

## Non-goals

- Energy sides. Energy stays on all sides; cables and generators only touch what they should.
- Fluids.
- Wrench based side editing (decided against).
- Side configuration for cables and ducts (Logistics M5 does per side modes on ducts with these types).

## Impact

- `MachineBlockEntity` container data grows by six ints; the capability lambdas pass the side through.
- New lang keys for sides and modes.
