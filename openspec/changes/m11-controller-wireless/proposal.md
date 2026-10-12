# Proposal

## Why

The 2026-10-12 play test of `m11-wireless-packing-station` showed that a second block on the rail for the wireless link is one block too many: the Warehouse Controller already is the site (name, chunk span, Chunk Loader Upgrades, record), so it should also be the site's antenna. The user decided the same day that the Wireless Packing Station goes away, the controller becomes wireless capable through an upgrade, and normal Packing Stations pack and unpack for the wireless line, so throughput comes from the Packing Stations the player already builds and upgrades.

This change implements `PLAN.md` M11 item 3 in its redefined form (the wireless link between sites) and is the first of the two follow ups recorded in the outpost decisions: `m11-controller-wireless`, then `m11-warehouse-power`.

## What Changes

- The **Wireless Packing Station** block, its item, block entity, recipe, textures and `storage/wireless_packing_station.json` are removed. Nothing is released, so there is no migration.
- New Storage item **Wireless Upgrade**, crafted at a crafting table, with its own controller slot like the Chunk Loader Upgrade (a Storage item and Storage slot, not a Foundation multiplier upgrade). A controller with the upgrade is the site's antenna.
- The Warehouse Controller gets an **FE buffer** (`controller_energy_capacity`, example 100,000 FE) filled through the Utility Duct power line or any energy source through the standard energy capability. It never gives FE out. `m11-warehouse-power` builds on this buffer for everything else in the warehouse.
- A controller is **wireless capable** while it holds a Wireless Upgrade and is powered (its buffer holds the cost of one item at the smallest cost). A site whose controller is not wireless capable is offline for wireless: it neither sends nor receives, and other sites show it in orange.
- **Transfers land in the terminal's output.** A transfer requested at a Storage Terminal is not put away at this site: the receiving Packing Station (one that reaches the terminal by data line) passes the arriving items on as packets into the terminal's output slots, like a request from this site's own storage (user decision during the play check, 2026-10-12).
- **Packing Stations carry the wireless line.** Any Packing Station on the site's rails packs picked items for another site and unpacks arriving ones for put-away; it does not need a data line for that. A transfer moves at no more than the sending station's rate out and the receiving station's rate in (Speed Upgrades apply), shared with its data line deliveries. A site without a Packing Station can neither send nor receive.
- **Cost:** each item costs `wireless_fe_per_item` plus `wireless_fe_per_item_per_block` times the distance between the two controllers (examples 4 and 0.02), paid from the **receiving** site's controller buffer. Numbers move into a new `storage/power.json`, which `m11-warehouse-power` extends.
- **Other sites are visible only from a wireless capable site.** The Storage Terminal's site selector and the controller's "Other sites" panel list other sites only while this site's controller is wireless capable; otherwise the terminal shows only this site and the panel shows the hint "Insert a Wireless Upgrade to trade with other sites" (or that the controller needs power).
- Kept from the play test: the Input row is locked while another site is shown, sites that cannot trade (now: no wireless capable controller) show their name in orange, the hover shows name and state only, no "team" wording anywhere in the GUI, wake rules of the Chunk Loader Upgrade unchanged.
- The site record keeps, instead of the teams with a Wireless Packing Station, whether the controller holds a Wireless Upgrade, whether it is powered, and whether the site has a Packing Station.
- Pure logic without Minecraft imports and with JUnit tests: the wireless cost (reused from `WirelessCost`), the wireless capable rule, the state of a site as another site sees it, and the choice of the receiving Packing Station.

## Non-goals

- Power for shuttles, wakes and crafting, the "no power" state of the warehouse as a whole: `m11-warehouse-power`.
- SEND and KEEP rules between sites, an In transit view, Wireless Terminal, cross dimension transfers, shuttles or carts between sites (later per `PLAN.md` M11).
- Range tiers or several Wireless Upgrades adding anything: one upgrade, presence only.
- Dropping the crates and adjacent inventory indexing: done by `m11-drop-crates-pipe-access`, which archives before this change, so transfers pick only from racks by shuttle here.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `storage/wireless-packing-station`: every requirement removed; the block is gone.
- `storage/warehouse-controller`: Wireless Upgrade slot, FE buffer, wireless capable rule, transfers between sites and their cost move to the controller, the "Other sites" panel only for a wireless capable controller with a hint otherwise.
- `storage/packing-station`: Packing Stations carry wireless transfers, work for wireless without a data line, status line and tooltip name the wireless line.
- `storage/storage-terminal`: other sites listed only while this site's controller is wireless capable; transfers go Packing Station to Packing Station with the new refusals.
- `storage/site-wake`: the site record keeps the controller's wireless state and whether the site has a Packing Station instead of the wireless teams.

## Impact

- Removed: `storage/.../packing/WirelessPackingStationBlock.java`, `WirelessPackingStationBlockEntity.java`, `gametest/WirelessPackingStationGameTests.java`, `storage/wireless_packing_station.json`, its registrations, capability, datagen entries (block state, model, loot, recipe, lang, tags) and textures.
- New: `storage/.../controller/WirelessUpgradeItem.java`, `WirelessUpgradeSlots.java`, `ControllerEnergy.java`; pure `site/WirelessReach.java` (capable rule) and `packing/ReceiverChoice.java`; `storage/power.json`; `gametest/ControllerWirelessGameTests.java`; JUnit tests.
- Changed: `WarehouseControllerBlockEntity` (upgrade slot, energy, capable state), `WarehouseControllerMenu`, `WarehouseControllerScreen`, `SiteStatusPayload`, `PackingStationBlockEntity` (wireless shipments, status), `WarehouseNetwork` (`shipWireless` through Packing Stations, record update), `SiteRecord`, `SiteRegistry`, `RemoteSite`, `RemoteTransfers`, `SiteRow`, `StockSyncPayload`, `StorageTerminalMenu`, `StorageTerminalScreen`, `TerminalRequests`, `StorageStatsManager`, `ServusStorageEvents` (controller energy capability), registries, datagen, `PackingStationGameTests` (cases that named the removed block).
- Docs page `docs/src/content/docs/mods/storage.md` and `PLAN.md` (section 2.3 item 6 and topology, M11 item 3).
