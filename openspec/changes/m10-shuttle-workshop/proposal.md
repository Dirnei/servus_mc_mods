# Proposal: m10-shuttle-workshop

PLAN.md milestone M10 (Field work), item 1: `m10-shuttle-workshop`.

## Why

Field work needs shuttles that differ from each other: a Harvester shuttle tends a field and never takes a warehouse job. Today every shuttle is the same and the fleet only knows how many shuttles a garage holds, so it cannot tell one shuttle from another or give a job to the right one. This change gives shuttles modules and identity first, with the Cargo Hold as the first module, so the next two M10 changes only have to add the field side.

## What Changes

- New **Shuttle Workshop** block in Storage. Right click opens a GUI with one shuttle slot, a tool slot and a utility slot and the player inventory. Modules put into the module slots are fitted into the shuttle; taking them out removes them. The shuttle keeps its modules when it is taken out.
- Shuttle items carry their modules as a **data component**. A shuttle has two module slots: a tool slot (the Harvester and later the Lumber module go there; no tool module exists in this change) and a utility slot. The tooltip lists the fitted modules. A shuttle without modules stacks with plain shuttles again.
- New **Cargo Hold** module (utility slot). A plain shuttle carries one stack; a shuttle with a Cargo Hold carries up to `cargo_hold_stacks` stacks (example 4, new key in `warehouse.json`). It takes several queued jobs that share its way (same source and destination for picks, same station and target for put-aways) in one trip.
- The **fleet tracks every shuttle** of its garages, one per garage slot, with the shuttle's modules, instead of a count per garage. Jobs say which modules they need and whether they are field work; the fleet hands a job only to a free shuttle that fits it. Warehouse jobs need no module and never go to a field only shuttle. The module concept has a field only flag that the Harvester module will set in `m10-harvester`.
- The **Shuttle Garage** locks exactly the slot of each shuttle that is out, instead of the first filled slots.
- Recipes for the workshop and the Cargo Hold, models, blockstate, loot and lang through datagen; placeholder textures.
- `PLAN.md` M10 item 1 and the docs storage page describe the workshop and the Cargo Hold.

## Capabilities

### New Capabilities
- `storage/shuttle-workshop`: the Shuttle Workshop block and GUI, shuttle modules with their slots, the Cargo Hold module.

### Modified Capabilities
- `storage/shuttles`: the fleet is every shuttle with its modules; jobs go to a shuttle whose modules fit; field only shuttles take no warehouse job; a Cargo Hold shuttle carries several stacks of jobs that share a way.
- `storage/shuttle-garage`: the GUI locks the slot of the shuttle that is out.

## Non-goals

- The Harvester module, field jobs, Farm Rail and Field Head (`m10-farm-rail-field-head`, `m10-harvester`).
- The Lumber module and tree fields (`m10-lumber`).
- Multi stop trips: a Cargo Hold shuttle batches only jobs with the same way, it never collects from several sources or delivers to several destinations on one trip.
- A garage capacity upgrade slot.
- Drawing modules on the shuttle cart; the cart looks the same with or without modules.

## Impact

- Storage only. New package `storage/shuttle/module` (pure profile and matching logic plus items, component, menu), new block `ShuttleWorkshopBlock`, menu and screen.
- `ShuttleFleet` (per shuttle identity, profile, several loads per shuttle, module matching), `ShuttleJob` (needs), `WarehouseNetwork` (adopts garages with profiles), `ShuttleGarageBlockEntity` and `ShuttleGarageMenu` (per slot lock), `StorageStatsManager` (`cargo_hold_stacks`).
- Registries: block, item, menu, data component. No new payloads.
- Unreleased project: no migration of saved shuttles; plain shuttles in existing garages simply have no modules.
