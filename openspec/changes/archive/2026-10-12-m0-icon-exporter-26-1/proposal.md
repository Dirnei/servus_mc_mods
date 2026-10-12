## Why

The docs Balance lab and recipe graph show item icons from `docs/src/icons/`, written by the dev only `gradlew iconExport` run. The Minecraft 26.1.2 port (`m0-port-minecraft-26-1-2`) left the exporter out: its 1.21.1 off screen GL drawing (`TextureTarget`, `RenderSystem` matrices, `NativeImage.downloadTexture`) is gone, so the run now logs a warning and writes nothing. Items added since 1.21.1 (Chunk Loader Upgrade, Wireless Packing Station, Field Head, Harvester and more) have no icon at all.

## What Changes

- `iconExport` renders icons again on 26.1.2: every item listed in `docs/src/data/gamedata.json` becomes a 64x64 PNG with a transparent background, drawn the way the inventory draws it (GUI item transform and GUI lighting), as before.
- The drawing reuses the picture in picture path the port built for the Shuttle Workshop: a dev only picture in picture renderer, registered only when the run asks for icons, draws each item into its own 64x64 GPU texture inside the GUI render pass (so the GUI lightmap and lighting apply), copies the texture into a mapped GPU buffer and writes it as a PNG through `NativeImage`.
- Block items and composed item models (the shuttle and its tiers, the Harvester, the Spreader) render as 3D models like in the inventory. Like on 1.21.1, parts of an oversized model outside the slot square are cut.
- The committed `servus_*` icons are regenerated; icons for items new since 1.21.1 are added.
- The icon spec names the Servus mods generically (there are three since the Foundation merge).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `foundation/gamedata-export`: the item icon requirement no longer names "all four Servus mods" and gains a scenario for block items and composed models.

## Impact

- Changed: `manufacturing/.../client/IconExporter.java` (hooks, item list, file writing), `ServusManufacturingClient.java` (registers the renderer when the run asks for icons).
- New: `manufacturing/.../client/IconExportRenderState.java`, `IconExportRenderer.java` (dev only picture in picture renderer with GPU readback).
- Regenerated: `docs/src/icons/servus_*/*.png`.
- No gameplay, data or network change; the normal client never registers the renderer and never exports.
