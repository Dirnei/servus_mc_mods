# Proposal

## Why

Play test (2026-10-07): the front of a machine is where the player stands and looks at the lamps and the GUI; items should never go in or out there. Today the front is a side like any other and can be set to anything.

## What Changes

- A side configuration can fix single sides to one mode. Fixed sides keep their mode through cycling, the panel, the carrot and loading.
- Machines and the Crafting Station fix their front to Off. The panel shows the front cell greyed and does not react; the carrot on the front only reports Off.
- Stations keep their fronts as they are: the Outbound Station pushes through its front by design, the Inbound Station accepts there.

## Capabilities

### Modified Capabilities
- `lib/side-config`: fixed sides.
- `manufacturing/machines`: the front exposes nothing.
- `storage/crafting-station`: the front exposes nothing.

## Non-goals

- Fixing station fronts.

## Impact

- `lib`: `SideConfig.lock`, `isLocked`, `isDefault` ignoring locked sides; `SideConfigPanel` greyed cells; lang key.
- `manufacturing`: `MachineBlockEntity` locks the front; GameTest `machineFrontIsClosed`.
- `storage`: `CraftingStationBlockEntity` locks the front.
