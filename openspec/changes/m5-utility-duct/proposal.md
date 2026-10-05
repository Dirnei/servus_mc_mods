# Proposal

## Why

Logistics is the empty mod. Its core block is the Utility Duct: one duct that carries several channels, each with a mode per side, so one line along a wall moves items and energy. PLAN.md milestone M5, items "Utility Duct with item and energy channels" and "Per side modes".

## What Changes

- **Utility Duct** block with **Item Channel** and **Energy Channel** items. Placing a channel item places a duct carrying that channel; using one on an existing duct adds the channel. Channels are removed from the duct GUI and drop as items; sneak wrench removes the whole duct.
- Per channel, per side modes: in and out (default), pull in, push out, disabled. Set in a GUI opened by right clicking the duct. Items: pull in actively extracts from the neighbour every few ticks; push out delivers; disabled hides the connection. Energy: pull in draws from the neighbour, push out feeds it.
- Each channel forms its own network over adjacent ducts that carry it, using the Lib network manager. Item routing is round robin with a per tick budget like Manufacturing ducts; energy is distributed water filling with a per tick rate.
- Stats in `data/servus_logistics/duct/utility_duct.json`.
- Lib: `ConnectableBlock` moves from Manufacturing to Lib together with a shared multipart datagen helper, so both content mods use it.

## Capabilities

### New Capabilities
- `logistics/utility-duct`: channels, modes, item and energy behaviour, GUI, stats.

### Modified Capabilities
None (the Lib move is a refactor without behaviour change).

## Non-goals

- Filters and priorities per side, facades, goggles overlay, dynamic model and per part shapes (next M5 changes).
- Fluid channel.

## Impact

- Manufacturing imports change for the moved class; its generated files stay identical.
- Logistics gets registries, datagen, payloads and a client screen.
