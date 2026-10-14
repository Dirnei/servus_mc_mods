# Tasks

## 1. Tool models

- [x] 1.1 In `storage/.../datagen/ShuttleModels.java`, inset the Harvester arms and move the Spreader to the back as in design.md, with updated Javadoc and comments. List the face check from design.md against the generated `tool_harvester.json` and `tool_spreader.json`. Verify: `gradlew :storage:runData` and `gradlew build` green.

## 2. Docs and plan

- [x] 2.1 In `docs/src/content/docs/mods/storage.md`, say the Spreader carries its hopper and bar at the back; add `m10-tool-mounts` to the M10 list in `PLAN.md`. Verify: docs tests green.

## 3. Integration

- [x] 3.1 Full `gradlew build`, unit tests and storage GameTests green. The game launches with all three mods.
- [ ] 3.2 Play check in game by the user from main:
  - The Harvester shows no flicker on its arms and reel.
  - A Spreader shows its hopper and bar at the back, on Mk1 to Mk3, with the Handling mast and crane and the spoiler.
  - Nothing flickers from several angles while it drives.

  Verify: the user confirms in game.
