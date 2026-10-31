# Tasks

## 1. Name

- [x] 1.1 `foundation/.../datagen/MachineDataParts.java`: block name "Incinerator" and the casing, incomplete, mixed stone and chimney messages say Incinerator; Javadoc of `SinkBlock` notes the shown name. Run `:foundation:runData` and check `foundation/src/generated/.../lang/en_us.json` has no "Sink" left. Run `:foundation:build`.
- [x] 1.2 `alchemy/.../datagen/book/WorkshopCategory.java`: the journal says "pour into an Incinerator". Run `:alchemy:runData` and `:alchemy:build`.
- [x] 1.3 `PLAN.md`: Sink becomes Incinerator in the mod table and text.

## 2. Verify

- [x] 2.1 Full `gradlew build` and tests green, the game launches with all mods; in game the block, its GUI title and the carrot messages say Incinerator, and a Sink from an older world loads as an Incinerator.
