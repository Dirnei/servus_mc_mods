# Proposal: m10-shuttle-build-minigame

PLAN.md milestone M10 (Field work), next item 3: `m10-shuttle-build-minigame`.

## Why

`m10-shuttle-tiers` builds Mk2 and Mk3 in the Shuttle Workshop with one click, and `m10-shuttle-looks` makes every shuttle show its parts. What is missing is the hands-on part the user asked for: building a shuttle yourself, and a reason to do it. Decided in explore on 2026-10-11: the build by hand is a short minigame that cannot fail and can always be skipped, and only a build by hand engraves a name plate. Building at once stays instant, so playing is a cosmetic reward and never a must for a fleet.

## What Changes

- Once the supplies of a build sheet are complete, the workshop offers **Build now** (unchanged, instant) and **Build by hand**.
- **Build by hand** runs the sheet's steps one after another on a large view of the shuttle in the workshop GUI. Three step kinds: **bolts** (click glowing spots in order), **wires** (connect contacts of the eight duct colours), **weld** (drag along a seam). No step can fail; a wrong click does nothing. The step list per build sheet comes from `storage/shuttle.json` (examples: 4 bolts, 4 wires, a seam, 3 bolts, about 20 seconds in all).
- While a build by hand runs, the shuttle and the supplies are locked in the workshop and no upgrade goes in or out. Progress is kept in the workshop when the GUI closes and survives a restart; **Cancel** ends the build and nothing is used. Breaking the workshop drops everything and the progress is lost.
- The server owns the step order: the client plays a step and reports it done, the server only accepts the step that is next.
- After the last step the GUI asks for a name. **Engrave** finishes the build like Build now and engraves the name as a **name plate**, a new data component on the shuttle (not the anvil name). An empty name finishes the build and keeps the shuttle's old plate.
- A plate is kept through later builds and replaced by the next build by hand. Only builds make plates, so only Mk2 and Mk3 shuttles carry one; a Mk1 from the crafting grid never does.
- The plate shows on the shuttle in the world (text on the free side spot of the hold that `m10-shuttle-looks` kept), in the item's name (so in tooltips and the garage GUI), and in the Field Head while that shuttle works the field.
- Installing an upgrade no longer resets build progress; a build only resets when it finishes or is cancelled.

## Capabilities

### New Capabilities
- `storage/shuttle-hand-build`: the build by hand, its steps from the stats file, locking and progress, the name plate and where it shows.

### Modified Capabilities
- `storage/shuttle-tiers`: Tier builds offer Build by hand next to Build now; the build sheet has steps.
- `storage/shuttle-looks`: the free name plate spot is on the side of the hold and carries the plate text.

## Impact

- Storage code: `ShuttleTiers` (steps per tier), `ShuttleStatsReader`, new pure `HandBuild` step logic, `ShuttleWorkshopBlockEntity` (build state), `ShuttleWorkshopMenu` (buttons, data slots), `ShuttleWorkshopScreen` (work panel), two client to server payloads and one server to client payload in `ServusStoragePayloads`, a `shuttle_name` data component, `ShuttleItem` name, `ShuttleGarageRenderer` plate text, `FieldHeadMenu` and `FieldHeadScreen`, `ShuttleFleet` and `WarehouseNetwork` (who works a field).
- Data: `storage/shuttle.json` gets `steps` per build sheet. Lang through datagen.
- No new items or blocks. Unreleased, so nothing to migrate.
