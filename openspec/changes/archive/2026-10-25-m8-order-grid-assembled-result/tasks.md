# Tasks

## 1. Most stocked choice

- [x] 1.1 Add the pure `MostStocked` picker in `shuttles/src/main/java/.../shuttles/terminal/` (highest count among candidates, ties to the first, empty when none) with JUnit tests in `shuttles/src/test/java/.../shuttles/terminal/MostStockedTest.java`: highest wins, tie keeps the first, no candidate in stock gives empty, empty candidates give empty. Verify with `gradlew :shuttles:test`.

## 2. Stock-aware cell choice for both JEI fills

- [x] 2.1 Add the shared cell choice beside `CraftingGrid` (`shuttles/.../pattern/CraftingGrid.java`): accepted items plus the warehouse's requestable stock give the stocked stack with its components, else the first accepted item; use `MostStocked`. Verify with `gradlew :shuttles:build`.
- [x] 2.2 `StorageTerminalMenu.fillGhosts` uses the shared choice, matching stock by item so a bare Rail option finds the stocked oak Rail; replace the private `mostStocked`. Add `OrderGridGameTests.transferPicksStockedRail` (oak and spruce Rails stocked, Elevator options with a bare Rail: the cell holds the oak Rail with its material) and keep `transferSetsStockedGhosts` green. Verify with `gradlew :shuttles:runGameTestServer`.
- [x] 2.3 `ProductionTerminalMenu.openEditorWith` fills through `CraftingGrid.templateOf` with the shared choice and the stock of `TerminalWarehouses.online(level, terminal pos)`, falling back to the first item without a warehouse. Add a GameTest in `PatternGameTests` (only spruce planks stocked, open the editor with the Heavy Duty Rack recipe: the middle row holds spruce planks) and keep the existing editor tests green. Verify with `gradlew :shuttles:runGameTestServer`.

## 3. Assembled result on the client

- [x] 3.1 `StorageTerminalScreen.orderResult` and `ProductionTerminalScreen.preview` use `PatternEditing.encode(level, cells).map(EncodedPattern::result)` instead of the display result. Add `OrderGridGameTests.rackGridResultMatchesPreview` (rack grid with oak planks: the encoded result carries oak wood and equals the order preview's item, Save as pattern stores it). Verify with `gradlew :shuttles:build` and `gradlew :shuttles:runGameTestServer`.

## 4. Verification

- [x] 4.1 Play check from main when the user starts it: order grid with iron and spruce planks shows a spruce rack, Save as pattern shows and stores, Next then Start crafts 3 racks; JEI plus on the rack recipe picks a stocked plank in both terminals; JEI plus on the Elevator recipe picks a stocked Rail with its wood and the elevator can be started.
- [x] 4.2 Full `gradlew build`, `gradlew test` and the GameTests green, and the game launches with all mods.

## Workflow follow-up

- Archive, add a Fixed line to `shuttles/CHANGELOG.md` under Unreleased, then propose `m8-pattern-variants`.
