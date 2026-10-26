# Tasks

Start only after `m8-shared-recipe-grid` is archived.

## 1. Pure expansion

- [x] 1.1 Add pure `shuttles/.../pattern/VariantExpansion` (variants from groups and candidates, ordered by stock, capped at `MAX_VARIANTS`, own variant always included, non-material groups picked once). JUnit `VariantExpansionTest`: `oneGroupGivesOneVariantPerCandidate`, `stockedVariantsFirst`, `capKeepsTheMostStocked`, `ownVariantAlwaysIncluded`, `nonMaterialGroupsArePicked`, `exactPatternGivesItself`. Verify: `gradlew :shuttles:test`.

## 2. Material patterns

- [x] 2.1 `pattern/PatternVariants`: `Verdict.MATERIAL` (same item and count, other components), `possible()` for it, the material group and the varying component types, the generic key; `pattern/EncodedPattern`: `duplicates` by result item and count for Any patterns, a `withKeys` caller re-assembles for material patterns; lang for "follows" in `ServusShuttlesLanguageProvider`. GameTests in `gametest/PatternGameTests`: `rackPatternIsAnyMaterial`, `ironBlockStaysExact`, `secondAnyRackRefused`, `exactRackBesideAnyRackStored`. Verify: `gradlew :shuttles:runData`, `gradlew :shuttles:runGameTestServer`.
- [x] 2.2 `pattern/PatternLibrary.Entry.variants` and `pattern/PatternRunners.withPatterns`: one planner recipe per variant through `VariantExpansion`, component cells take stocked and made keys in a second pass. GameTests: `spruceRackFromOakPattern`, `sprucePlanksCraftedForRack`, `spruceElevatorUsesSpruceRail`, `exactOakElevatorCraftsOakRail`. Verify: `gradlew :shuttles:runGameTestServer`.

## 3. Orders

- [x] 3.1 `order/OrderBook`: resolve a generic key before planning in `create` and `preview`, return the resolved key, `runs` for material variants; `cpu/CpuBlockEntity.patternFor` re-assembles the variant's result; `cpu/CpuOrders` and `terminal/TerminalRequests` make the request for the resolved key. GameTests: `genericRackTakesMostStockedWood`, `genericRackWithoutStockCraftsOwnPlanks`, `oneOffAnyRackTakesMostStockedWood` (in `OrderGridGameTests`). Verify: `gradlew :shuttles:runGameTestServer`.
- [x] 3.2 `terminal/TerminalCraftables`: one generic entry per Any material pattern with its tooltip line, variant outputs not listed again, cache key with the generic keys. GameTest `craftableListShowsOneRack`. Verify: `gradlew :shuttles:runGameTestServer`.

## 4. Screens

- [x] 4.1 `client/PatternGridClient`: cycling result for an Any material grid, "follows" tooltip line; `client/ProductionTerminalScreen` Patterns tab tooltip with the "follows" line; Storage Terminal craftable tooltip line. Verify: `gradlew :shuttles:build`, and in the client JEI + on the rack opens both grids as Any with a cycling result.

## 5. Integration

- [x] 5.1 Full `gradlew build`, `gradlew test`, `gradlew :shuttles:runGameTestServer` and `gradlew :foundation:runGameTestServer` green, the game launches with all mods. Then the user's play check from main: JEI + on the Heavy Duty Rack gives an Any rack pattern in the Production Terminal and an Any order grid in the Storage Terminal; the result cycles through woods; the craftable list shows one rack; crafting it with mostly birch planks in stock makes birch racks; a request for spruce racks uses or crafts spruce planks; an Any Elevator uses a spruce Rail for a spruce Elevator; a second Any rack pattern is refused; no slowdown when the craftable list or a preview opens.

## Workflow follow-up

- At archive: a line under `## Unreleased` in `shuttles/CHANGELOG.md` (Added: rack and Elevator patterns can be Any; the wood or material follows the inputs and the CPU uses what is in stock).
- PLAN.md: nothing to remove; add deeper material chains as an open point if the play check needs them.
