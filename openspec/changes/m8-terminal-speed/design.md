# Design

## Context

See proposal.md for the measured causes. Measurement (GameTest `TerminalSpeedGameTests`, one warehouse crate, a furnace behind a Machine Adapter, a CPU, both terminals on one data line): Counter Terminal `stock()` about 1.2 ms per call, of which `TerminalCraftables.of` about 0.4 ms; Production Terminal view about 0.15 ms; on the real open path neither terminal sent anything (`firstSyncTick` -1).

Vanilla 1.21.1 open path (NeoForge 21.1.255 sources): `ServerPlayer.openMenu` sends the open screen packet, then `initMenu` calls `menu.addSlotListener` (which runs `broadcastChanges`) and `menu.setSynchronizer`, which calls `sendAllDataToRemote`. `broadcastFullState` only runs on a resync. The open screen packet is sent before `initMenu`, and payloads are handled on the client main thread in order, so a payload sent from `sendAllDataToRemote` reaches a screen that already exists.

## Goals / Non-Goals

**Goals:** content on the client in the tick the screen opens; no flash of "Loading..." for a terminal seen before; no rebuild of the slow parts when nothing changed.

**Non-Goals:** event push to open screens; caching production rows (cheap, change per step); the Production Terminal redesign.

## Decisions

### Send on open
Both menus override `sendAllDataToRemote()` and call one private `sendNow()` that builds the payload, remembers its signature, resets the sync timer, records `firstSyncTick` and sends. `broadcastFullState()` calls the same. `sendAllDataToRemote` runs once from `setSynchronizer` on open and again only on resyncs, so no double send.

### Client cache per terminal
`StockSyncPayload` and `ProductionPayload` carry the terminal's `BlockPos`. `ClientStock` and `ClientProduction` keep a map from position to the last content (session lifetime) plus the position of the open screen. The screens call `ClientStock.open(pos)` / `ClientProduction.open(pos)` instead of `clear()`: the open content becomes the cached one for that position (or none), the craft preview is cleared. `update(payload)` stores by position and, when it is the open terminal, replaces the shown content and bumps the version. `loaded()` means "content for the open terminal exists". The maps are cleared on `ClientPlayerNetworkEvent.LoggingOut`. Positions are per dimension in practice; the key includes the dimension to be safe.

Alternative considered: delay the first send until the client acknowledges the screen. Rejected: the open order already guarantees the screen exists, and the cache is needed anyway for repeat opens.

### Server view cache
- `WarehouseIndex` gets `version()`: `update` compares the new snapshot with the old one and does nothing when equal (batched polling re-reports unchanged inventories all the time); a real change or `remove` bumps the version. `entries()` caches its sorted list per version. Pure, JUnit tested.
- `TerminalWarehouses.stock(warehouses)` caches the merged, sorted list in a small identity keyed cache: key is the list of warehouse identities plus their index versions. A hit returns the cached list.
- `TerminalCraftables.of` caches per data network (weak identity map) with a key built from the producers: for each producer its position and a signature of its recipe list. Large recipe lists (from `SingleInputRecipes`, cached per recipe manager) are returned as the same list instance, so their signature is the identity hash plus size; small lists (Crafting Stations from patterns, Assemblers) use a content signature of recipe ids and first output item and count. A recipe reload yields new list instances, a saved pattern changes the station's list content, a member joining or leaving changes the producer set: each changes the key. A hit skips the map building and the name sort.

Alternative considered: revision counters on DataNetwork and Pattern Storage. Rejected: an Assembler changing its configured recipes would not bump them; the producer signature covers every source with one rule.

### Tests
- JUnit `WarehouseIndexTest`: unchanged snapshot keeps the version, a change or removal bumps it, entries are the same list instance while the version holds.
- GameTests `TerminalSpeedGameTests`: `counterSendsOnOpen` and `productionSendsOnOpen` open through `setSynchronizer` and assert the content went out in that tick; `craftablesCachedOnLine` asserts the second build on an unchanged line returns the cached list and a saved pattern or a removed adapter changes it. `TerminalCraftGameTests.terminalSendsOnOpen` switches to the real open path.
- Client cache: checked in game (client classes are not loaded by GameTests).

### PLAN.md
No open decision is touched.

## Risks / Trade-offs

- [A provider that returns a new large list on every call] -> its key changes each time, so it is rebuilt each time: correct, only not faster. The two large providers today return cached lists.
- [Cached content from a terminal whose line changed while closed] -> shown for one moment until the fresh content replaces it; acceptable and expected from a cache.
- [`WarehouseIndex.update` equality check costs a map compare] -> cheaper than the remove and re-add it skips.
