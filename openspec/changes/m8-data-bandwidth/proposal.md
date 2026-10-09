# Proposal

## Why

After `m8-packing-station` the data network is instant and only the Packing Station limits throughput. The user wants a second lever and a progression path: data cables in tiers with a bandwidth, simplex cables that share it between both directions and duplex cables that do not, cables upgraded in the world, and speed upgrades for the Packing Station, all visible with the goggles and in the Production Terminal. Foundation never depends on Manufacturing, so upgrades and cables use vanilla materials; Manufacturing may add its own better upgrades later.

This change is a play test follow-up to PLAN.md M8, added as item `m8-data-bandwidth`, and replaces the old "Later: High-Speed Data Cable" note. It depends on `m8-packing-station`.

## What Changes

- **Data line tiers:** the Data line of the Utility Duct gets tiers, each a line item with a bandwidth in items per second and a direction mode from datapack JSON (examples: Data Cable 32/s simplex, Duplex Data Cable 32/s per direction, Fast Data Cable 128/s duplex; more tiers possible later). Simplex shares its bandwidth between both directions; duplex has the full bandwidth in each direction. Ducts of different tiers still form one data network.
- **Bandwidth limits deliveries:** a delivery moves at most the Packing Station's rate and the bandwidth of the slowest duct on its path; deliveries that share a duct split its per tick budget. No global flow optimization.
- **In-world replacement:** using a higher tier data cable item on a duct whose data line is a lower tier replaces the line in place and drops the old cable item; a click never downgrades. Sneaking keeps the placement assist behaviour.
- **Packing Station speed upgrades:** upgrade slots in the station; a Foundation Speed Upgrade item (vanilla recipe) multiplies the station's rate per upgrade (example x2, JSON). Other mods (Manufacturing later) may add better upgrade items through the same Foundation upgrade API.
- **Visibility:** the empty hand duct summary adds "N of M items/s" for that duct's busiest direction; the goggles overlay colours data ducts by load; the Production Terminal Machines tab lists each Packing Station with its rate use per direction and names the busiest line segment.
- Recipes with vanilla materials (examples: Duplex with quartz, Fast with gold and quartz, Speed Upgrade with redstone and gold).

## Capabilities

### New Capabilities
- `foundation/upgrades`: upgrade items and the upgrade slot API that blocks of any Servus mod use (speed multiplier from JSON).

### Modified Capabilities
- `foundation/data-cable`: tiers, bandwidth per duct, simplex and duplex, in-world replacement, summary with load, goggles load overlay.
- `foundation/data-packets`: deliveries are limited by bandwidth along their path in addition to gates.
- `storage/packing-station`: speed upgrade slots.
- `storage/production-terminal`: Packing Stations and the busiest segment in the Machines tab.

## Non-goals

- Manufacturing upgrade items (later, by Manufacturing).
- Wireless links and any distance based cost (M9).
- Bandwidth for Item Pipe, Power Cable or Fluid Pipe lines.
- Fair scheduling beyond splitting a duct's budget among deliveries in creation order.

## Impact

- `foundation`: `logistics.duct` (Line items for tiers, the data line's tier saved per duct, replacement on use, models and colours per tier), `data` (`DataNetwork` per duct budgets, `DataNode` tier), `data.packet` (`Shipment` path bandwidth), new `upgrade` package, goggles overlay renderer, datagen (items, recipes, lang, textures), new `data/servus_foundation/data/cable_tiers.json` and `upgrades.json`.
- `storage`: Packing Station upgrade slots in block entity, menu and screen; `ProductionView`/`ProductionRows` and the Machines tab.
- Docs pages, `gamedata.json`.
