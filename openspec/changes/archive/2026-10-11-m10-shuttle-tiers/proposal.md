# Proposal: m10-shuttle-tiers

PLAN.md milestone M10 (Field work), item 4 of the list, the first of the 2026-10-11 follow-ups: `m10-shuttle-tiers`.

## Why

Every shuttle is the same cart today, and the only way to make one carry more is the Cargo Hold. The play test asked for shuttles that grow: a bigger hold that is expensive because it raises throughput the most, faster travel, faster handling at stops, all in one place. This change makes the shuttle itself the tier (Mk1 to Mk3), adds count upgrades in the Mekanism style, and turns the Shuttle Workshop into the place where shuttles are upgraded and built.

## What Changes

- The Shuttle becomes **Shuttle Mk1, Mk2 and Mk3**, three items. Mk1 keeps the item id `shuttle` and the old recipe; Mk2 and Mk3 are new items that only the Shuttle Workshop builds. A tier sets the stacks a shuttle carries per trip (examples 2, 4 and 8) and the max count of each count upgrade (examples 2, 4 and 8), all from a new `storage/shuttle.json`.
- **Count upgrades**: Speed (the Foundation Speed Upgrade item, reused) raises travel speed; the new **Handling Upgrade** shortens the time a shuttle spends at stops. Each goes into a shuttle once per item up to the tier's max.
- **Stops take time**: loading and unloading at a source, a terminal's Packing Station, a station or a target take a stop time plus a time per stack (examples 10 ticks and 4 ticks per stack); Handling shortens it, and it also shortens a harvester's work time per plot.
- **Slower base shuttle**: the base speed drops (example 0.15 blocks per tick from 0.25) and each Speed count adds to it. Speed is per shuttle; `shuttle_speed` leaves `warehouse.json`.
- The **Shuttle Workshop** gets one install slot instead of the tool and utility slots: a count upgrade adds one count per item up to the max, a tool replaces the fitted tool and the old one comes back. The GUI lists the tier, the counts with their max and the tool, each with a button that takes one out.
- **Tier builds**: the workshop now keeps a shuttle, a row of supply slots and its build state, and drops them when broken. The next tier's build sheet (supplies from `shuttle.json`) is listed with what is in and what is missing; **Build now** uses the supplies and turns the shuttle into the next tier with its counts, tool and name. The build by hand with a name plate comes in `m10-shuttle-build-minigame`.
- The **Cargo Hold** item, `cargo_hold_stacks` and `harvester_stacks` go away: Mk1 already carries two stacks, enough for a crop and its seeds.
- The shuttle sync carries each shuttle's item, so clients know its tier, counts and tool for `m10-shuttle-looks`; the cart is drawn with the shuttle's own item.
- Recipes (Handling Upgrade), models, lang through datagen; placeholder textures for Mk2, Mk3 and the Handling Upgrade.

## Capabilities

### New Capabilities
- `storage/shuttle-tiers`: shuttle tiers, count upgrades and their effects, stop times, the stats file and tier builds.

### Modified Capabilities
- `storage/shuttle-workshop`: one install slot, removal buttons, the workshop keeps a shuttle and supplies, Build now; the Cargo Hold goes away and the module slots requirement changes.
- `storage/shuttles`: the item is a tier, speed per shuttle, stops take time, stacks per trip come from the tier.
- `storage/harvester`: field cargo from the tier's stacks, work time per plot shortened by Handling, the Harvester goes in through the install slot.

## Non-goals

- Drawing tiers, upgrades and tools on the cart, and the bigger shuttle (`m10-shuttle-looks`).
- The build by hand, the minigame and name plates (`m10-shuttle-build-minigame`).
- Field range on the Field Head and harvesters that never ride a lift car (`m10-field-range`).
- Tier downgrades: a built tier stays.
- JEI pages for build sheets.

## Impact

- Storage only, plus reading the Foundation Speed Upgrade item. Package `storage/shuttle/module` (pure tier stats, profile, build check), `ShuttleItem`, `ShuttleModules` (component now holds tool and counts), new `ShuttleWorkshopBlockEntity`, `ShuttleWorkshopMenu` and screen rewritten.
- `ShuttleFleet` (per shuttle speed, stop times, stack counts, shuttle item in views), `ShuttleView`, `ShuttleGarageRenderer`, `ShuttleGarageBlockEntity` (hands the fleet its stacks), `WarehouseNetwork`, `StorageStatsManager` (`shuttle.json`, removes `cargo_hold_stacks`, `harvester_stacks`, `shuttle_speed`).
- Registries: items `shuttle_mk2`, `shuttle_mk3`, `handling_upgrade`; `cargo_hold` removed; block entity type for the workshop.
- GameTests that used a plain one stack shuttle or the Cargo Hold are rewritten for tiers. Unreleased: dev worlds keep their Mk1 shuttles, Cargo Holds vanish.
