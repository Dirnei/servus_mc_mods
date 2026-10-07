# Design

## Context

`ShuttleGarageBlockEntity.acceptsRail` returns true only for the facing side. `RailBlock.connectsTo` connects to rails, elevators and any node for which `docksTo` holds (`docksRail() && acceptsRail(side)`), and sets the matching `DOCK_*` flag from the same test; `RailModels` draws an arm when connected and not docked, a dock piece when docked. `ShuttleFleet.refreshHome` takes the first travel neighbour the network links, so the home rail follows the link. The garage model comes from the `oriented` datagen helper (front, side, top textures); the front texture holds the doorway and the lamp.

## Goals / Non-Goals

**Goals:**
- Rail at the back, plain arm, no other behaviour change.

**Non-Goals:**
- See proposal.

## Decisions

### Settles no PLAN.md open decision

### Rail side
`acceptsRail` returns true for the side opposite the facing. Nothing else in the network changes.

### Arm instead of dock
`StorageNodeBlockEntity.showsDock()` defaults to `docksRail()`; the garage overrides it to false. `RailBlock.connectsTo` keeps using `docksTo` (link), while the `DOCK_*` flags use `docksTo && showsDock()`. Linked but not docked renders the arm.

### Model and textures
A `garage` helper in the blockstate provider builds `models().cube` with the front texture (lamp, no doorway) on north, a new `shuttle_garage_back` texture (doorway with rail, no lamp) on south, side on east and west, top on up and down, rotated by the facing like `oriented`. The texture script splits the old front art into a lamp only front and a doorway only back.

### Minecraft side verification
Existing garage GameTests place the rail in front; they flip the facing so the rail is behind. A new GameTest `garageRailIsAnArm` asserts the rail behind a garage is connected without the dock flag and that a rail in front neither connects nor docks.

## Risks / Trade-offs

- [Existing worlds with garages facing their rail] → none released; placed garages lose their home rail until turned with the carrot.

## Open Questions

None.
