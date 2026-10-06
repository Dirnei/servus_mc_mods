# Design

## Context

See proposal.md for motivation. The rail change left these pieces in place: `RailBlock extends ConnectableBlock` with `connectsTo` accepting only horizontal `RailBlock` neighbours and a flat shape; `RailBlockEntity extends StorageNodeBlockEntity` with `holdsStock()` false; `WarehouseNetwork` keeps such connector nodes out of the sources and candidate sets; `RailModels` writes the multipart blockstate with the models `rail_center` and `rail_arm`; the Lib `ConnectableBlock` carries `UP` and `DOWN` properties and `ConnectionRule.always()` joins any touching nodes. The storage GameTests build on the `empty_12x6x12` template, which is six blocks high.

## Goals / Non-Goals

**Goals:**
- A shaft is just stacked segments. No pairing, no range, no config.
- Reuse the rail's connector node handling and the rail arm model unchanged.

**Non-Goals:**
- Shuttle movement data on the elevator. The shuttle change adds whatever it needs.

## Decisions

### Elevator block extends ConnectableBlock
`ElevatorBlock extends ConnectableBlock` with radius 4. `connectsTo`: vertical directions accept an `ElevatorBlock` neighbour, horizontal directions accept a `RailBlock` neighbour, everything else is false. So side by side shafts never link visually. `getShape` is overridden to a pillar, `Block.box(4, 0, 4, 12, 16, 12)`, united with the rail arm boxes `Block.box(5, 0, 0, 11, 1, 5)` rotated per connected horizontal side, cached per state like the parent does. No `canSurvive` rule: a segment stands on its own, so breaking the lowest segment leaves the shaft above floating, which the spec allows ("the shaft above it stays in place"). Alternative considered: subclassing `RailBlock`. Rejected, the rail's flat shape and horizontal only rule would have to be undone.

`RailBlock.connectsTo` becomes `direction.getAxis().isHorizontal() && (neighbour instanceof RailBlock || neighbour instanceof ElevatorBlock)`.

### Elevator block entity is a connector node
`ElevatorBlockEntity extends StorageNodeBlockEntity` and returns false from `holdsStock()`, exactly like the rail. `WarehouseNetwork` needs no change: connector handling is keyed on `holdsStock()`. The summary line counts connectors as "rails"; it is renamed to "connectors" so elevators are not reported as rails.

### Datagen and look
`ElevatorModels` in the storage datagen package writes: `elevator_center`, a pillar element `from(4, 0, 4).to(12, 16, 12)` with the texture `block/elevator` on all faces; the multipart blockstate with the pillar always present and the existing `rail_arm` model (`models().getExistingFile(modLoc("block/rail_arm"))`) rotated 0, 90, 180, 270 on the `NORTH`, `EAST`, `SOUTH`, `WEST` conditions. `UP` and `DOWN` need no parts because the pillar already spans the full height. The item uses the pillar model through `simpleBlockItem`. The texture is a 16 by 16 placeholder drawn by a scratchpad script: dark steel with two lighter vertical guide rails and a rung every four pixels, no green.

### Registration and data
- `ServusStorageBlocks.ELEVATOR`: id `elevator`, `MapColor.METAL`, strength 2.0, `SoundType.METAL`, `noOcclusion()`.
- `ServusStorageItems.ELEVATOR` as a simple block item, appended to `CREATIVE_TAB_ORDER` after the rail.
- `ServusStorageBlockEntities.ELEVATOR` for `ElevatorBlockEntity`.
- Recipe: pattern `"III"`, `"IRI"`, `"III"` with iron ingots and one Warehouse Rail, output 4, category `DECORATIONS`, unlocked by having a rail. Example numbers, they live in the generated JSON.
- Loot: self dropping. Tag: `MINEABLE_WITH_PICKAXE`. Lang: "Rail Elevator".

### GameTest
`elevatorLinksFloors` in `ServusStorageGameTests` on `empty_12x6x12`: crate at (2,1,3), rail at (3,1,3), segments at (4,1,3), (4,2,3), (4,3,3), rail at (5,3,3), controller at (6,3,3), terminal at (7,3,3), chest with bread at (4,2,4) touching only the middle segment. After 10 ticks the terminal stock is online, lists the crate's cobblestone and no bread. Then the middle segment is removed and after 10 more ticks the cobblestone is gone from the stock while the controller side stays online. Rails and the controller stand on segment or template floor blocks placed by the test where support is needed: the rail at (5,3,3) gets a stone block at (5,2,3).

## Pure versus Minecraft logic

No new pure logic. `CandidateNeighbours` already covers connector nodes. Minecraft: block, block entity, datagen, GameTest, plus a manual client check of the shaft look and the rail arms into it.

## Risks / Trade-offs

- [A floating shaft after breaking the lowest segment] -> Accepted and specified, matches how ducts behave.
- [Rail arm model reused by two blocks] -> The model file is written once by `RailModels`; `ElevatorModels` only references it, so datagen order does not matter.
- [Six block high template] -> The GameTest uses three segments, which fits with room to spare.
