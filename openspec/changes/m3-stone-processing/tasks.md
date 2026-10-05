# Tasks

## 1. Recipes

- [ ] 1.1 Add the three gated assembling recipes and update the stone_processing tech icon and unlocks. Verify `gradlew build` and `gradlew :manufacturing:runData` write `recipe/assembling/stone.json`, `stone_bricks.json` and `glass.json` with the `tech` field.

## 2. Verification

- [ ] 2.1 Server check over RCON: an assembler with the stone recipe merged in and cobblestone in the inputs produces stone. `gradlew clean build` green. Commit.
