# Design

## Context

Builds on `m8-packing-station` (must be archived first): deliveries are instant, `Shipment` moves per tick bounded by source, target and `TransferGate` allowances from pure `RateBudget`s; the Packing Station is a gate. The data line is `Line.DATA` of the Utility Duct (`foundation.logistics.duct`), its network is `DataNetwork` with `DataNode` per duct and `DataMembers`; ducts render from part models with a colour per line; the goggles overlay (`DuctOverlayRenderer`) already draws hidden ducts. See proposal.md for the why.

## Goals / Non-Goals

**Goals:**
- Cable tiers as a visible, buildable throughput lever, combined with the station rate by a simple minimum.
- Upgrade items in Foundation usable by any Servus block.
- Load is visible on the duct, with the goggles and in the Production Terminal.

**Non-Goals:**
- Max-flow routing, fairness beyond creation order, bandwidth for other lines.

## Decisions

### Tiers as data on the data line
- `Line.DATA` stays one line; the duct block entity saves the data line's tier id (`servus_foundation:data_cable`, `duplex_data_cable`, `fast_data_cable`). Each tier is its own line item registered in Foundation, all placing `Line.DATA`.
- Tier stats come from `data/servus_foundation/data/cable_tiers.json` (bandwidth per second, `duplex` flag), loaded by a reload listener next to `PacketLimitsManager`; an unknown tier falls back to the base tier.
- Rendering: the data line part models get a tint per tier through the existing `DuctBlockColor` (tier from model data); no new geometry.

### Replacement
`LineItem.useOn` on a duct that already carries the data line compares tiers: higher replaces the tier in place (sides, on/off, connections unchanged), consumes one item and drops the old tier's item with `Block.popResource`; lower or equal falls through to the current "carries the line" behaviour. Sneak keeps the placement assist.

### Bandwidth accounting
- Pure `DuctBudget` (no Minecraft imports, JUnit): per duct and per tick a budget from bandwidth with the same fractional carry as `RateBudget`; simplex holds one shared budget, duplex one per direction (direction = from the lower to the higher node position or the reverse along the path). It records used items for load reporting (rolling one second window).
- `DataNetwork` keeps budgets for ducts that carried traffic this tick only (lazy map, cleared each tick), so idle networks cost nothing.
- `Shipment` asks for a path once and again when the graph changes: BFS shortest paths, preferring the one whose minimum tier bandwidth is highest (pure `PathPick` over the graph, JUnit). Per tick amount = min(source, target, gates, min budget along the path); then consumes from each duct budget. Deliveries tick in creation order.

### Upgrades
- New `foundation.upgrade` package: `UpgradeItem` (multiplier from `data/servus_foundation/data/upgrades.json` keyed by item id, default 2 for the Speed Upgrade), `UpgradeSlots` (an `ItemStackHandler` that only accepts `UpgradeItem`s or items in the `servus_foundation:upgrades` item tag, one per slot) and `UpgradeMath.multiplier(list)` (pure, JUnit). Other mods register items as `UpgradeItem` or add them to the tag with their own JSON entry.
- Packing Station: `upgrade_slots` (example 2) in `packing_station.json`; its two `RateBudget`s use `base * multiplier`; the read-only GUI gets a third row of real slots for upgrades and shows the current rate.

### Visibility
- Summary: `DataNetwork` load for the clicked duct (max of directions) appended as "N of M items/s".
- Goggles: the server sends load levels of data ducts near the player (bucketed 0 to 4, only on change, for players revealing hidden things) in a small payload; `DuctOverlayRenderer` tints those ducts. Avoids per frame queries.
- Production Terminal: `ProductionView` adds Packing Station rows (use per direction from their budgets' rolling window) and the busiest duct of the line.

### APIs
NeoForge 21.1: `RegisterPayloadHandlersEvent` (payload), `AddReloadListenerEvent` (tier and upgrade JSON), `RegisterColorHandlersEvent.Block` (tier tint, existing `DuctBlockColor`), `RenderLevelStageEvent` (existing overlay), item tags via datagen.

### Pure versus Minecraft
- Pure, JUnit: `DuctBudget` (simplex shared, duplex separate, fractional carry, load window), `PathPick` (shortest, highest bottleneck), min along path, `UpgradeMath`.
- Minecraft, GameTests: replacement drops the old item and never downgrades, slowest duct decides, shared duct split, upgrade doubles a station, summary text with load, Machines tab rows. Goggles colours are checked in game.

### PLAN.md open decisions
Replaces "Later: High-Speed Data Cable" with tiers and upgrades; settles that Foundation owns upgrade items and Manufacturing may add better ones without a dependency from Foundation.

## Risks / Trade-offs

- [Per tick path budgets cost CPU with many deliveries] -> budgets only for ducts used this tick, paths cached per delivery until the graph changes.
- [Creation order can starve a late delivery on a saturated duct] -> visible in the summary and Production Terminal; players add bandwidth; fairness can come later.
- [Simplex direction semantics are abstract] -> the summary and Machines tab show numbers; docs explain with an example.
