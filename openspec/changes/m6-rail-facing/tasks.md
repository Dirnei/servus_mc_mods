# Tasks

## 1. Shared facing rule

- [x] 1.1 Move `garage/GarageFacing` to `rail/RailFacing` with a `sneaking` input (sneaking returns the look side); rename `GarageFacingTest` to `rail/RailFacingTest` and add tests: sneak with a clicked rail, sneak with one rail beside, sneak with none. Run `gradlew :shuttles:test`.
- [x] 1.2 Add `rail/RailFacings` (Minecraft side): from a `BlockPlaceContext` and a rail predicate, the clicked rail side, the rails beside and the look side, returning the rail side; move the garage's `clickedRail` and `railsBeside` there and make `ShuttleGarageBlock.getStateForPlacement` use it (doorway behaviour unchanged, sneak now faces the player). Run `gradlew :shuttles:build`.

## 2. Blocks that turn to a rail

- [x] 2.1 Use `RailFacings` in `station/StationBlock` (Inbound, Outbound, Packing), `fluidstation/FluidStationBlock` (keep the refusal check first) and `field/FieldHeadBlock` (Farm Rail predicate). Run `gradlew :shuttles:build`.
- [x] 2.2 GameTests in `gametest/` using the mock player place helper from `FluidTankGameTests` (extract it to a shared gametest helper if it is private): station placed from the aisle faces the rail, sneaking faces the player, rail in look direction wins, Field Head picks the Farm Rail over a Warehouse Rail and still serves a field with its back to the rail, Fluid Inbound beside a rail, garage sneaking faces the player. Run `gradlew :shuttles:runGameTestServer`.

  Play check fix 2026-10-29: the Warehouse Controller turns its front to the rail too (`WarehouseControllerBlock.getStateForPlacement`), GameTest `controllerFacesTheRail`.

  Play check fix 2026-10-29: a Field Head is refused beside a Warehouse Rail with no Farm Rail, with an action bar hint (`FieldHeadBlock.getStateForPlacement`), GameTest `fieldHeadRefusesWarehouseRailOnly`.

## 3. Rack facing

- [x] 3.1 `rack/HeavyRackBlock` and `fluidrack/FluidRackBlock`: facing from a clicked rack of the same kind, else the rack below, else `RailFacings` (sneak faces the player); section joins unchanged. Run `gradlew :shuttles:build`.
- [x] 3.2 GameTests: rack against a rack's east side and against its front copies the facing, rack on the floor beyond a rail faces the rail, Fluid Rack beside a rail faces it, a wood mismatch still copies the facing. Run `gradlew :shuttles:runGameTestServer`.

## 4. Rack row assist

- [x] 4.1 Add `rack/RackAssist` (`PlacementAssist`: run = same kind and facing, direction along the row from the clicked side face or the hit half of front, back, top, bottom); make `HeavyRackItem` and `FluidRackItem` extend `AssistedBlockItem` with it. Run `gradlew :shuttles:build`.
- [x] 4.2 GameTests: sneak on the right half of a row's first rack places past the far end facing the row, sneak on an exposed side face places beside it, a blocked end places nothing; unsneaked side click places against the face. Launch the client and check the ghost and arrow on both rack kinds. Run `gradlew :shuttles:runGameTestServer`.
  - Note: the GameTests are in `RackPlacementGameTests` (plus a Fluid Rack row). The client was not launched in the apply session; the ghost and arrow on both rack kinds are part of the user's play check (7.2).
  - Note: the run must hold racks with the clicked rack's facing, which `isRun(level, pos, stack)` cannot see. Foundation's `PlacementAssist` got a default `isRun(level, start, pos, stack)` that `PlacementAssists.target` calls; existing assists keep their behaviour.

  Play check fix 2026-10-29: the user missed an up arrow; a sneak look at the top or bottom of a rack now stacks up or down its column (`RackAssist.direction`), spec scenario "Stack up a column", GameTest `sneakOnTheTopStacksUp`.

## 5. Section layer

- [x] 5.1 `HeavyRackItem.place` and `FluidRackItem.place`: before the assist, an unsneaked top click on a rack of the same kind places a rack on every bay of the clicked section when all spots are free and the stack (or creative) covers every bay, Fluid Rack items only without fluid; one `super.place` per bay clicking the top of the bay below, left to right. Run `gradlew :shuttles:build`.
- [x] 5.2 GameTests: layer over a three bay section uses three items and forms one section of three with the same facing; two items place one rack; a taken spot places one rack; only the clicked section of two side by side gets the layer; Fluid Rack layer has its port above the port below; a Fluid Rack item with water places one rack. Run `gradlew :shuttles:runGameTestServer`.

## 6. Warehouse parts place instead of opening a GUI

- [x] 6.1 Add the item tag `servus_shuttles:warehouse_parts` (`registry` tags class) filled in `datagen/ServusShuttlesItemTagsProvider` with every Shuttles block item; add one shared check and use it in each `useItemOn` that returns `TRY_WITH_EMPTY_HAND` (`StationBlock`, `HeavyRackBlock`, `FluidStationBlock`, `ShuttleGarageBlock`, `WarehouseControllerBlock`, `StorageTerminalBlock`, `ProductionTerminalBlock`, `PatternStorageBlock`, `CpuBlock`, `CraftingStationBlock`, `FieldHeadBlock`, `ElevatorBlock`, `ShuttleWorkshopBlock`, `FarmRailBlock`) to return `PASS`. Run datagen and `gradlew :shuttles:build`, verify the tag JSON lists the block items.
  - Note: `ElevatorBlock` already passes every held item and `FarmRailBlock` only returns `TRY_WITH_EMPTY_HAND` for an axe, so both needed no change. The check lives in `registry/ServusShuttlesTags.placesInstead`.
- [x] 6.2 GameTests: a rack in hand on a rack front places a rack and opens no menu, a rail in hand on a station places a rail, cobblestone on a rack front opens the rack menu. Run `gradlew :shuttles:runGameTestServer`.

  Play check fix 2026-10-29: narrowed to racks and the Warehouse Rail placing against racks only (tag `servus_shuttles:places_on_racks`, checked in `HeavyRackBlock`); every other block opens its GUI again. Rack fronts walk to the nearest edge, so the top edge stacks up (`RackAssist.direction`), GameTest `sneakAtTheTopEdgeOfAFrontStacksUp`. Going up the column lays a whole layer over the top rack's section (`RackLayer.placeUpColumn`, Foundation `AssistedBlockItem.placeWithoutAssist`), GameTest `sneakUpLaysALayerOnTopOfTheColumn`; the ghost still shows the one rack above the clicked column.

## 7. Wrap up

- [x] 7.1 Add Unreleased lines to `shuttles/CHANGELOG.md`: Changed stations, racks and the Field Head turn to the rail when placed (sneak to face yourself); Added rack layer on a section top click and sneak row extension; Changed blocks in hand place against warehouse blocks instead of opening their GUI. Run `gradlew build`.
- [ ] 7.2 Full `gradlew build` and `gradlew test` green, the game launches with all mods; play check with the user: build a small warehouse from the aisle without sneaking, a rack wall with layers and the row assist, a Field Head beside a Farm Rail, sneak placements, garage unchanged.
  - Note: `build`, `test`, `:shuttles:runGameTestServer` (1126) and `:foundation:runGameTestServer` (253) are green; the play check with the user is open.

## Workflow follow-up

- Archive after the user signs off the play check; groom PLAN.md (nothing stays open from this change unless the play check adds a point).
