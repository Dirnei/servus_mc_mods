# Design

## Context

The Calciner build is described by the pure table `CalcinerLayout`, which lists cells as `right`/`up` offsets from the Calciner, each with a `Role`. Forming, the part lookups, the Journal schematic (`ServusManufacturingMultiblockProvider`), `CalcinerLayoutTest` and `CalcinerGameTests` all read this one table. Forming replaces vanilla Bricks with `CalcinerShellBlock` (one block, `CELL` 1 to 6, which drops Bricks). Parts switch to their `FORMED` state, and the Copper Duct gets `PIECE` 0 to 2. `CalcinerPorts.push` pushes Mercury from the Receiver and Sulfur from the lowest duct into any touching fluid handler. That only works because both sit right above their tank columns. `CalcinerRenderer` draws the Ash and the Receiver's Mercury from the Calciner's buffers. Particles come from `CalcinerPartBlock.animateTick` on the lit Firebox and Hood.

The new look was agreed with a three.js preview during exploration (variant "collar" for Sulfur, "basin" for Mercury).

## Goals / Non-Goals

**Goals:**
- Keep the single layout table as the source of forming, ports, schematic and tests.
- Fill the two tanks without any neighbour scan beyond the two known tank cells.
- Every new model part passes the project's z-fighting check.

**Non-Goals:**
- No new block entity, GUI, payload or datapack field.
- No change to the Fluid Tank in Foundation.

## Decisions

### Layout table
These are the new cells, as (right, up) offsets:

| Index | Role | Position |
| --- | --- | --- |
| 0 | `CALCINER` | (0,0) |
| 1, 2 | `STONE_BRICKS` | (-1,0), (1,0) |
| 3, 4 | `DEEPSLATE_TILES` | (-1,-2), (1,-2) |
| 5 | `FIREBOX` | (0,-1) |
| 6 | `ASH_PAN` | (0,-2) |
| 7 | `ALEMBIC` | (0,1) |
| 8 | `RECEIVER` | (1,1) |
| 9 | `HOOD` | (0,2) |
| 10 | `TANK` Sulfur | (-1,-1) |
| 11 | `TANK` Mercury | (1,-1) |
| 12 | `DUCT` elbow | (-1,2) |
| 13 | `DUCT` down into the stone | (-1,1) |

- `FIRST_DUCT = 12` and `SULFUR_OUTLET = 13`. New constants `SULFUR_TANK = 10` and `MERCURY_TANK = 11` name the tank cells.
- `BRICK` and `AIR` go away. Roles get the symbols `S` and `E`, so the pattern rows read `DH_`, `DLR`, `S0S`, `TFT`, `EAE`.
- `placement()` keeps working unchanged: it walks toward the middle column.
- Alternative considered: keep one `BRICK` role and look up the material by row. That was rejected, because the role is what forming, the schematic and the "missing" message read.

### Two shell blocks instead of loot conditions
- `CalcinerShellBlock` stays one class with `FACING` and `CELL` (now 1 to 4). It is registered twice:
  - `calciner_stone_bricks`, with properties like Stone Bricks (stone map colour, strength 1.5, stone sound), dropping Stone Bricks.
  - `calciner_deepslate_tiles`, with properties like Deepslate Tiles (deepslate map colour, strength 3.5, deepslate tiles sound), dropping Deepslate Tiles.
  - Both are built like the current `bricks()` helper in `ServusManufacturingBlocks`: `requiresCorrectToolForDrops()` and `noOcclusion()`, since the formed models are not full cubes.
- `ServusManufacturingBlockLoot` uses `dropOther` for each. `CalcinerStructure` maps the roles `STONE_BRICKS` and `DEEPSLATE_TILES` to their vanilla block for checks and unforming, and to their shell block for forming.
- Alternative considered: one shell block with a `LootItemBlockStatePropertyCondition` on `CELL`. The API exists in 26.1.2 (`LootItemBlockStatePropertyCondition.Builder.setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(Property<Integer>, int))`, checked with javap on the 26.1.2 client jar). It was rejected because one block cannot mine and sound like both stone bricks and deepslate tiles.
- The id `calciner_shell` disappears. The project is unreleased, so no migration is needed.

### The Calciner fills its own tanks
- `CalcinerPorts.push` no longer looks around the ports. For each of Sulfur and Mercury, it takes the fluid capability of the tank cell (`SULFUR_TANK` or `MERCURY_TANK` at the Calciner's facing) through `level.getCapability(Capabilities.Fluid.BLOCK, pos, Direction.UP)` and fills what fits.
- It runs on the existing `PUSH_INTERVAL` from the server tick of `CalcinerBlockEntity`.
- A tank that is part of a larger Fluid Tank behind it simply fills that whole tank.
- The Receiver and the lower duct keep their drain only capability (`CalcinerPorts.fluid`), and the Receiver keeps its bucket use. Nothing else changes for pipes.
- Alternative considered: pipe the principle down through the stone as a hidden block. That was rejected as needless world state.

### Neighbour check without the air cell
- `CalcinerStructure.neighbourChanged` now only checks the two tank cells. The tanks still have no hook of their own.
- The tanks touch the Firebox, the Stone Bricks above them and the Deepslate Tiles below them. These are all formed blocks whose `neighborChanged` already calls this check, so removing a tank is still seen.

### Duct pieces
- `PIECE` becomes 0 to 3:
  - 0: Calciner elbow. It runs across into the Hood's pipe and turns down.
  - 1: Calciner drop. A straight pipe down into the left Stone Bricks.
  - 2: `ATHANOR_NEAR`.
  - 3: `ATHANOR_OVER`.
- `calcinerPiece` tests `PIECE < ATHANOR_NEAR` as before. `AthanorBlockEntity` and its GameTest only use the constants, so they keep working.

### Models (datagen, `AlchemyModels`)
- The Ash Pan and Firebox frames take a texture parameter: `block/deepslate_tiles` for the Ash Pan and `block/polished_deepslate` for the Firebox. The mouth uses `block/stone_bricks`, and its frame runs to the full block height because the cornice is gone. `trim()` and its constants are deleted.
- Stone shell models are chosen per cell:
  - Left (cell 1), collar: the body ends at y 15. A stone ring around a 6 px hole from x 5 to 11 fills y 15 to 16. A cut copper ring with outer edge 5.05 to 10.95 and inner edge 6.05 to 9.95 rises from y 15 to 16.6 and meets the duct pipe (x 6 to 10), which drops into it to y 13 of the stone.
  - Right (cell 2), basin: the body ends at y 12. A stone ring around a hole from 3 to 13 fills y 12 to 16, and the basin floor uses `block/smooth_stone`. A drain grate of 1 px iron bars sits in the middle from 6 to 10: long bars from y 12.2 to 12.4, cross bars from y 12 to 12.195. The cross bars stop 0.005 px below the long bars, so no faces meet in one plane.
- The deepslate shell uses the vanilla `block/deepslate_tiles` cube model with the shell's facing ignored.
- Duct pieces:
  - Elbow: the across arm runs from x -5 to 10 in model space, 5 px into the Hood's pipe as before. It has the cut copper joint at the bend.
  - Drop: copper from y -3 to 16.
- Receiver formed:
  - Four cut copper legs, 1 px square, at the corners of 4 to 12, from y 0 to 3.
  - The flask from y 3 to 11 and the neck from y 11 to 15.
  - A 2 px cut copper tap from y 0.5 to 3 with a small lip, ending above the basin.
- Alembic formed: the beak ends 4 px inside the Receiver's flask, i.e. from model x -4 to 4.
- All coordinates are in the model space of `AlchemyModels` (front faces north, the player's right is -x). The preview used mirrored x, and tasks convert it.

### Look while working
- `CalcinerStructure.light` also lights the Receiver. `CalcinerPartBlock` already has `LIT` and lit models per part, so the lit Receiver model is the same as unlit.
- `CalcinerRenderer` draws the drop and the puddle:
  - The drop is a 1 px Mercury box falling from the tap into the basin, cycling with the level's game time, while the Receiver is lit.
  - The puddle is a 0.6 px Mercury slab on the basin floor while the Fluid Tank at `MERCURY_TANK` holds fluid. It is read on the client from `FluidTankBlockEntity.rawShare()`, the synced value `FluidTankRenderer` already draws from.
  - Both use `FluidSprites.still` and `FluidSprites.tint` of the Mercury fluid, like the existing flask fill.
- The flask fill box moves up to the new flask (y 3.25 to 10.75).
- The renderer's bounding box shrinks to 3 by 5.
- Alternative considered: vanilla dripping particles. They were rejected because no vanilla drip can be tinted silver.

### Journal and PLAN
- The schematic provider maps the two new symbols through `CalcinerStructure.expected` and drops the `AIR` skip.
- Visualize still anchors on the Ash Pan, which stays the bottom middle cell.
- PLAN.md text about the brick furnace with tank columns is updated in this change.

### APIs
- `Capabilities.Fluid.BLOCK` with `level.getCapability(...)`, as used today by `CalcinerPorts`.
- `BlockBehaviour.Properties.of()` with `mapColor`, `strength`, `sound` (`SoundType.STONE`, `SoundType.DEEPSLATE_TILES`), as in the existing `bricks()` helper.
- Datagen: `BlockLootSubProvider.dropOther` and `BlockStateProvider`, both used today.

### Pure vs Minecraft
- Pure and covered by JUnit (`CalcinerLayoutTest`): the layout table, its pattern rows, roles, round trip per facing, the free corner, placements and the tank cells.
- Covered by GameTests (`CalcinerGameTests`): forming, drops, unforming on tank removal, filling the tanks directly and the ports.
- Checked in the play check: models, drop and puddle.

## Risks / Trade-offs

- [Single tanks hold less than columns of three] → Pipes take from the tanks as before, and players may extend a tank backward into a larger Fluid Tank, which the push fills as a whole.
- [Existing dev worlds hold `calciner_shell` blocks] → Unreleased. The play check rebuilds the Calciner.
- [Models reaching outside their block (duct drop to y -3, tap to y 0.5 over the basin) can sample wrong ambient occlusion] → Use `ao(false)` on those models if they show dark, as the Purifier spouts do.
- [The puddle reads a block entity of another mod module] → Foundation is a declared dependency of Manufacturing. Without a tank there, the structure is unformed anyway.
