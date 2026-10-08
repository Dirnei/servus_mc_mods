# Proposal

## Why

PLAN.md M8 (on-demand autocrafting) builds on a data cable that links the warehouse, the Crafting CPU, terminals and machines without any setup. This change implements the M8 item `m8-data-cable`. It first shipped as a standalone Lib block; after `m8-foundation-merge` the user wants data to live in the same block as the other lines, so the data cable becomes the fourth line of the Utility Duct and the standalone block goes away.

## What Changes

- New duct line **Data** with the item **Data Cable** (`servus_foundation:data_cable`, copper ingot, redstone, copper ingot gives 4). Like the Item Pipe, Power Cable and Fluid Pipe it places a Utility Duct carrying the line or adds the line to a duct, uses the sneak placement assist and drops from a broken duct.
- The data line links to the data line of neighbouring ducts and forms a **data network**. A block is a **data member** on a side when it exposes `servus_foundation:production` (role producer) or `servus_foundation:stock_provider` or `servus_foundation:stock_sink` (role stock) there; today that covers the Assembler, the Crafting Station and the Warehouse Controller. Plain inventories are not members. Members are found through capability invalidation, nothing scans blocks every tick.
- The data line has no channels, filters or priorities: in the endpoint GUI its tab only switches a side on or off and removes the line. An off side reaches no member and does not link to the neighbouring duct's data line.
- Its own teal colour and band, a fixed teal ring at member endpoints, and four line band textures.
- A Foundation API to ask for the data network at a data duct or member position and for its members by role, for the later M8 changes.
- Right clicking a duct with the data line with an empty hand shows a one line summary: ducts, producers and stock members.
- The standalone Data Cable block, its block entity, placement assist, block models and loot table are removed.

## Capabilities

### New Capabilities
- `foundation/data-cable`: the data line, the data network, member detection by capability, on and off sides, the summary and the member query API.

### Modified Capabilities
- `foundation/utility-duct`: line items include the Data Cable, the data network is a separate per line network, the Data Cable recipe, the Data tab of the endpoint GUI.
- `foundation/duct-appearance`: four lines keep the thickest size, the teal data colour and the fixed teal data ring.

## Non-goals

- Moving items over the data line (packets, size and speed): `m8-cable-packets`.
- A `data_link` capability for members without production or stock (terminals, CPU, Pattern Storage, Access Point): `m8-access-point`.
- Machine Adapter and plain inventories as members: `m8-machine-adapter`.
- High-Speed Data Cable and wireless.

## Impact

- `foundation`: `logistics.duct` (Line, block, block entity, line item), `logistics.menu` and `logistics.client` (Data tab, ring colour), `logistics.datagen` (parts for 15 line sets, recipe, lang, item model), `data` package (node type becomes the duct through a small `DataNode` interface, standalone block classes removed), registries and creative tab, textures (`duct_data`, band textures with data, item texture).
- `storage`: `DataCableGameTests` use ducts with the data line.
- `manufacturing`: none beyond recompiling (the pump stub counts arms per line generically).
- Docs: Foundation page gets a Data line section; `gamedata.json` regenerated.
