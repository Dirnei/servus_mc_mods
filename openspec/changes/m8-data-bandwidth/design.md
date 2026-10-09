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

## Decisions made during apply

- Reconciled with what `m8-packing-station` built: `Shipment` already moved per tick bounded by source, target and `TransferGate` allowances; the path budget is one more bound in the same `TransferAmount` step. `PacketRoutes` stays for its tests, but `DataNetwork.packetRoute` now picks the route with `PathPick`. `Shipment.nodeRemoved` became `reroute()`, called when a node is added or removed, when networks merge and when a duct changes its tier.
- A tier is the id of the data cable item that placed the line (`servus_foundation:data_cable`, `duplex_data_cable`, `fast_data_cable`), saved as `DataTier` on the duct only when it is not the base tier. `DataCableItem` extends `LineItem` and is the only place that sets a tier. "Higher" means more capacity, bandwidth times two for duplex, so no rank field is needed in `cable_tiers.json`.
- The ring colour of a tier lives on its `DataCableItem` (first teal, blue, gold; white, gray, black after the play test, see below), not in JSON: colours are client side and a dedicated server's datapack does not reach the client. The cable tooltip reads `CableTiersManager`, so a remote client shows the shipped numbers when a server pack changes them.
- `DataNode.dataTier()` returns the tier of a duct and null for carriers (terminals, CPU, Crafting Station, Pattern Storage): carriers never limit a delivery.
- Budgets are not cleared every tick. `DataNetwork` keeps a `DuctBudget` for each duct that carried items during the last second and drops it after a full idle second; a new budget starts full (one second's worth, `RateBudget.fill()`), exactly what an idle duct would have saved up. This keeps the fractional carry and the load window correct, and small deliveries (up to 32 items) stay instant as before.
- Direction on a duct: forward when the path's node before it is lower (x, then y, then z) than the node after it. Two deliveries crossing the same duct in opposite directions always get opposite directions.
- The bandwidth GameTests (`slowestDuctDecides`, `fastDuctsCarryMore`, `sharedDuctSplits`, `simplexOppositeShares`, `duplexOppositeSeparate`, `summaryShowsLoad`, `busiestDuctNamed`) live in Storage as `DataBandwidthGameTests`, because Foundation has no data members to move items between. `slowestDuctDecides` uses a Warehouse Controller and a Crafting Station (no gates) over five Fast and one Data Cable duct instead of a Packing Station at 64; the rate is measured over two seconds after the first second. Foundation GameTests got `DataCableTierGameTests` (mixed tiers, drops, placement keeps the tier, `upgradeCableDropsOld`, `noDowngrade`) and `UpgradeGameTests`.
- Summary: a second lang key `message.servus_foundation.data_cable.summary_load` ("...; this duct N of M items/s"); `DataSummary.of(network)` without a duct stays for the old tests. N is the duct's load over the last second (both directions for simplex, the busier one for duplex), M the tier's items per second.
- Goggles: `DuctLoadSync` sends `DuctLoadPayload` (packed position to level 1 to 4) every 10 ticks to players who reveal, only when it changed, for busy ducts within 48 blocks; ducts not listed are idle. Levels: 1 below a quarter, 2 below half, 3 below 90 percent, 4 from 90 percent. `DuctOverlayRenderer` draws a translucent centre box on every data duct within 32 blocks, green when idle up to red when full. The payload is registered in a new `ServusFoundationPayloads`.
- Upgrades: `UpgradesManager` reads `speed_multiplier` per item id from `data/<ns>/data/upgrades.json` (all namespaces merged); `Upgrades.multiplier` falls back to the `UpgradeItem` default and to 1 for a tagged item without an entry. The Speed Upgrade stacks to 16, one per slot. `UpgradeMath.multiplier` takes the list of multipliers.
- Packing Station: `UpgradeSlots` sized by `upgrade_slots` when the block entity is created; a datapack that lowers the count later loses upgrades beyond the new count on load (no release yet). The GUI gets an Upgrades row between To network and the status line, the rate ("16/s") right of its label, sent in `PackingStatusPayload`; shift click moves upgrades between the row and the inventory. Upgrades drop with the buffers. The station records what passed its gate per direction over the last second (`used(Flow)`) for the Production Terminal.
- Production Terminal: `ProductionPayload` got `packing` (`PackingRow`: to storage, to network, rate) and `busiest` (zero or one `DuctRow`: tier name, load, bandwidth); a direction is busy at its rate minus one item for fractions. The Machines tab shows them below the machines in a "Data line" section. The busiest duct is the one with the highest share of its bandwidth.
- Line colours after the play test ("I can't really see a difference"): Item Pipe orange (border `#9A5212`, fill `#EE8C2A`), Power Cable red (`#861A18`, `#D23630`), Fluid Pipe blue (`#264C9C`, `#3F76E4`), and the data line by tier: Data Cable white (`#8A8E94`, `#E6E8EA`), Duplex Data Cable gray (`#46484E`, `#8C8E92`), Fast Data Cable black (fill `#2C2D31` with a lighter border `#6E727A`). Data textures carry a sparse speckle so the white tier does not read as the near white ring, and black bands on a multi line duct keep their lighter edge so they do not read as a gap between the dark dividers. All duct textures and the six cable items come from one generator (`gen_line_colours.py` in the session helpers, seeds 1621 for the noise and 4242 for the speckle); the data tiers differ on the item only by the stripe (Duplex gray with dark edges, Fast black with light edges).
- The tier shows on the duct body, not only on the ring: `DataCableItem` got a skin (texture suffix: empty, `duplex`, `fast`), `UtilityDuctBlockEntity.getModelData` adds `DuctModelData.DATA_SKIN` for a non base tier, and `UtilityDuctModel` swaps every quad whose sprite is a data containing texture (`duct_data`, `duct_*_data`, `duct_*_data_v`) for the same quad on `<name>_<skin>`, remapping the UVs from the old sprite onto the new one (`IQuadTransformer.STRIDE` and `UV0`), cached per skin. The existing model data refresh on a tier change (block update, `requestModelDataUpdate` in `loadAdditional`) covers it. The variants need no atlas entry: the vanilla `blocks` atlas stitches the whole `textures/block` directory of every namespace. A missing variant keeps the base texture. Ring tints match the body: `#F4F4F4`, `#8C8E92`, `#3C3E44`.
