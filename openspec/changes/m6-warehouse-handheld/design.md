# Design

## Context

Today a zone is a `ZoneDefinition` (name plus `ZoneFilter`) copied onto each `StorageNodeBlockEntity` by `ZoneLabelItem.useOn`. `WarehouseZones` maps bay positions to those copies and feeds `PutAwayPlan` tiers; `StorageFill`, the controller screen, the terminal's Storage tab and `/servus_warehouse zones` group bays by the zone's lower cased name. Fluid Racks are stamped per section through `FluidRackBlock.stampSection`.

Foundation already has the pattern for team data the client must see: duct channels (`DuctChannelData` as overworld `SavedData` with a codec, pure `ChannelList` with name checks, `ChannelSync` broadcasting the list on join and on every change, `Teams.teamOf(player)` for the team id). Zones follow the same shape.

## Goals / Non-Goals

**Goals:**
- Zones as team data that bays reference by id, edited in one place.
- One handheld item with a mode framework that `m11-handheld-link` extends with LINK without rework.
- Feedback that feels like a scanner: sound, flash, scan line, action bar, HUD panel.

**Non-Goals:**
- No migration of label stamped zones (decided with the user; Zone Labels are removed outright).
- No custom first person rendering of the screen onto the item model; the screen is a HUD panel.
- No change to the Data Potato.

## Decisions

### Team zones mirror duct channels
`zone/ZoneList` (pure: id, team UUID, name, colour index 0 to 15, filter) with name rules 1 to 24 characters, unique per team ignoring case, ordering by name, text search and `resolve(id)` that turns an unknown id into "no zone". `zone/WarehouseZoneData` is overworld `SavedData` (`SavedDataType` with a codec, key `servus_warehouse_zones`), the only place that mutates the list, and `zone/ZoneSync` sends the full list to a joining player and to everyone after each change, held client side in `ClientZones`. The filter is stored as the existing `ZoneFilter` (ghost items and tags), so `PutAwayPlan` and the Fluid Rack fluid rule stay unchanged.

*Alternative:* keep stamped copies and add "restamp all bays of zone X". Rejected: copies drift, and the renaming problem is the reason for the change.

*Name length:* 24 like channels instead of the label's 32, so names fit the beam next to the code sticker.

### Bays hold a zone id
`StorageNodeBlockEntity` stores an `int` zone id (0 = none) instead of a `ZoneDefinition`, saved as `ZoneId`. The old `Zone` tag is not read, which drops label stamped zones on load. Display and filter are resolved through `WarehouseZoneData` on the server and `ClientZones` on the client. `WarehouseZones` keeps `pos -> id` and resolves the definition when it builds put-away tiers; grouping keys in `StorageFill`, `ProductionRows` and the command become the zone id as text, with the resolved name for display.

Editing a zone posts `WarehouseEvent.ZonesEdited` to every warehouse network of every loaded dimension (through Foundation's `NetworkManager` network sets), drained in the next tick like any other event, so tiers and storage rows refresh without polling. Client side, beams re-render from `ClientZones` because the rack renderer reads the name and colour each frame.

### Location codes are computed on the server and pushed to bays
`zone/LocationCode.of(dx, dy, dz)` is pure (`N`/`S` from `-dz`/`dz`, `E`/`W` from `dx`, zero as `N0`/`E0`, signed height, `+0` for zero). `WarehouseNetwork` knows `controllerPos`; whenever the controller joins or leaves, or a rack bay joins the network, the network sets each affected bay's code (an empty string without a controller) and the bay syncs it in its update tag. This is event driven and costs nothing per tick. The handheld reads the aimed bay's code from the client block entity, so the panel's top line needs no request.

*Alternative:* compute codes on the client from the controller position. Rejected: the client does not know which controller a bay belongs to.

### Handheld item and mode framework
`handheld/HandheldItem` with data components for mode (enum `HandheldMode`: ZONE, FIND, INFO; LINK is added by `m11-handheld-link`), active zone id, bound controller (dimension + pos), FIND target item and pinned bay. The item id is `servus_shuttles:handheld` and its name "Handheld HH-1", so later models (`HH-2`, `HH-3`) can be separate items sharing the class.

Mode switching: client `InputEvent.MouseScrollingEvent` (cancellable, NeoForge `net.neoforged.neoforge.client.event.InputEvent`) is cancelled while the player sneaks with the handheld in the main hand, and a `HandheldModePayload(step)` asks the server to change the component. The server owns the state.

### Scanning uses vanilla held-use repeat
Right click on a bay goes through `HandheldItem.useOn`. Vanilla repeats `startUseItem` every 4 ticks while the use key is held (`Minecraft.rightClickDelay = 4`, checked in 26.1.2 sources), so a sweep needs no custom input: each repeat lands on whatever bay the crosshair is on. The server tracks per player the last scanned bay and the sweep count (reset after 10 ticks without a scan), skips a repeat on the same bay silently, and sends a `HandheldScanPayload(positions, ok, sweepIndex)` that plays the beep at a pitch rising with `sweepIndex` and starts the flash. Section scans use `HeavyRackBlock.section`, Fluid Racks keep `FluidRackBlock.stampSection` reworked to take a zone id.

`HeavyRackBlock.useItemOn` and `FluidRackBlock` pass for the handheld (replacing the `ZoneLabelItem` check), so the rack GUI never opens and `useOn` runs. `controller/WarehouseControllerBlock` does the same for binding.

### HUD panel, scan line, flash and glow are client renderers
- Panel: a GUI layer registered with `RegisterGuiLayersEvent` (as `PlacementAssistHud` does), drawn while the handheld is in either hand, styled as the green LCD.
- INFO data: the panel asks the server with `HandheldInfoRequest(pos)` when the aimed bay changes, at most every 10 ticks, and the server answers with zone id, slots used and total, and the top items. Code and zone name come from the client block entity.
- Scan line, flash and FIND glow: `RenderLevelStageEvent.AfterTranslucentBlocks` (as `AnchorRangeRenderer` does). The flash is a fading box (`RenderTypes.debugQuads()`) standing 0.05 px off the bay's faces, so the bay's own opaque faces do not hide it, and every further bay flashed at once one more 0.005 px out, so neighbouring bays of a section never share a plane; the glow is an outline of crossed bands along the twelve edges drawn with `RenderTypes.textBackgroundSeeThrough()` (no depth test in 26.1.2) for the glow time; the scan line is a thin red quad from the hand offset to the hit point while use is held.
- FIND list and bay counts: opening the GUI sends `HandheldMenuStatePayload` with the loaded bays per zone id and the bound warehouse's stock (one stack per item with its count, largest first, from the warehouse index); it is sent again after every create, edit and delete. No separate stock request. Picking an item sends `HandheldFindPayload(item)`, and the server answers with `HandheldFindResultPayload` (item, total, loaded bay positions holding it and their codes, glow ticks).
- The server side of the handheld (`handheld/HandheldActions`) takes any `Player`, not only a `ServerPlayer`, and sends payloads only to a `ServerPlayer`, so GameTests drive it with mock players.

### Beam stripe and code sticker
`HeavyRackRenderer` draws the label through `client/RackBeamLabel`. The rack's top front beam is only 1 px tall (y 15 to 16 between the posts), so the label is laid out around it: the zone colour stripe is a thin box over the beam's front between the posts (z 0.005 to 0.1 px out); the zone name lies below the stripe, cut with an ellipsis, left of the sticker; the code sticker is a white box hanging from the beam on the right (x 1 to 6 px, y 13.2 to 15.6 px, z 0.105 to 0.2 px), 0.005 px in front of the stripe it overlaps; its barcode bars are thin dark boxes 0.005 px in front of the sticker, maximal runs so no two bars touch, hashed from the code text by the pure `zone/Barcode` (JUnit `BarcodeTest`); the code text lies below the bars. Boxes use a plain white block texture (`textures/block/zone_sticker.png`) tinted per part. No two faces share a plane where they overlap. `FluidRackRenderer` keeps its zone name (through `client/ZoneText`, the renamed `ZoneLabelText`) and gains no sticker.

### Recipe and look
Shaped recipe in datagen: iron ingots for the body, yellow dye for the bumper, a glass pane for the screen, redstone and red dye for the scan window (numbers and items in the datagen recipe, not code). Sounds are vanilla events at another pitch in `sounds.json` (`handheld.beep` note block bit, `handheld.buzz` didgeridoo, `handheld.click` stone button), played on the client from the scan and FIND answers. A 16x16 item texture: dark steel body, yellow bumper corners, green screen, red top window.

### Settles from PLAN.md
- "Zone Labels" (M6) is replaced by the handheld and team zones.
- "Wireless Terminal with range tiers" (Open: Shuttles) stays open but now names the handheld's LINK mode as its home; PLAN.md is updated at archive.

### Pure vs Minecraft
Pure with JUnit: `ZoneList` (names, uniqueness, ordering, search, resolve), `LocationCode`, the sweep tracker (same bay skip, count reset). Minecraft side with GameTests: assigning a bay, section assign, Fluid Rack section, zone edit changing put-away, delete unzoning bays, old `Zone` tag loading unzoned, location code after controller moves. Manual play check: panel, sounds, sweep feel, flash, glow, beam stripe and sticker without flicker.

## Risks / Trade-offs

- [Removing the Zone Label breaks worlds that used it] → Accepted with the user; the changelog says zones must be set again.
- [Held-use repeat at 4 ticks may feel slow on a fast sweep and skip a bay] → The sweep checks the crosshair target at each repeat; if play tests show skipped bays, the client can send the bays between the last and the current hit along the row.
- [Broadcasting `ZonesEdited` to every warehouse network] → Edits are rare player actions; each network only rebuilds its zone tiers.
- [Codes change when the controller moves] → Intended; the code is relative by design.
- [Text on the beam gets crowded with a long zone name and a code] → Names are capped at 24 characters and cut with an ellipsis on the beam; the full name is in the GUI title and the panel.

## Migration Plan

No data migration. On load, racks ignore the old `Zone` tag and the Zone Label item is unregistered, so stacks of it are dropped by the game. Rollback is not supported for worlds saved with team zones on an older version (they would load unzoned there too).
