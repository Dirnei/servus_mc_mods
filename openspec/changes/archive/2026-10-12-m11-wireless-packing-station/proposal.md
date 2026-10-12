## Why

With `m11-controller-site` and `m11-chunk-loader-upgrade` every warehouse is a named site that can sleep and wake. What is missing is the link between sites: a player at the base wants the iron that sits in the mining outpost. M11 decided (2026-10-11) that the first link is wireless only: no shuttles, portals or carts travel between sites yet.

## What Changes

- New Storage block **Wireless Packing Station**. It behaves like the Packing Station on the warehouse side: it sits on a rail (joins only through rails), shuttles and instant picks bring items into its To network buffer, and what arrives lands in its To storage buffer and is put away. Its far side is not the data line but a **wireless line** shared by every Wireless Packing Station of the placing player's team in the same dimension. It is never a member of a data line.
- It has an FE buffer (`energy_capacity`, example 100,000 FE) filled through the Utility Duct power line or any `IEnergyStorage` source, and the Speed Upgrade slots of the Packing Station. A transfer costs FE per item and distance: `fe_per_item` plus `fe_per_item_per_block` times the distance between the two stations (examples 4 and 0.02), paid by the receiving station, at `items_per_second` (example 8) per station and direction. Numbers in `storage/wireless_packing_station.json`.
- Sites of one team see each other by name: the controller GUI gets a list of the team's other sites in the dimension with their state (online, sleeping and able to wake, sleeping without a way to wake). The site registry records which teams have a Wireless Packing Station at a site.
- The Storage Terminal can look at another site: its title becomes a site selector ("This site" and the other sites with a Wireless Packing Station of the team). An online site shows its live stock; a sleeping site shows its last known stock greyed out. Requesting an item of another site makes a **transfer**: the other site picks the items into its Wireless Packing Station, which sends them wirelessly to a Wireless Packing Station of the requesting site; they arrive in the requesting site's storage (put away there), not in the terminal's output slots. A sleeping site with enough Chunk Loader Upgrades is woken and held awake until the transfer ends; a sleeping site without that cannot be ordered from. The transfer is listed with the terminal's requests (waking, picking, sending, missing) and can be cancelled.
- Pure logic without Minecraft imports and with JUnit tests: the wireless cost and how many items one tick may move, and the state of a site as another site sees it.

## Capabilities

### New Capabilities

- `storage/wireless-packing-station`: the block, joining by rail, the wireless line per team and dimension, the FE buffer and cost, the rate, transfers between sites, the GUI and status, obtaining it.

### Modified Capabilities

- `storage/storage-terminal`: a site selector, the stock of another site (live or last known, greyed), transfers from another site as requests.
- `storage/warehouse-controller`: the controller GUI lists the team's other sites.
- `storage/site-wake`: the site record also keeps the teams that have a Wireless Packing Station at the site.

## Impact

- New: `storage/.../site/WirelessCost.java`, `RemoteSite.java` (pure), `storage/.../packing/WirelessPackingStationBlock.java`, `WirelessPackingStationBlockEntity.java`, `storage/.../terminal/SelectSitePayload.java`, `SiteRow.java`, `RemoteTransfers.java`; JUnit tests; `gametest/WirelessPackingStationGameTests.java`; textures.
- Changed: `PackingStationBlockEntity` (shipping hook and helpers for the wireless subclass), `PackingStationMenu` (valid for both blocks), `WarehouseNetwork` (`shipWireless`, wireless teams), `SiteRecord`, `SiteRegistry`, `StorageTerminalBlockEntity` (remote requests), `TerminalRequests`, `StorageTerminalMenu`, `StockSyncPayload`, `StorageTerminalScreen`, `ClientStock`, `SiteStatusPayload`, `WarehouseControllerMenu`, `WarehouseControllerScreen`, registries, `ServusStorageEvents` (capabilities), `ServusStoragePayloads`, `StorageStatsManager`, datagen (block state, loot, recipe, lang, tags).
- Docs page `docs/src/content/docs/mods/storage.md` and `PLAN.md` (M11 item 3, section 2.3 wireless).
