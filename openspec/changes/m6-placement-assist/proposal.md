# Proposal

## Why

Laying long straight runs of Warehouse Rail or Utility Duct means walking along the run and clicking the last block every time. Bridging in the air means aiming at a small rail end from awkward angles. Create solves this for shafts: clicking anywhere on a run extends it at its far end and shows a ghost with an arrow first. We want the same for rails and ducts. The component lives in Servus Lib so every straight-run block can use it.

This implements a new item under PLAN.md milestone M6 (Storage automation), "Sneak placement assist for rails and ducts". It goes ahead of the pick rate stats screen, the last open M6 item.

## What Changes

- New Lib component: while sneaking with an assisted item and looking at a block of the same run, the block is placed at the first free spot past the end of the run in a chosen direction instead of against the clicked face. A ghost of the block and an arrow show the target before the click.
- The assist is sneak only. Unsneaked placement stays exactly as it is, because rails and ducts turn corners all the time.
- The walk has a maximum reach from a new Lib config option (for example 16 blocks). If the walk ends on a block that cannot be replaced, or the reach runs out, nothing is shown and the click places nothing.
- Warehouse Rail: the direction comes from where the top or bottom face was hit (nearest edge). A hit on a side face uses that face. The run is the chain of rails in that direction. The placed rail follows the normal stand, hang and gap rules, so a rail that would not hold drops as it always does.
- Utility Duct: the direction is the clicked face. The run is the chain of ducts that carry the held line. Sneak clicking a duct that lacks the line still places a new duct against the clicked face when that spot is free, as today.
- PLAN.md: add the item to M6 and the placement assist to the Lib API sketch.

## Capabilities

### New Capabilities
- `lib/placement-assist`: sneak-only straight-run placement shared by Servus blocks. Covers the run walk, the reach limit, the ghost and arrow preview, and server authority over the target.

### Modified Capabilities
- `storage/warehouse-rail`: the Warehouse Rail item uses the placement assist, with its direction taken from the face quadrant.
- `logistics/utility-duct`: the "Channels are placed as items" requirement changes its sneak behavior. A sneak click walks the run of the held line before placing.

## Impact

- `lib`: new `placement` package (pure walk and direction logic with JUnit tests, the item hook), a client renderer for the ghost and arrow, and a new option in `ServusLibConfig`.
- `storage`: the rail item becomes an assisted block item instead of a simple block item (`ServusStorageItems`).
- `logistics`: `LineItem` takes part in the assist.
- No new blocks, models, textures or recipes. No network payloads, because placement already runs on both sides and the server result is authoritative.

## Non-goals

- Elevator segments, racks or any other block: they can adopt the assist later.
- Placing several blocks per click, or filling a whole gap in one go.
- Changing the rail gap rules or giving assisted placement any special mid-air handling.
- A key binding or toggle other than sneaking.
