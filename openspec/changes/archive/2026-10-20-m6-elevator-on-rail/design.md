# Design

## Context

Paths: `S` = `shuttles/src/main/java/io/github/dirnei/mc/servus/shuttles`.

- `S/rail/ElevatorItem` extends `AssistedBlockItem`; `updatePlacementContext` turns a plain click on a segment into a placement on top of the shaft. A click on a rail places the segment above the rail today, because the rail cannot be replaced.
- `S/rail/ElevatorBlock.canSurvive` needs a segment or a sturdy top below; `neighborChanged` drops a segment that lost its support. `S/rail/ElevatorBlockEntity` saves lift upgrades and hands them on in `preRemoveSideEffects`.
- `S/rail/RailBlock` (Warehouse Rail; `S/field/FarmRailBlock` extends it) with `RailBlockEntity`, which holds the sleeper wood in a `WoodSlot`. `WarehouseFixtures.rail` shapes a rail from its neighbours with `Block.updateFromNeighbourShapes`.

## Decisions

### D1. Placement on a rail

`ElevatorItem.useOn` handles a click on exactly a Warehouse Rail (`ServusShuttlesBlocks.RAIL`, not a Farm Rail) itself, sneaking or not: the segment state shaped by its neighbours must survive there, then the item reads the rail's sleeper wood, sets the segment in the rail's place (no rail item drops), hands the wood to `ElevatorBlockEntity.rememberRail` and uses up one item. Any other click goes to the normal placement, so `canPlace` and `updatePlacementContext` never have to accept a rail (settled during apply instead of a placement context at the rail).

### D2. Remembered rail

`ElevatorBlockEntity` saves `ReplacedRail` (true only for a segment set by D1) and `RailWood`, the rail's wood id; the block is always the Warehouse Rail, so nothing else is kept.

### D3. Putting the rail back

`ElevatorBlockEntity.preRemoveSideEffects` schedules a `TickTask` on the server that calls `ElevatorBlock.putRailBack` once the removal is done (settled during apply: setting the rail inside the removal would be undone by the level's own neighbour updates for the removed block), only when the space is now air: the rail state from `Block.updateFromNeighbourShapes`, then the wood set on the new `RailBlockEntity`. A replaced segment (another block put in its place) gets no rail. Segments above lose their support on the rail (its top is not solid) and drop as today.

### D4. Segment wood

`ElevatorBlockEntity` implements Foundation's `WoodHolder` with a `WoodSlot`, saved, synced in the update tag and collected into the drop as `WoodContent.WOOD`, the same way `RailBlockEntity` does it. D1 sets the segment's wood from the replaced rail (and still remembers the rail's wood for D3). The elevator item becomes a `WoodBlockItem`, so placing it applies its wood, and its item model uses Foundation's wood item model type with the sleepers on the `Wood.BOARD` texture. `ElevatorRenderer` draws the sleepers of every rail piece with `WoodTextures.spriteOf(wood)` of the segment it draws (the floor segment for the car's rail), made public in Foundation for this. The elevator recipe becomes `servus_foundation:wood_shaped`; `WoodRecipe.assemble` also counts the `WoodContent.WOOD` of any ingredient that carries it, so a rail passes its wood on and planks keep working as before.

## NeoForge APIs

- `BlockItem.useOn`, `Block.updateFromNeighbourShapes`, `BlockEntity.preRemoveSideEffects`, `MinecraftServer.schedule(new TickTask(tick, runnable))` (checked in the 26.1.2 jar).

- Data components on the drop through `BlockEntity.collectImplicitComponents` and `applyImplicitComponents`, as `RailBlockEntity` does with its `WoodSlot`.

## Pure and Minecraft

- No new pure logic: the rule is "rail in, rail out".
- Minecraft, GameTests: placing on a rail, the rail coming back with its wood and joining its neighbours, a falling shaft, bare ground, building up from a rail segment, a Farm Rail left alone, the remembered rail surviving a save.
