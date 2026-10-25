## 1. Precondition

- [x] 1.1 Check that `m10-field-harvest-buffer` is implemented on main (`field/HeadUnload`, the Field Head To storage buffer, `FieldRuns.storeAtHead`) and archived, so `openspec/specs/shuttles/harvester/spec.md` "Field jobs" names the To storage free slot; if it is not archived yet, stop and archive it first. Check: `openspec list`, `gradlew :shuttles:test`.

## 2. Pure logic

- [x] 2.1 `field/FieldKind`, `field/TreeSpots` (single spots on the even grid, squares with a gap, complete squares, negative coordinates). Add `TreeSpotsTest`: even grid; negative x and z; square members and gap column; square cut off by a missing plot. Check: `gradlew :shuttles:test`.
- [x] 2.2 `field/TreeWalk` (logs through 26 neighbours from the base, never below it or into another base; leaves that no other log reaches within 6 steps; attachments touching removed blocks; `TooLarge`, `NotATree`). Add `TreeWalkTest`: oak; two touching crowns; diagonal acacia branch; 2x2 base; neighbour trunk at base level; log limit; block limit; log pillar without leaves; vines. Check: `gradlew :shuttles:test`.
- [x] 2.3 `field/TreePlotWork` (fell, plant; square needs four), `PlotState.IDLE` in `field/PlotState` and `field/FieldRipeness`, `field/SeedPlan` counting four per square, `field/FieldDue` and `field/FieldWork` for Trees (`NO_LUMBER`, `NO_LUMBER_WAY`, `NEEDS_RANGE`, `TREE_TOO_LARGE`, no `DRY`). Add `TreePlotWorkTest`; extend `FieldRipenessTest` (idle neither planted nor open), `SeedPlanTest`, `FieldDueTest`, `FieldWorkTest`. Check: `gradlew :shuttles:test`.

## 3. Field Head

- [x] 3.1 `field/FieldHeadBlockEntity`: kind and 2x2 flag saved (missing reads Crops), kind aware `isCropItem` and sapling list from `ItemTags.SAPLINGS`, `setKind`, Range Upgrade rule for 2x2, no water and no aging for Trees, `unfellable` plots. Item tags `servus_shuttles:saplings_2x2`, `servus_shuttles:saplings_2x2_only` in `datagen/ServusShuttlesItemTagsProvider`; block tags `servus_shuttles:tree_logs`, `servus_shuttles:tree_attachments` in `datagen/ServusShuttlesBlockTagsProvider`. Check: `gradlew :shuttles:runData`, `gradlew :shuttles:build`.
- [x] 3.2 `field/HarvesterWork`: `PlotRules` and the Trees branch of `classify` (sapling, log, open spot, idle, blocked), used by the head's passes, `refreshReach` and the Spreader's `dosePlot`. Check: `gradlew :shuttles:build`.
- [x] 3.3 `field/FieldEvents.onGrowFeature` on `BlockGrowFeatureEvent` (lowest priority, skipped when cancelled) queues the position and its eight horizontal neighbours. Check: `gradlew :shuttles:build`.
- [x] 3.4 Field tab kind buttons and sapling picker: `ChooseFieldKindPayload`, `twoByTwo` on `field/ChooseFieldCropPayload`, `menu/FieldHeadMenu` opening data, `client/FieldHeadScreen`, sapling entries next to `client/CropPickerEntries`, greyed 2x2 with hint, "Trees need no water" on the Water tab, status lines and markers; field command prints kind and sapling (`command`); lang in `datagen/ServusShuttlesLanguageProvider`. Check: `gradlew :shuttles:runData`, `gradlew :shuttles:build`, every new payload registered.

## 4. Lumber module and runs

- [x] 4.1 `shuttle/module/ModuleSpec.LUMBER`, `shuttle/module/JobNeeds.LUMBER`, `registry/ServusShuttlesItems.LUMBER` (field only, wide, creative tab), recipe in `datagen/ServusShuttlesRecipeProvider`, item model and lang, tooltip. Check: `gradlew :shuttles:runData`, `gradlew :shuttles:build`.
- [x] 4.2 `shuttle/FieldJob` kind and needs, `shuttle/FleetSaves` saves the kind (missing reads Crops); `network/WarehouseNetwork.tickFields` and `shuttle/ShuttleFleet` reach and shuttle checks per kind. Check: `gradlew :shuttles:build`.
- [x] 4.3 `field/HarvesterWork.fell` (snapshot, `TreeWalk`, drops by loot table, removal with `Block.UPDATE_CLIENTS` leaves first, break sound and particles at the base) and sapling planting for spots and squares; `shuttle/FieldRuns` `fellAndPlant` with drops rolled first, saplings kept from the drops, `fell_ticks` with Handling, kind change ends the run after unloading; `fell_ticks`, `max_tree_logs`, `max_tree_blocks` in `stats/ShuttlesStatsManager` and `resources/data/servus_shuttles/shuttles/field.json`. Check: `gradlew :shuttles:build`.

## 5. Look

- [x] 5.1 `datagen/ShuttleModels.lumber`: turret, folded two part arm and felling head in `STEEL` and `DARK_IRON` on every face, below the spoiler and clear of the Handling mast and crane; list every pair of overlapping boxes with whether faces share a plane, fix by ending at the neighbour's face or insetting 0.05 px. Check: `gradlew :shuttles:runData`, `gradlew :shuttles:build`.

## 6. GameTests

- [x] 6.1 `gametest/LumberGameTests`: an oak grown by bone meal on a spot counts ripe; old trees count after the first pass; a Lumber shuttle fells an oak with leaves, logs and saplings go to the To storage buffer and seed part, no item on the ground, no leaf left; the neighbour oak keeps its leaves; a sapling is replanted from the drops; a 2x2 dark oak square is planted only with four saplings and needs a Range Upgrade; a tree over `max_tree_logs` stays and the status says so; a log pillar stays; a Harvester takes no tree job and a Lumber shuttle no crop job; the Spreader grows a sapling and stops; a Trees field uses no water; a run survives a save and load; a head saved without a kind loads as Crops. Check: `gradlew :shuttles:runGameTestServer`.

## 7. Plan and integration

- [x] 7.1 At archive: groom `PLAN.md` (remove `m10-lumber` from Open: Shuttles, cut the open decision to "other field kinds and which modules they need", add an open point for Crimson and Warped fungi on nylium) and add a line under `## Unreleased` in `shuttles/CHANGELOG.md` (Added: tree fields and the Lumber module). Check: `openspec validate m10-lumber --strict`.
- [x] 7.2 Full `gradlew build`, `gradlew test`, `gradlew :shuttles:runGameTestServer` green, the game launches with all mods; then the user's play check from main: a Trees field of oak and a 2x2 dark oak field, felling, replanting, Spreader on saplings, the Lumber tool on the shuttle from several angles without flicker.
- [x] 7.3 Play check fix: `foundation/picker/PickerList.ranked` orders picker entries none first, pickable before locked, stock most first, then name; `foundation/client/ItemPicker.refilter` uses it. JUnit in `PickerListTest`: `rankedPutsPickableFirstThenMostStockThenName`, `rankedWithoutStockKeepsNameOrderAndLocksLast`. Check: `gradlew :foundation:test`, `gradlew build`.
- [x] 7.4 Play check fix: a Trees field reads its plots again when its sapling or plant mode changes, not only its layout (`field/FieldHeadBlockEntity` `setCrop`, `setSapling`, `setHarvestOnly`), so choosing oak on a serving field opens the grass spots. GameTest `LumberGameTests.choosingSaplingOpensSpots`; the oak field tests get 64 saplings now that the other spots take some. Check: `gradlew build`, `gradlew :shuttles:runGameTestServer`.
