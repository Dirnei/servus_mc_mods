# Proposal

## Why

Building a warehouse means placing stations, racks and Field Heads next to rails, and today most of them face the player: standing in the aisle and placing a rack against the rail turns the rack's back to the rail, so players walk around or turn blocks by hand. The garage already turns its doorway to the rail on placement; every other block that docks to a rail should do the same. Rack walls are also slow to build one block at a time, and right clicking a rack or station front with a block in hand opens a GUI instead of placing.

This extends PLAN.md M6 Warehouse automation (rails, racks, placement assist), which is otherwise done. It also takes up the `m6-placement-assist` non-goal "racks can adopt the assist later".

## What Changes

- **Auto facing on placement:** the Inbound, Outbound and Packing Station, the Fluid Inbound and Fluid Outbound, the Heavy Duty Rack, the Fluid Rack and the Field Head turn their rail side to a rail when placed, by the garage's existing rule: the rail clicked, else a rail in the look direction, else the only rail beside it, else the front faces the player. The Field Head looks for Farm Rails; its front and ripe bar then face the rail.
- **Sneaking skips it:** a sneak placement faces the player, for all those blocks and the garage. Rails and the elevator keep their placement as it is.
- **Racks copy the rack they are placed against:** a rack placed by clicking any face of a rack of its kind takes that rack's facing; a rack on top of a rack still takes the facing below.
- **Rack section layer:** clicking the top of a rack with a rack of the same kind, not sneaking, places a full layer over that rack's section (one item per bay, all spots free, always in creative), like the Fluid Tank's layer; otherwise one rack is placed as usual.
- **Rack row assist:** sneaking while looking at a rack with a rack of the same kind extends the rack's row at its first free spot, with the Foundation ghost and arrow preview and reach, copying the row's facing.
- **Racks and rails place against racks:** right clicking a Heavy Duty Rack or Fluid Rack while holding a rack or a Warehouse Rail (item tag `servus_shuttles:places_on_racks`) places it against the rack instead of opening the rack's GUI.

## Capabilities

### New Capabilities
- `shuttles/rail-facing`: the shared facing rule for blocks that dock to a rail, which blocks use it, the sneak override, and the `places_on_racks` tag that places racks and rails against racks.

### Modified Capabilities
- `shuttles/shuttle-garage`: the Doorway requirement gains the sneak override and points at the shared rule.
- `shuttles/heavy-duty-rack`: the row requirement takes the facing of a clicked rack; new section layer and row assist requirements.
- `shuttles/fluid-rack`: same as the Heavy Duty Rack.
- `shuttles/field-head`: the wood and facing requirement turns the front (and ripe bar) to a Farm Rail.
- `shuttles/fluid-inbound`: placement faces the rail instead of always the player.
- `shuttles/fluid-outbound`: placement faces the rail like the Fluid Inbound.

## Non-goals

- No auto facing for rails, the elevator, the controller, terminals, CPU or other blocks that do not dock to a rail.
- No change to what a Field Head serves: it still serves a Farm Rail on any side.
- No layer over a whole row, only the clicked section; no vertical assist (the layer covers going up).
- No ghost preview for the section layer (the Fluid Tank layer has none either).
- No turning of placed blocks: the carrot still does not turn racks or stations.

## Impact

- `shuttles`: `garage/GarageFacing` becomes a shared pure `rail/RailFacing` (JUnit), used by `ShuttleGarageBlock`, `StationBlock` (Inbound, Outbound, Packing), `FluidStationBlock`, `HeavyRackBlock`, `FluidRackBlock`, `FieldHeadBlock`; `HeavyRackItem` and `FluidRackItem` become assisted block items with a section layer; a new rack `PlacementAssist`; `useItemOn` of the GUI blocks checks the new item tag; `ServusShuttlesItemTagsProvider` fills the tag.
- `foundation`: `PlacementAssist` gains a default start aware `isRun` that `PlacementAssists.target` calls; the existing placement assist, ghost and config are reused. Not player visible, so no Foundation changelog line.
- No saved data changes, no migration: only new placements behave differently.
- `m10-shuttle-sounds` (proposed, not applied) touches the garage too, but its block entity and menu, not placement.
- Shuttles CHANGELOG under Unreleased.
