# Design

## Context

Shuttles are data on the warehouse network (`ShuttleFleet`), not entities. Clients draw them from `ShuttleView` records in `ShuttleGarageRenderer`, which turns each shuttle so the model's +z faces its heading and submits the composed shuttle item (`RenderSubmit.item`, `ItemDisplayContext.NONE`) at 0.8 scale.

What the server already knows about a standing shuttle:
- traffic: `room`, `leader`, `leaderSpeed`, `inRing()` per tick (`ShuttleFleet.follow`), synced through `followKey` when it changes;
- the segment gate: `ShuttleMotion.advance` asks `pay(...) && lifts.mayStart(...)`; a refusal leaves `motion.held()` true, but not why (power, lift car or deck);
- `stop`, `handover`, `riding`, `waitsAtDoor`, `nextLoaded`, field and felling runs.

Clients get `Drive.STILL` for any standing shuttle and a `Lead` for a follower, so they cannot tell a lift wait from a power wait or a stop.

The Data Potato (`foundation/item/DataPotatoItem`) calls `PotatoInspectable.potatoReport` on the clicked block on the server and prints the lines; blocks let it through by returning `PASS` from `useItemOn`. Rails (`RailBlock`, `FarmRailBlock extends RailBlock`) and elevator segments are `StorageNodeBlockEntity`s with `network()` to reach the `WarehouseNetwork` and its fleet.

## Goals / Non-Goals

**Goals:**
- One server-side answer to "why does this shuttle stand", used by both the lights and the potato.
- Lights and plates cost no extra syncs while shuttles drive.

**Non-Goals:**
- Changing traffic or lift behavior. The reason is observed, never acted on.

## Decisions

### D1 A pure `StandReason` decided on the server
New pure enum and record in `shuttle/`: `StandReason` with kinds `DRIVING`, `FOLLOWING` (held behind a shuttle, leader id), `RING` (moves with a ring), `STOP`, `FIELD_WORK`, `DOOR`, `HANDOVER`, `RIDING`, `LIFT` (shaft bottom, floor), `DECK` (shaft bottom, floor), `POWER`, `CHUNK`. `ShuttleFleet` works it out per shuttle after `traffic()` from the existing fields, in a fixed priority (restore, stop, field run, door, handover, riding, chunk, gate reason, followed, driving).

The gate reason is the only new state: `Shuttle.gate` is set where a gate refuses (`pay` returns false: `POWER`; `ShuttleLifts.mayEnterRide` and the not-boarded branch of `startRide`: `LIFT`; `mayEnterCross`: `DECK`) and cleared when `advance` passes the gate. Floor = segment y minus the car's `bottomY()` plus 1.

Alternative: derive everything on the client from the existing view. Rejected, the client cannot tell the gates apart and the potato needs a server answer anyway.

### D2 Sync the reason kind, let clients see braking themselves
`ShuttleView` gets one field `signal` (a `VAR_INT`, the kind's ordinal). `StreamCodec.composite` in 26.1.2 goes up to 12 fields (checked in `minecraft-patched-26.1.2.116-sources.jar`), the view has 10, so it becomes 11. The fleet keeps `shuttle.signalShown` next to `followShown` and sets `syncPending` when the kind changes, so the lift, power and deck waits that leave `held()` unchanged still sync.

Braking is not sent. The client compares the glide speed now with the speed one tick earlier (`ShuttleGlide.at` at `elapsed` and `elapsed - 1`, both from `ShuttleDrive.glide`), so braking for a stop, a corner or a leader lights up between syncs on every client alike.

### D3 Pure `TailLight` mapping
`shuttle/TailLight.of(kind, braking, gameTime)` returns `OFF`, `DIM_RED`, `RED` or `AMBER` (with blink phase off as `OFF`). `DRIVING` and `RING` give `DIM_RED`, or `RED` while braking; `FOLLOWING` gives `RED`; `LIFT`, `DECK`, `POWER`, `CHUNK` give `AMBER` for 10 ticks then `OFF` for 10; `STOP`, `FIELD_WORK`, `DOOR`, `HANDOVER`, `RIDING` give `OFF`. The blink is a look constant in code, not balance.

### D4 Lamps and plate drawn by the renderer in shuttle space
After the shuttle item, still in the shuttle's pose, `ShuttleGarageRenderer` submits:
- two headlight boxes on the front face and two tail light boxes on the back face of the body, each about 1.5 x 1 px, standing 0.05 px off the body face so no face shares a plane with the body or the hold. Lit faces use `LightCoordsUtil.FULL_BRIGHT`; an `OFF` tail light uses the world light with a dark red tint, so the housing still reads as a lamp.
- a dark steel plate box on each long side, 0.05 px off the body, with `RenderSubmit.text` on its outer face 0.01 px in front of the plate (sign style, no shadow). Plate width = text width plus 1 px margin; text scale shrinks so `#` plus three digits fit the body length.

Lamp and plate positions depend on the tier body (Mk1 crate, Mk2 and Mk3 holds, Lumber hold) and on tools at the front (Harvester reel) and back (Spreader hopper, spoiler). They live in one pure table `shuttle/SignalLayout` keyed by tier and tool, in model pixels, with a JUnit test that the lamps stay inside the block and clear of the tool boxes from `ShuttleLook`.

Alternative: put lamps and plates into the composed item model. Rejected, the item would show them in every slot (the user wants world only), the number would need per-item baked text, and the item model cannot glow per state.

### D5 Plate text only within 16 blocks
The renderer measures the camera distance to the shuttle and submits the text only within 16 blocks (squared distance check), like sign text. The plate box is always drawn so its look does not pop.

### D6 Potato report in `ShuttleReport`
`RailBlock` and `ElevatorBlock` implement `PotatoInspectable`; `ElevatorBlock.useItemOn` returns `PASS` for the Data Potato before opening its menu. `potatoReport` asks the node's `network()` for the fleet and calls `ShuttleReport.at(fleet, pos)`, which lists every shuttle whose current node is that block or that drives from it to the next node, plus the rider for any segment of a shaft whose car is at that height. Each shuttle gets its label line and its reason line. For `FOLLOWING` it walks the leader ids (max 8), naming each leader and its reason, and ends with "wait for each other" when an id repeats.

The chain walk is pure (`StandReason.chain(start, leaderOf, limit)` returning the ids and whether it closed a cycle) and unit tested. Message keys `message.servus_shuttles.potato.shuttle_*` in `ServusShuttlesLanguageProvider`.

### D7 PLAN.md
No PLAN.md decision is settled here. The change adds under "Open: Shuttles" the deadlock it was found with: a shuttle crossing a shaft keeps the car from folding that deck while the shuttle it waits behind waits for the car.

## APIs used

- `SubmitNodeCollector.submitText` through Foundation's `RenderSubmit.text` (already used in the repo); `LightCoordsUtil.FULL_BRIGHT` (used in `CalcinerRenderer`, `HollowRevealRenderer`).
- `StreamCodec.composite` with 11 parts (`net.minecraft.network.codec.StreamCodec`, 26.1.2 sources).
- Foundation's `PotatoInspectable` and `DataPotatoItem`.

## Pure Java and tests

- Pure, JUnit: `StandReason` chain walk, `TailLight.of`, `SignalLayout` bounds.
- Minecraft: reason bookkeeping in `ShuttleFleet` and `ShuttleLifts` (GameTest: a shuttle waiting for the car reports `LIFT` with the right floor; one behind a stopped shuttle reports `FOLLOWING`), the potato on a rail (GameTest on the report lines), renderer (play check).

## Risks / Trade-offs

- [More syncs when reasons flicker, for example a gate refusing on alternate ticks] → the kind only changes when the shuttle really changes state; watch the sync count in the play check with a busy shaft.
- [Lamps collide with tools or the spoiler on some combination] → `SignalLayout` test against `ShuttleLook` boxes, and the play check walks every tier with Harvester, Spreader, Lumber and the top Speed step.
- [Text in the world on many shuttles costs frames] → the 16 block cut-off; plates are one box each.
- [`ShuttleView` gains a field] → client and server ship in one jar; no save data changes.

## Decisions made while applying

- The reason is worked out at the end of the fleet tick, after every shuttle moved, not right after `traffic()`: the gate refusals and stops of this tick are known only then. The order follows D1; a shuttle settling after a restore counts as `CHUNK`, a busy shuttle standing at its path end outside a stop counts as `STOP`.
- `FOLLOWING` means held back: the shuttle ahead makes it stand or drive slower than it could (the same test that puts it into the ring check, recorded as `Shuttle.heldBy`), not merely a leader within the window. A follower that leaves the lift car without waiting is not held.
- The gate reason is reset at the start of every gate question and set by the refusing gate; it counts only while `motion.held()`. A missing car on a ride or crossing also reports `LIFT` or `DECK`.
- Braking is pure: `ShuttleDrive.braking` compares the step of the tick the shuttle drives now with the step before, both from `glide`; `ShuttleGlide.braking` feeds it the view. The tests live in `ShuttleDriveTest`.
- `ShuttleLook` has no part boxes, so `SignalLayoutTest` keeps the outlines of body, drives, spoiler, Handling parts and tools mirrored from `ShuttleModels` (a turned spoiler strut as its bounding box) and takes the Lumber hold from `HarvesterParts.hold`.
- Placement: headlights x 3.25 to 4.75 and 11.25 to 12.75, tail lights x 4.05 to 5.45 and 10.55 to 11.95 (beside the Handling mast at x 3 to 4 and the spoiler struts at x 5.5 and 9.5), both at y 7.5 to 8.5, above the Harvester reel. A Lumber shuttle's walls fold away at a tree, so its lamps sit on the chassis (y 2.4 to 3.4, below the folded walls) and its plates hide while it stands unfolded. A Spreader's hopper hides the back and reaches outside the block, so its tail lights stand on the hopper rim (y 12.05 to 13.05, z -1.95 to -1.45). Plates: y 5 to 8 (Lumber 6.5 to 9.5, clear of its wall strips), 0.3 model pixels per text pixel, at most 10 pixels long.
- The number stands 0.05 px in front of its plate, not 0.01 px: at 16 blocks the depth buffer cannot tell 0.01 px apart.
- Lamps are boxes with the white concrete texture tinted per state; the plate uses the heavy rack iron texture, both drawn from their own corner so the texture never runs past the sprite.
- `ElevatorBlock.useItemOn` already passes every item through, so the potato needed no change there; `RailBlock.useItemOn` passes the Data Potato explicitly, which Farm Rails inherit.
- The report has one line per shuttle ("Shuttle #3: Waiting behind Shuttle #5"), then one "then" line per shuttle along the chain with its own reason, and a cycle line when the chain comes back. A rail that is not part of an online warehouse says so.
- `GameTestHelper.relativePos` turns positions by 180 degrees for unrotated tests in 26.1.2, so the GameTests work out relative positions themselves.
