# Proposal

## Why

PLAN.md M10 item 2, `m10-shuttle-looks`. Since `m10-shuttle-tiers` a shuttle has a tier, Speed and Handling counts and a tool, but every shuttle is still drawn as the same small slab. Players cannot tell a Mk3 hauler with eight Speed upgrades from a plain Mk1, or a harvester from a picker, on the rails or in their inventory.

## What Changes

- A shuttle is drawn from parts: the tier's body and hold (Mk1 an open wooden crate, Mk2 a closed iron hold, Mk3 a closed diamond hold), drive parts that change with the Speed count, a Handling part that changes with the Handling count, and the fitted tool on the front and sides (the Harvester gets a reel that reaches out past the body).
- The same composed shuttle shows on the rails and as the item in inventories, the garage and the workshop.
- Shuttles are drawn about twice as big as before (a 12 pixel wide body at full block scale instead of a 0.6 scale slab), staying inside the rail's block so they do not clip racks; the tool may stick out sideways.
- Cargo shows on top only while the hold is open (Mk1); Mk2 and Mk3 holds are closed and show no cargo.
- The body keeps a free spot on its back for the name plate that `m10-shuttle-build-minigame` engraves.

## Capabilities

### New Capabilities
- `storage/shuttle-looks`: how a shuttle looks from its tier, counts and tool, on the rails and as an item.

### Modified Capabilities
- `storage/shuttles`: "Shuttles are drawn moving" now draws the shuttle's own look and shows cargo only on open holds.

## Impact

- Client code in `storage/client`: a composing item model for the three shuttle items, part models registered as standalone models, `ShuttleGarageRenderer` drawing at the new size.
- Pure look rules (which part variant a count shows) in `storage/shuttle/module`, with JUnit tests.
- Datagen: part models under `models/shuttle/`, base item models for the three tiers; placeholder textures.
- No server, network or data format change: the synced shuttle item from `m10-shuttle-tiers` already carries tier, counts and tool.

## Non-goals

- The name plate itself and the build by hand (`m10-shuttle-build-minigame`).
- Animations beyond what exists (moving parts, opening lids at stops, spinning reels).
- Final art: textures are placeholders until the family's art direction is decided (PLAN.md section 5).
