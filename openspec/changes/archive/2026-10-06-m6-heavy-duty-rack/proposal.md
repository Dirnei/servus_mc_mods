# Proposal

## Why

The warehouse has bulk crates but nothing for mixed, chest like storage that still looks like a warehouse. The Heavy Duty Rack (Schwerlastregal) is a pallet rack bay: a chest sized inventory you open like a chest, that joins with neighbouring bays into one continuous rack and shows a sample of what it holds. The user decided on 2026-10-06: visual connection only, one chest of slots, and a display of the item types it holds the most of. This extends the PLAN.md section 2.3 progression item "Crates and shelving racks" and adds the M4 line "Heavy Duty Rack".

## What Changes

- New block **Heavy Duty Rack** in `servus_storage`: a horizontal block with a front, holding a chest sized inventory (`slots` in `data/<ns>/storage/rack.json`, example 27) of any items. Right click opens a chest style GUI.
- Racks beside each other with the same facing, and racks above or below each other, render as one continuous rack: shared posts disappear at the seams and beams run through. Each rack keeps its own inventory; the connection is purely visual.
- The front face shows a sample of the contents: the item types with the highest counts, up to `display_count` from `rack.json` (example 4), placed on the shelf levels, without counts. The sample updates within a second of a change.
- Automation sees the whole inventory on every side. Breaking spills the contents; sneak dismantling with the Configuration Carrot keeps them on the dropped item.
- The rack is a stock holding storage node: it is indexed, it joins through rails, rails dock onto it and it carries the connection lamp.
- Craftable from iron ingots and planks, in the Servus creative tab after the crates.
- PLAN.md: section 2.3 and the M4 list mention the Heavy Duty Rack.

## Capabilities

### New Capabilities
- `storage/heavy-duty-rack`: what the rack is, its GUI and inventory, how bays connect visually, the sample display, obtaining and dismantling.

### Modified Capabilities
None.

## Non-goals

- A shared inventory across connected bays. Each bay is its own inventory and its own index source.
- Counts in the sample display, or showing every stack. Only the top item types are drawn.
- Shuttle docking positions on the rack. The shuttle change decides those.

## Impact

- `servus_storage`: new `rack` package with `HeavyRackBlock`, `HeavyRackBlockEntity`, `HeavyRackMenu`, a pure `TopCounts` helper with JUnit tests, `HeavyRackRenderer` and `HeavyRackScreen` on the client, registry entries for block, item, block entity and menu, `rack.json` data, datagen for the multipart blockstate, models, recipe, loot, tags and lang, placeholder textures, a GameTest.
- Generated resources gain the rack files. The `rack` package name is free because the crate family change deleted the old rack.
