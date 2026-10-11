## Why

The Warehouse Rail and the Farm Rail are a 6 pixel strip in the middle of the block, while the shuttles drawn at 0.8 scale have their wheels at about 2.4..3.6 and 12.4..13.6 pixels, so they stand on the floor beside the rail instead of on it. The rails also ignore the wood family the stations and the Field Head share. Decided in the explore session of 2026-10-11 with mockups: a wide gauge track with wooden sleepers from the recipe, steel heads for the Warehouse Rail and copper heads for the Farm Rail.

## What Changes

- New rail geometry for both rails: two raised rail heads, 2 pixels wide, at 2..4 and 12..14 across the travel direction and 1 pixel above the sleepers; four separate sleepers per block, each 2 pixels long with gaps between them, 14 pixels wide (1..15) so they stick out past the heads.
- Every piece follows the gauge: straight, end of a run, a stepped arc for the curve with radial sleepers, a `#` crossing on a sleeper plate, and dock pieces (buffer beam in the dock stripes, widened to the new gauge).
- Rails become wood blocks like the stations and the Field Head: the sleepers show the wood of the planks in the recipe, the item carries the wood and shows it, a placed rail keeps it and its drop carries it again. Rails of different wood do not stack (accepted).
- Warehouse Rail heads are light steel; Farm Rail heads are fresh copper with a light touch of verdigris. Each rail type uses one rail head texture, so a later change (`m10-rail-corrosion`) can swap it per block like the wood.
- Recipes: the Warehouse Rail takes iron ingots and planks; the Farm Rail takes copper ingots and planks instead of eight Warehouse Rails and a composter.
- The rail inside an elevator shaft (decks, the lift car and the lowest floor) is drawn with the same pieces: steel heads, oak sleepers.
- Shuttles stand on the rail heads: the garage renderer draws them 2 pixels higher, on rails and on a lift car.
- The item shows a short straight piece of track in its wood.
- The old rail textures (`rail`, `rail_curve`, `rail_cross`, `farm_rail`, `farm_rail_curve`, `farm_rail_cross`) are replaced by rail head textures; the dock stripe textures stay.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `storage/warehouse-rail`: the rail look (gauge, sleepers, heads, pieces) and wood from the recipe; the recipe takes planks.
- `storage/farm-rail`: copper heads and a copper and planks recipe.
- `storage/rail-elevator`: the deck and car rail use the same pieces as the outside rail.
- `storage/shuttle-looks`: shuttles stand with their wheels on the rail heads, also on a lift car.
- `foundation/wood-material`: the rails (and the Field Head) join the list of wood blocks; rails show the wood on their sleepers, not as board panels under an iron frame.

## Impact

- Storage: `rail/RailBlockEntity` (wood slot), a new pure `rail/RailLook` (pieces and boxes) with JUnit tests, a new `rail/RailItem` (placement assist plus wood tooltip), `registry/ServusStorageItems`, `ServusStorage` (`Wood.register`), `client/ElevatorRenderer`, `client/ShuttleGarageRenderer`.
- Datagen: `datagen/RailModels` rewritten, loot (`ServusStorageBlockLoot`, rails move to the wooden list), recipes (`ServusStorageRecipeProvider`).
- Foundation: `WoodModel` already wraps multipart block state models; checked, no change expected.
- Textures: new `rail_head` and `farm_rail_head`; old rail textures removed; `rail_dock` and `farm_rail_dock` stay.
- GameTests: new rail look tests (wood kept on drop, both recipes); existing rail, elevator, shuttle, field and harvester tests stay green.
- Docs: rail entries in `docs/src/content/docs/mods/storage.md`; `PLAN.md` M10 lists the change.
- Saves: unreleased; placed rails load with oak sleepers.
