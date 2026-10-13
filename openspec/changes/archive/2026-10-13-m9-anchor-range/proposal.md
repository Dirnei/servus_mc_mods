# Proposal

## Why

An Ender Anchor loads exactly its own chunk, so a base of any size needs a row of anchors, and the team cap counts blocks: four anchors, or one woken warehouse site that may load 36 chunks for a single slot. Players want one anchor to cover a base, a cap a server admin can reason about (chunks, not blocks), and a way to see what is loaded. Decided in `/opsx:explore` on 2026-10-13 after the Ender Anchor play check (memory note "anchor-range-decisions").

## What Changes

This change is a follow-up to PLAN.md milestone **M9 Alchemy** for the Foundation machines (section 2.1, "Ender Anchor"), as a new item `m9-anchor-range`.

- **Anchor Upgrade in Foundation.** The Storage Chunk Loader Upgrade moves to Foundation and is renamed Anchor Upgrade (id `servus_foundation:anchor_upgrade`). Recipe unchanged: an Ender Anchor in the middle with ender pearls, iron ingots and redstone. The Warehouse Controller keeps using it (9 chunks per upgrade, `chunks_per_upgrade`).
- **Anchor area.** The Ender Anchor gets 2 Anchor Upgrade slots. Its area is a square of radius = upgrades: 1, 9 or 25 chunks. The square can be offset, as long as the anchor's own chunk stays inside it.
- **FE per chunk.** The anchor pays its `energy_per_tick` for every chunk of its square, overlaps included.
- **Team cap in chunks.** `maxEnderAnchorsPerTeam` becomes `maxLoadedChunksPerTeam` (example 49). A team's load is the union of every chunk its anchors and woken sites hold, so overlaps count once. An anchor or a wake whose area would push the load over the cap is refused as a whole. An anchor that changes its area while active is re-checked without its own old area.
- **Anchor GUI.** Upgrade slots, four arrow buttons for the offset, the status line, the team load ("14 / 49 chunks") and a "Show range" toggle (client only, off by default).
- **Range outline.** While shown, the area's chunk borders are drawn in purple at the anchor's height with a column at each corner.
- `PLAN.md`, README and docs describe the area, the cap and the Anchor Upgrade.

## Capabilities

### New Capabilities
- none

### Modified Capabilities
- `foundation/ender-anchor`: loads a square area with upgrades and offset, FE per chunk, team cap in chunks, the Anchor Upgrade, GUI and range outline.
- `storage/site-wake`: records count Anchor Upgrades, a wake counts its span against the chunk cap.
- `storage/warehouse-controller`: Anchor Upgrade slots instead of Chunk Loader Upgrade slots; the upgrade item is no longer a Storage item.
- `storage/storage-terminal`: transfers name the Anchor Upgrade.

## Non-goals

- Anchor areas other than squares, or an offset that leaves the anchor's own chunk.
- Sharing the FE of overlapping chunks between anchors.
- Showing the range of a warehouse site in the world (the controller keeps its chunk map).
- A migration for Chunk Loader Upgrades in dev worlds (nothing is released; they vanish).
- A server wide view of all loaded chunks (a command can follow).

## Impact

- Code, Foundation: `machine/anchor/` (`EnderAnchorBlockEntity` area, offset, slots, tickets for many chunks; `EnderAnchorData` keeps the chunks of every entry; new pure `AnchorArea` and `TeamLoad`), `machine/menu/EnderAnchorMenu`, `client/EnderAnchorScreen`, a new `client/AnchorRangeRenderer`, new `upgrade/AnchorUpgradeItem` and `upgrade/AnchorUpgradeSlots`, payloads for the offset buttons, `config/ServusFoundationConfig`, datagen (item, recipe, lang, GUI texture).
- Code, Storage: the upgrade item, slots and recipe go; `controller/WarehouseControllerBlockEntity`, `menu/WarehouseControllerMenu`, `site/SiteWakes` (register the span's chunks, check the chunk cap), `registry/ServusStorageItems`, datagen, GameTests.
- Data: `machine/ender_anchor.json` `energy_per_tick` now per chunk, `energy_capacity` raised so a full square runs a while without input.
- Docs: `docs/src/content/docs/mods/foundation.md`, `storage.md`, `reference/datapack/machines.md`, `gamedata.json`; `README.md`; `PLAN.md` sections 2.1 and the M11 site text.
