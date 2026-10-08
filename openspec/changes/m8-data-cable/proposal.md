# Proposal

## Why

PLAN.md M8 (on-demand autocrafting) builds on a data cable that links the warehouse, the Crafting CPU, terminals and machines without any setup. This change implements the first M8 item, `m8-data-cable`: the cable block and its network in Servus Lib, so every later M8 change has a place to plug into and other mods can join through capabilities alone.

## What Changes

- New block **Data Cable** in `servus_lib`, Lib's first block. It is a thin connectable block that links to neighbouring Data Cables and to blocks that are data members. It holds nothing, moves nothing and needs no configuration.
- A block is a **data member** on a side when it exposes `servus_lib:production` (role producer) or `servus_lib:stock_provider` or `servus_lib:stock_sink` (role stock) on that side. Today that covers the Assembler, the Crafting Station and the Warehouse Controller. Plain inventories are not members.
- A new Lib network type **data network**: cables are graph nodes, members are found when cables join, when a neighbour changes and when a member's capability goes away. Each network keeps its member list by role. Nothing scans blocks every tick.
- A Lib API to ask for the data network at a cable or member position and for its members by role, for the later M8 changes (packets, Access Point, CPU).
- Right clicking a Data Cable with an empty hand shows a one line summary in the action bar: cable count, producers and stock members of its network.
- Sneak placement assist for the Data Cable item, like rails and ducts.
- Datagen in Lib grows a blockstate, loot table, block tag and recipe for the cable.

## Capabilities

### New Capabilities
- `lib/data-cable`: the Data Cable block, the data network, member detection by capability and the member query API.

### Modified Capabilities
None. The placement assist spec is generic; the cable only uses it.

## Non-goals

- Moving items over the cable (packets, size and speed): `m8-cable-packets`.
- A `data_link` capability for members without production or stock (terminals, CPU, Pattern Storage, Access Point): added by `m8-access-point`, the first change that needs it.
- Access Point, Counter Terminal on the cable, Crafting CPU, Pattern Storage, Production Terminal: their own M8 changes.
- Machine Adapter and plain inventories as members: `m8-machine-adapter`.
- High-Speed Data Cable, wireless, facades or goggles overlay for the cable.

## Impact

- `lib`: new packages for the cable block, block entity and data network; first `DeferredRegister` for blocks and block entities in `ServusLib`; datagen providers for blockstates, loot tables and block tags; a texture.
- `storage`: GameTests that wire the cable to the Warehouse Controller and a Crafting Station (both already expose the capabilities), since Lib has no members of its own.
- No change to existing blocks, specs or saved data. All four mods still load alone with Lib.
