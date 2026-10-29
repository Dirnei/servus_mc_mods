# Tasks

## 1. Pure logic

- [x] 1.1 Add `shuttles/.../zone/ZoneList` (id, team, name, colour, `ZoneFilter`; name 1 to 24 characters unique per team ignoring case, ordering by name, text search, `resolve` turning an unknown id into no zone). JUnit `ZoneListTest`: name too short, too long, taken ignoring case, same name in another team allowed, ordering, search, resolve of a deleted id. Verify: `gradlew :shuttles:test` green.
- [x] 1.2 Add `shuttles/.../zone/LocationCode` (`of(dx, dy, dz)` to `N12.E5+2`). JUnit `LocationCodeTest`: north east up, south west down, `N0`/`E0` zeros, `+0`, negative height. Verify: `gradlew :shuttles:test` green.
- [x] 1.3 Add `shuttles/.../handheld/SweepTracker` (per player last bay and sweep count, same bay skipped, reset after a quiet gap). JUnit `SweepTrackerTest`: rising index across bays, repeat on the same bay skipped, reset after the gap. Verify: `gradlew :shuttles:test` green.

## 2. Team zones on the server and client

- [x] 2.1 Add `zone/WarehouseZoneData` (overworld `SavedData`, codec, key `servus_warehouse_zones`, create/edit/delete), `zone/ZoneSync` and `client/ClientZones` (full list on join and after every change), payloads registered in `terminal/ServusShuttlesPayloads`. Verify: `gradlew build`, GameTest that a created zone survives a save and load of the data.
- [x] 2.2 Switch `network/StorageNodeBlockEntity` to a zone id (`ZoneId` tag, old `Zone` tag ignored), resolve name and filter through the zone data, keep the zone through the carrot dismantle. Verify: `gradlew build`, GameTests in `gametest/ZoneGameTests` for dismantle keeping the zone and an old `Zone` tag loading unzoned.
- [x] 2.3 Rework `network/WarehouseZones`, `storage/StorageFill`, `production/ProductionRows`, `client/WarehouseControllerScreen`, `client/InboundStationScreen` and `command/ServusShuttlesCommands` (`zones`) to group by zone id and show the resolved name; add `WarehouseEvent.ZonesEdited` posted to every warehouse network on create, edit and delete. Verify: `gradlew build`, GameTests: editing a zone's filter changes put-away at once, deleting a zone unzones its bays, existing put-away routing tests stay green.

## 3. Location codes

- [x] 3.1 In `network/WarehouseNetwork` set each bay's code when the controller joins or leaves and when a rack bay joins; store and sync it on `StorageNodeBlockEntity`. Verify: `gradlew build`, GameTest: a bay reads `N8.E0+0`, then `N8.W1+0` after the controller moves one block east, and nothing without a controller.

## 4. Handheld item

- [x] 4.1 Add `handheld/HandheldItem`, `handheld/HandheldMode` and data components (mode, active zone, bound controller, FIND item, pinned bay) in `registry/ServusShuttlesComponents` and `registry/ServusShuttlesItems`; item texture `textures/item/handheld.png`, model, recipe and lang via datagen. Remove `zone/ZoneLabelItem`, `menu/ZoneLabelMenu`, `client/ZoneLabelScreen`, `client/ZoneLabelText`, `zone/SetLabelNamePayload`, `zone/SetLabelTagsPayload`, the label's registrations, recipe, model, texture and lang. Verify: `gradlew build`, `runData` output committed, no `zone_label` left in resources.
- [x] 4.2 Sneak + scroll mode switching: client `InputEvent.MouseScrollingEvent` handler in `client/`, `HandheldModePayload`, server applies the step with wrap around and a click sound in `registry/ServusShuttlesSounds`. Verify: `gradlew build`, in game: sneak scroll cycles ZONE, FIND, INFO and the hotbar slot stays.
- [x] 4.3 `HeavyRackBlock.useItemOn`, `FluidRackBlock` and `controller/WarehouseControllerBlock` pass for the handheld instead of the Zone Label. Verify: `gradlew build`, GameTest: handheld on a rack front opens no menu.

## 5. ZONE mode

- [x] 5.1 `HandheldItem.useOn` in ZONE: assign or clear one bay, sneak for the section (`HeavyRackBlock.section`), Fluid Rack whole section (`FluidRackBlock.stampSection` on zone ids), action bar `code  before -> after`, "choose a zone" message, deny on same zone, sweep via `SweepTracker`; `HandheldScanPayload` to the client; beep and buzz sounds. Verify: `gradlew build`, GameTests: assign, already in zone, section of three (sections hold three bays per `rack.json`), Fluid Rack section, no active zone.
- [x] 5.2 Handheld GUI (`menu/HandheldMenu`, `client/HandheldScreen`): team zone list with colour, name, filter summary and loaded bay count, No zone entry, choose active zone, create and edit form (name, 16 dye palette, nine ghost slots, tag line, reusing the label screen's ghost slot and tag logic), delete with a second confirm click when bays use it; payloads for create, edit, delete and active zone. Verify: `gradlew build`, in game: create, rename and recolour a zone and watch the beams follow.

## 6. Bind, FIND and INFO

- [x] 6.1 Binding on the Warehouse Controller (component, action bar), not bound text on the panel. Verify: `gradlew build`, GameTest: binding stores the controller, a broken controller reads as not bound.
- [x] 6.2 FIND: GUI tab with the bound warehouse's stock and search (the stock comes with `HandheldMenuStatePayload` when the GUI opens, largest first from the warehouse index), pick or scan a bay to start, server answers loaded bay positions, client glow for the configured time (example 8 s, in `warehouse.json`). Verify: `gradlew build`, GameTest for the server side bay list, in game glow through walls.
- [x] 6.3 INFO: `HandheldInfoRequest` on aim change (throttled to 10 ticks), server answer with zone, slots and top items, right click pins. Verify: `gradlew build`, GameTest for the answer of a known bay.

## 7. Client rendering

- [x] 7.1 HUD panel layer (`client/HandheldHud`, `RegisterGuiLayersEvent`): green LCD with mode, code and the mode's content. Verify: `gradlew build`, in game in both hands.
- [x] 7.2 `client/HandheldRenderer` on `RenderLevelStageEvent.AfterTranslucentBlocks`: red scan line while use is held, fading green or red bay flash 0.05 px off the bay faces, 0.005 px further per further bay, FIND glow outline without depth test. Verify: `gradlew build`, in game from several angles, no flicker.
- [x] 7.3 `client/HeavyRackRenderer`: zone colour stripe, zone name cut with an ellipsis, code sticker with barcode; layers stepped 0.005 px apart; list every overlapping pair in the task note and confirm no shared planes. Verify: `gradlew build`, in game no z-fighting while moving around a zoned rack.
  - Note (front space, pixels in front of the face at z 0; model posts x 0 to 2 and 14 to 16, top beam y 15 to 16, both ending at z 0): stripe box x 2 to 14, y 15 to 16, z 0.005 to 0.1. Stripe and beam: the stripe's back lies 0.005 in front of the beam's front, no shared plane. Stripe and posts: the stripe's side faces lie at x 2 and 14 like the posts' inner faces, but at z 0.005 to 0.1 where the posts (z up to 0) have no face, so they do not overlap. Sticker box x 9 to 14, y 13.2 to 15.6, z 0.105 to 0.2. Sticker and stripe: overlap at y 15 to 15.6; the sticker's back is 0.005 in front of the stripe's front; their y and x faces lie on different planes (sticker 13.2 and 15.6 against stripe 15 and 16; the sticker's right face at x 14 meets the stripe's right face only where the sticker stands in front of it, z ranges disjoint). Sticker and posts: the sticker's right face at x 14 lies in front of the post (z ranges disjoint). Bars z 0.205 to 0.215 over y 14.4 to 15.35, 0.005 in front of the sticker; bars are maximal runs, so neighbouring bars never touch. Zone name text at z 0.064, y 13.9 to 14.9, x left of 8.8: below the stripe (y under 15) and left of the sticker, overlaps no face. Code text at z 0.22, y 13.5 to 14.3, below the bars (y from 14.4) and in front of the sticker front (0.2). No two faces share a plane where they overlap.

## 8. Docs and finish

- [x] 8.1 Shuttles `CHANGELOG.md` `## Unreleased`: Added handheld, team zones and location codes; Removed Zone Label, label stamped zones are lost and need the handheld. Verify: file reviewed.
- [ ] 8.2 Full gradle build, tests green, game launches with all mods; play check of sweep, section, FIND glow, INFO panel, beams and zone edits. Verify: `gradlew build` and `gradlew test` green, client starts from main.

## Workflow follow-up

- At archive: update the Purpose of `openspec/specs/shuttles/zones/spec.md` (it still names the Zone Label), groom PLAN.md (Zone Labels in the M6 row, the Wireless Terminal open point now pointing at the handheld's LINK mode in `m11-handheld-link`).
