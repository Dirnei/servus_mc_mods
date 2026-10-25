## Why

Shuttles farm crops but not wood, so every warehouse still needs a player with an axe for logs, sticks and charcoal. Trees are the first field kind beyond crops that PLAN.md asks for, and the Field Head, farm rails, supply buffer and To storage buffer already carry everything a tree farm needs except the trees themselves.

This change implements the PLAN.md M10 Shuttles open point `m10-lumber` ("a Lumber module for tree fields") and settles the trees part of the open decision "Field kinds beyond crops (trees first, then others) and which modules they need". It is the first of two stacked changes: this one makes tree fields work with a plain felling look; `m10-lumber-felling-look` adds the felling arm, the saw and the toppling tree.

It builds on `m10-field-harvest-buffer`, which must be on main first: logs, saplings, sticks and apples go into the Field Head's To storage buffer like a Harvester's harvest, and plain shuttles put them away.

## What Changes

- **Field kind on the Field Head**: Crops (as today) or Trees, chosen on the Field tab and saved with the head. In a Trees field the sapling takes the place of the crop everywhere: the picker, the supply buffer's seed part, supply runs, the Stock tab and the stock levels.
- **Sapling picker**: in Trees mode the crop picker lists every sapling (the `minecraft:saplings` item tag, mods' saplings included) after "None, fell only". Saplings that can grow a big tree (spruce, jungle, dark oak, pale oak, from a Shuttles item tag) get a "2x2" entry; dark oak and pale oak only have the 2x2 entry. A 2x2 entry needs a Range Upgrade in the head.
- **Tree spots**: a Trees field plants only on spots, every other plot in both directions, so trees have room. A 2x2 field plants squares of four saplings with a gap of one plot between squares, all four at once.
- **Trees need no water**: a Trees field is never watered, uses no water and so its rails do not age.
- **Ready to fell without scanning**: the head learns that a sapling grew into a tree from NeoForge's `BlockGrowFeatureEvent` (natural growth, a player's bone meal and the Spreader's doses all fire it), from block breaks and places as today, and from the one pass when the field is loaded, changed or rechecked, which also counts trees that already stand on the field. Grown trees count as ripe plots, saplings as planted, so the ripe share, harvest threshold, ripe share bar and fertilizing work unchanged.
- **Lumber module**: a craftable tool module installed in the Shuttle Workshop, wide and field only like the Harvester. A Lumber shuttle takes only tree field jobs, a Harvester only crop field jobs; one shuttle never does both.
- **Felling**: a Lumber shuttle drives rail by rail like a harvester and fells each grown tree from the rail nearest its trunk: the connected logs, the natural leaves no other trunk holds and hanging parts such as vines and cocoa are removed at once (up to a data limit, example 200 logs), their loot tables give the drops (logs, saplings, sticks, apples), the drops go into the cargo and the spot is replanted. Leaves are stripped, never left to decay. In this change a tree simply comes down with the break sound and particles at its base.
- **Reused**: the Spreader doses saplings with bone meal, the head's supply buffer holds saplings, stock levels count the produce (logs), and the field status, Stock tab and field command cover tree fields.
- **Lumber look**: a turret with a folded steel arm and a felling head on the shuttle, as real geometry in the family's steel.

## Capabilities

### New Capabilities
- `shuttles/lumber`: the Lumber module, tree field jobs and runs, felling a tree with its drops, replanting spots and squares, and runs that cannot go on.

### Modified Capabilities
- `shuttles/field-head`: new requirements for the field kind, the sapling picker, tree spots and trees ready to fell; "Water buffer" leaves Trees fields dry; "Ripeness tracking" adds tree growth; "Plots in sight of the rails" lets saplings, trees and leaves keep a line open; "Field work status" gains the tree field reasons; "Field diagnostics" prints the kind.
- `shuttles/harvester`: "Harvester module" and "Field jobs" (as `m10-field-harvest-buffer` leaves it) apply to Crops fields only.
- `shuttles/spreader`: "Dosing the field" doses saplings in Trees fields.
- `shuttles/shuttle-workshop`: "Tool modules mark field work" names the Lumber module.
- `shuttles/shuttle-looks`: "A shuttle is drawn from its parts" draws the Lumber tool.

## Impact

- Shuttles mod: `field/FieldHeadBlockEntity` (kind, 2x2 flag, sapling as crop, no water for trees, tree classification), `field/FieldEvents` (grow feature event), new pure `field/FieldKind`, `field/TreeSpots`, `field/TreeWalk`, `field/TreePlotWork`; `field/HarvesterWork` gains tree reading, felling and sapling planting; `field/FieldWork` and `field/FieldDue` tree reasons; `shuttle/FieldJob` carries the kind, `shuttle/FieldRuns` fells and replants; `network/WarehouseNetwork.tickFields` queues tree jobs for Lumber shuttles; `shuttle/module/ModuleSpec`, `JobNeeds`, `registry/ServusShuttlesItems` (Lumber); `menu/FieldHeadMenu`, `client/FieldHeadScreen`, `client/CropPickerEntries` (kind buttons, sapling list); `command` field command; `shuttle/FleetSaves` saves the job's kind.
- Data: `field.json` gains `fell_ticks`, `max_tree_logs`, `max_tree_blocks`; item tags `servus_shuttles:saplings_2x2` and `servus_shuttles:saplings_2x2_only`; block tags `servus_shuttles:tree_logs` and `servus_shuttles:tree_attachments`; recipe, item model, tool model and lang by datagen.
- Saves: heads without a kind load as Crops; field jobs without a kind load as crop jobs.
- Depends on `m10-field-harvest-buffer` being implemented and archived first; its `shuttles/harvester` "Field jobs" text is the base of this change's delta.

## Non-goals

- The felling arm, saw, toppling animation and leaf burst (`m10-lumber-felling-look`).
- Crimson and Warped fungi on nylium, azalea bushes as their own kind, and other field kinds (later open points).
- Fortune, shears or silk touch on felled trees; leaves are never kept as blocks.
- A landing lane check for the falling tree, and keeping bee nests: a bee nest on a felled tree stays where it hung.
- One shuttle that does both crops and trees.
