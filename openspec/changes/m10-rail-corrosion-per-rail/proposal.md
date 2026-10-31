## Why

Corrosion is one stage per field today: a fresh rail placed into an old field takes the field's stage at once, which feels wrong, and an axe with too little durability left for the whole field does nothing at all. Making the stage belong to each rail lets a field age unevenly, lets a worn axe do part of the job, and gives corrosion a clear consequence: a corroded rail stops the water for the rails behind it.

This refines the M10 Field work item "Farm Rail and Field Head with range, corrosion and tabs" (`shuttles/rail-corrosion`), which is built and shipped. It is not an open point in `PLAN.md`; it came out of play.

## What Changes

- **BREAKING (behavior)**: every farm rail keeps its own corrosion stage. The field no longer has one stage and the join rule goes away: a placed rail keeps the stage it has, a crafted rail is fresh copper.
- **BREAKING (behavior)**: the stage and any scrape progress stay on the rail item when it is broken, so breaking and placing a rail again does not clean it. Rails of different stages do not stack, and the item shows its stage.
- Water flows from the Field Head along the rails of its field. A corroded rail is dry and blocks the water for every rail behind it, so a corroded first rail dries the whole field. Only plots of wet rails are hydrated and only wet rails use water.
- An aging step moves every wet rail one stage older; dry rails do not age. Anti-corrosion items still hold the whole field per step.
- Scraping works per rail: starting at the clicked rail and going outward through the field, each rail above fresh copper goes back one stage for `axe_damage_per_rail` durability (example 4, so a stone axe cleans one stage on about 32 rails). A worn axe spends all the durability it has left; the rails it fully pays for go back a stage and the rest stays as scrape progress, in durability points, on the next rail. Unbreaking applies.
- `axe_damage_per_rail` in `data/servus_shuttles/shuttles/field.json` goes from 2 to 4.
- The Field Head's Water tab shows how many rails are in each stage instead of one field stage, and says when water is blocked and how many rails are dry. The top plate's rust rim shows while a corroded rail blocks water. The field diagnostics command prints the counts.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `shuttles/rail-corrosion`: stages per rail instead of per field, stage and progress kept on the item, water blocked by corroded rails, aging of wet rails only, partial scrape with progress, item look per stage, pure logic for water reach and scraping.
- `shuttles/field-head`: hydration covers wet rails only, the Water tab shows rail counts per stage, the diagnostics command prints counts per stage.

## Non-goals

- Per-rail aging counters: the head keeps one water counter for the field.
- Any other way to clean copper (wax-like coatings, chemicals); anti-corrosion items stay the only way to hold the stage.
- A migration for old worlds beyond keeping each rail's saved stage.
- Changing Warehouse Rails, which never corrode.

## Impact

- `shuttles` code: `field/FieldCorrosion` (pure logic for water reach and scraping, join removed), `field/FarmRailBlock` (scrape, loose join tick removed), `field/FieldHeadBlockEntity` (field stage removed, wet rails, aging, hydration boxes, counts), `field/FieldHeads` (loose join removed), `rail/RailBlockEntity` (scrape progress, item component), a new data component in `registry/ServusShuttlesComponents`, the farm rail item model, `menu/FieldHeadMenu`, `client/FieldHeadScreen`, `command/ServusShuttlesCommands`, language and the `field.json` default.
- Saves: old rails keep the stage they saved; the head's saved field stage is no longer read.
- Specs: `shuttles/rail-corrosion`, `shuttles/field-head`.
- `shuttles/CHANGELOG.md`: Changed entry.
