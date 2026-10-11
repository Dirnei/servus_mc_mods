## Why

M11 makes every Warehouse Controller a **site**: the base or an outpost. Sites will wake for transfers (`m11-chunk-loader-upgrade`) and trade over wireless (`m11-wireless-packing-station`), and both need a site to have a name, an owner team and to know which chunks it covers. Today the controller has no GUI at all (right click prints a summary to chat, stock rules are commands), so the player cannot see or set any of this.

## What Changes

- The Warehouse Controller gets its first GUI, opened by right clicking it with an empty hand: a name field, a status block (online state, the old summary as a tooltip, chunk count, capacity), a small chunk map of the site and a row of Chunk Loader Upgrade slots above the player inventory. Right click no longer prints the summary to chat.
- Every controller is a site with a name (at most 32 characters). An unnamed controller shows "Warehouse at <x>, <z>". The name travels in the dropped item when the controller is dismantled with the Configuration Carrot, like the stock rules. The controller remembers the team of the player who placed it.
- The controller always knows the chunks its site spans: every chunk holding a member of its network or an indexed inventory touching a member. The set is kept from member join and leave events and source changes, recomputed at most once per tick after a change, never by scanning blocks. A chunk that unloads stays in the set; a chunk leaves it only when it is loaded and holds nothing of the site any more. The set is saved on the controller.
- New Storage item **Chunk Loader Upgrade** (own item, not part of the Foundation multiplier upgrade API, like the Field Head's Range Upgrade). The controller has `chunk_loader_upgrade_slots` dedicated slots (example 4), and each upgrade covers `chunks_per_upgrade` chunks (example 9), both in `storage/warehouse.json`. Crafted from a Foundation Chunk Loader, ender pearls, iron and redstone.
- When the site spans more chunks than its upgrades cover, the controller shows an error state: the GUI says so in red and the chunk map draws in red, and the controller's new activity lamp turns red (the `ACTIVITY` block state the stations already use: idle green, blocked red; working orange is kept for the wake of the next change). A controller without upgrades has no capacity to exceed and shows no error; its GUI says it cannot wake.
- `/servus_warehouse info` prints the site name, chunk count and capacity.
- Pure logic without Minecraft imports and with JUnit tests: chunk keys and the span merge, capacity and state, the display name, the chunk map layout.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `storage/warehouse-controller`: the controller becomes a named site with a GUI, a chunk span, Chunk Loader Upgrade slots and an over capacity error state; the summary moves from chat into the GUI; the info command shows the site.

## Impact

- New: `storage/.../site/SiteChunks.java`, `SiteCapacity.java`, `SiteName.java`, `ChunkMapLayout.java` (pure), `storage/.../controller/ChunkLoaderUpgradeSlots.java`, `SiteStatusPayload.java`, `RenameSitePayload.java`, `storage/.../menu/WarehouseControllerMenu.java`, `storage/.../client/WarehouseControllerScreen.java`, `ClientSiteStatus.java`; JUnit tests under `storage/src/test/.../site/`; `gametest/ControllerSiteGameTests.java`.
- Changed: `WarehouseControllerBlock` (GUI, activity lamp, placement, upgrade drops), `WarehouseControllerBlockEntity` (name, team, span, upgrades, state), `WarehouseNetwork` (span upkeep), `StorageStatsManager` and `storage/warehouse.json`, `ServusStorageItems`, `ServusStorageMenus`, `ServusStoragePayloads`, `ServusStorageClient`, `ServusStorageCommands` (info), datagen (block states with the activity lamp, item model, recipe, lang), controller front textures for idle, working and blocked.
- Docs page `docs/src/content/docs/mods/storage.md` and `PLAN.md` M11.
