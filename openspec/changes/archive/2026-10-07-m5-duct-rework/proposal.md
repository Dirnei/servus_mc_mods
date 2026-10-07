# Proposal

## Why

Two duct families confuse players: Manufacturing ships Item Duct, Splitter Duct, Filter Duct and Power Cable, Logistics ships the Utility Duct that already does all of it. The Utility Duct also hides what it carries: a block with an item line and an energy line looks exactly like one with a single line, and its side configuration sits behind a six side overview GUI instead of on the side the player is looking at. This change implements the PLAN.md M5 item "Utility Duct with item and energy channels" in its final form and carries out the decision of 2026-10-06 to retire the Manufacturing ducts.

## What Changes

- **Manufacturing ducts removed.** Item Duct, Splitter Duct, Filter Duct and Power Cable leave Manufacturing: blocks, items, block entities, networks, GUI, recipes, textures, generated assets, the `splitter_filter_ducts` tech and their specs. The `logistics` tech descends from `crusher` only.
- **Manufacturing depends on Logistics.** `servus_manufacturing` declares `servus_logistics` as a required dependency so its GameTests and the reference block use the Utility Duct. PLAN.md, CLAUDE.md, the OpenSpec project context and the mod family loading spec state the new rule: Storage depends on Lib only, Manufacturing on Lib and Logistics.
- **Lines instead of channel items.** What a duct carries is a line. The two lines are the Item Pipe (today Item Channel) and the Power Cable (today Energy Channel), still placed as items and dropped on break.
- **Duct grows with its lines.** A duct with one line is thin, with two lines thicker. Each line has its own color, and a duct carrying both shows both colors along the duct and its arms. Arms show only the lines that connect on that side.
- **Eight channels per line per side.** Every side of a line is on channel 1 to 8 (default 1), each with a color. Items only travel between item sides on the same channel, energy only between energy sides on the same channel, within one connected run of ducts. An arm that ends at an inventory or machine carries a ring in its channel color per line.
- **Endpoint GUI with the Configuration Carrot.** Clicking an arm that touches an inventory or machine with the carrot opens that side's configuration: per present line a mode (in and out, pull in, push out, disabled), the channel, and for the item line the filter and priority. Bare hand right click no longer opens anything. Sneak carrot keeps taking facades off and dismantling.

## Capabilities

### New Capabilities
- `logistics/duct-appearance`: how a Utility Duct looks: size by line count, line colors, per line arms, channel rings, interaction with facades.

### Modified Capabilities
- `logistics/utility-duct`: lines placed as items (renamed), per side per line modes and channels, channel bound routing for items and energy, endpoint GUI opened with the carrot.
- `lib/mod-family-loading`: Manufacturing may depend on Logistics; Storage stays independent.
- `manufacturing/research`: built in techs no longer include `splitter_filter_ducts`.

### Removed Capabilities
- `manufacturing/item-duct`: replaced by the Utility Duct's Item Pipe.
- `manufacturing/power-cable`: replaced by the Utility Duct's Power Cable.
- `manufacturing/splitter-duct`: replaced by pull in sides of the Utility Duct.
- `manufacturing/filter-duct`: replaced by per side filters of the Utility Duct.

## Non-goals

- Fluid line. The size step for a third line is reserved but not built.
- Tiered transfer rates for the Power Cable.
- Per part removal by clicking a line on the block. Removal stays a button in the endpoint GUI.
- A sneak carrot shortcut that cycles modes without the GUI.
- Migrating placed Manufacturing ducts. Nothing is released; placed ducts vanish with their blocks.

## Impact

- `manufacturing`: packages `duct` and `cable`, `FilterDuctMenu`, `FilterDuctScreen`, registry entries, datagen, textures, generated assets, recipes, `splitter_filter_ducts.json`, `logistics.json` parents, GameTests rewritten on the Utility Duct; `build.gradle` and `neoforge.mods.toml` gain the Logistics dependency.
- `logistics`: `Channel` becomes `Line`, items renamed, block entity stores channel per line per side, item and energy networks route per channel, new dynamic model composed from part models, block colors for channel rings, shape by line count, endpoint menu and screen, carrot handling in the block, datagen for part models and lang, textures for both line colors.
- `lib`: no code change; `ConnectableBlock` keeps serving rails and other connectables.
- Root `build.gradle`: `logistics_version_range` for the mods.toml template.
- `PLAN.md` sections 0 (dependency rule), 2.1 (ducts), 2.2 (Utility Duct), M2 and M3 lines; `CLAUDE.md` and `openspec/config.yaml` dependency rule.
