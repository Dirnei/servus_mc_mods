# Spec Delta

## Purpose

The game data export writes the effective balance data of the Servus mods (machine stats, recipes, deposits, techs, display names) to one versioned JSON file, so tools outside the game, such as the documentation site, see the numbers the game really uses.

## ADDED Requirements

### Requirement: Data run writes the game data file
The Manufacturing data run SHALL write `docs/src/data/gamedata.json` at the repository root. The file SHALL contain a schema version number, the mod version, and sections for machines, recipes, deposits, techs and items. The file SHALL be committed like other datagen output.

#### Scenario: Data run
- **WHEN** `gradlew :manufacturing:runData` finishes
- **THEN** `docs/src/data/gamedata.json` exists and its `schema` field is `1`

### Requirement: Machine stats are effective values
Each machine entry SHALL hold the stats the machine uses at runtime: the values from its datapack file merged over its code defaults, so fields missing from the file appear with their default value. Each entry SHALL state its kind (burner, powered, crafter or generator) and the recipe types it runs.

#### Scenario: Partial machine file
- **WHEN** `burner_extractor.json` only sets `work_ticks` to 80
- **THEN** the burner extractor entry has `work_ticks` 80, `energy_per_tick` 0, `fuel_divisor` 1 and kind `burner`

#### Scenario: Default without a file field
- **WHEN** `crusher.json` sets no `work_ticks`
- **THEN** the crusher entry carries the crusher's code default for `work_ticks`

### Requirement: Recipes are exported with effective times
The export SHALL list every Servus assembling and crushing recipe, Servus smelting and blasting recipes, and the vanilla smelting recipes whose input or result appears in a Servus recipe, deposit yield or tech cost. Each recipe SHALL have its id, type, inputs with counts, outputs with counts, and its time with the codec default applied when the file omits it. Tag inputs SHALL be exported as tags.

#### Scenario: Recipe without a time
- **WHEN** the `crushing/raw_iron` recipe file has no `time` field
- **THEN** its exported time is the crushing default

#### Scenario: Vanilla smelting needed by a chain
- **WHEN** a deposit yields raw iron
- **THEN** the vanilla recipe smelting raw iron into an iron ingot is in the export

### Requirement: Deposits, techs and items are exported
The export SHALL list each deposit with its block and yield, each tech with its parents, cost, `ticks_per_pack` (default applied) and unlocks, and each item referenced anywhere in the file with its display name. Servus item names SHALL come from the English language file; vanilla names SHALL come from the vanilla English language data.

#### Scenario: Tech with default lab time
- **WHEN** a tech file omits `ticks_per_pack`
- **THEN** its exported `ticks_per_pack` is the default

#### Scenario: Display names
- **WHEN** the export references `servus_manufacturing:iron_gear` and `minecraft:iron_ingot`
- **THEN** their names are "Iron Gear" and "Iron Ingot"

### Requirement: Output is stable
The export SHALL be deterministic: entries sorted by id, keys in a fixed order, pretty printed. It SHALL NOT contain timestamps or commit hashes, so running the data run twice on the same sources gives the same bytes.

#### Scenario: Repeat run
- **WHEN** the data run runs twice without source changes
- **THEN** `gamedata.json` is byte identical after both runs

### Requirement: CI rejects a stale export
The build workflow SHALL run the Manufacturing data run and fail when `docs/src/data/gamedata.json` differs from the committed file.

#### Scenario: Hand edited machine file
- **WHEN** a commit changes `assembler.json` but not `gamedata.json`
- **THEN** the build workflow fails and names `gamedata.json` as out of date
