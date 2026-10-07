# Proposal

## Why

Play test feedback (2026-10-07): the Shuttle Garage should take its rail at the back, not at the front, and the rail should run into the garage as a normal rail piece instead of ending in the amber dock piece used for storage blocks and stations. The front then faces the player with the lamp and the GUI, the back opens onto the aisle.

## What Changes

- **Rail at the back.** The garage links to a rail only on the side opposite its facing. The doorway texture with the rail moves to the back; the front keeps the online lamp.
- **Normal rail piece.** A rail beside a garage's back renders an arm, like towards another rail, not a dock piece.
- Home rail, parking, protection and the GUI are unchanged; the home rail is simply the rail behind the garage.

## Capabilities

### Modified Capabilities
- `storage/shuttle-garage`: rail side and doorway side.
- `storage/warehouse-rail`: the piece towards a garage is an arm.

## Non-goals

- Changing how shuttles are drawn or how homes are chosen.

## Impact

- `storage`: `ShuttleGarageBlockEntity.acceptsRail` (back), a `showsDock()` hook on `StorageNodeBlockEntity` used by `RailBlock` (garage returns false), a four texture garage model in datagen, a back texture, garage GameTests flipped so the rail sits behind.
