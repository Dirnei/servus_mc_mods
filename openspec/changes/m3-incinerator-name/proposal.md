# Proposal

## Why

Players read "Sink" as the basin you wash your hands in, not as a block that destroys surplus. The block already has a smoking chimney, so "Incinerator" says what it does at a glance. This refines the Sink of PLAN.md M3 (pumps, Sink, Ender Anchor), which is built and released; it is not an open point.

## What Changes

- Every player-facing text says Incinerator instead of Sink: the block name (and so the GUI title, tooltips, JEI and the creative tab), the forming and casing messages, the chimney message and the Alchemy journal page that mentions it.
- The block id stays `servus_foundation:sink`, as do the block entity, menu, recipe and lang keys, so released worlds, recipes and other mods' references keep working without migration.
- PLAN.md names it Incinerator.
- CHANGELOG lines (Changed) for Foundation and Alchemy.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `foundation/sink`: the block is shown to players as the Incinerator.

## Non-goals

- Renaming the block id, classes, spec path or lang keys (would need a migration for released worlds).
- The docs site: it describes released versions only and follows when a release ships the new name.

## Impact

- `foundation/.../datagen/MachineDataParts.java` (lang), generated `en_us.json` of Foundation.
- `alchemy/.../datagen/book/WorkshopCategory.java`, generated `en_us.json` of Alchemy.
- `PLAN.md`, `foundation/CHANGELOG.md`, `alchemy/CHANGELOG.md`.
