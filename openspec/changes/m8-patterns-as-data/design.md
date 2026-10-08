# Design

## Context

`m8-pattern-storage` built patterns as items: `PatternItem` (Blank Pattern and Pattern), the `servus_storage:pattern` data component holding `EncodedPattern`, a 9 slot `ItemStackHandler` in `PatternStorageBlockEntity`, `PatternStorageMenu`/`PatternStorageScreen`, and `PatternEditing` consuming and returning Blank Patterns. `PatternLibrary`, `PatternRunners`, the CPU and station jobs only read `EncodedPattern` through `PatternStorageBlockEntity.pattern(slot)`. See proposal.md for why.

## Goals / Non-Goals

**Goals:** patterns as block entity data with the same slot addressing, so `PatternLibrary`, the terminal payloads and the CPU stay unchanged; no item, menu or recipe left for patterns; patterns survive mining the block.

**Non-Goals:** see proposal.md.

## Decisions

- **Storage shape.** `PatternStorageBlockEntity` keeps an `EncodedPattern[]` of `max(slots(), saved size)` places, `null` for free places. Saved as a list of `{slot, pattern}` entries with the existing `EncodedPattern.CODEC`. The public API (`capacity`, `pattern(slot)`, `usedSlots`, `firstFree`, `set`, `clear`, a new `size()`) stays, so `PatternLibrary` and `PatternEditing.storageAt` only lose their `patterns()` handler use.
- **Carried by the item.** A new data component `servus_storage:patterns` (list of `{slot, pattern}`) replaces `servus_storage:pattern`. The block entity fills it in `collectImplicitComponents` and reads it in `applyImplicitComponents`, like the Heavy Rack's wood; the loot table copies it from the block entity (`CopyComponentsFunction`), and the carrot dismantle builds the item with `saveToItem`-equivalent components (`BlockEntity.collectComponents()`), so both paths keep the patterns. Chosen over dropping patterns as loose data or losing them, because mining a block should not destroy work.
- **Action bar read out.** `useWithoutItem` shows `message.servus_storage.pattern_storage.count` ("%s of %s patterns"); used counts every kept pattern, total is `capacity()`.
- **Editing.** `PatternEditing.save` drops the Blank Pattern step and returns `no_storage` when the line has no Pattern Storage, `full` when none has room; `delete` returns nothing to the player. The `no_blank` lang key goes, `no_storage` comes. The `Player` parameter stays for the menu call shape.
- **Recipe.** Pattern Storage: iron ingots around paper, redstone and a chest (`IPI`, `RCR`, `IPI`), so it stays cheap without blanks.
- **Tooltip helper.** `PatternItem.stacks(...)` used by the terminal screen moves to `CraftingGrid.stacks(...)` (or a small static helper) before `PatternItem` is deleted.
- **Removed.** `PatternItem`, `BLANK_PATTERN`, `PATTERN` items and the `pattern` component, `PatternStorageMenu`, `PatternStorageScreen` and its menu type and screen registration, recipes `blank_pattern`, `blank_pattern_from_pattern` (wipe), item models, textures, lang, docs icons for both items, the docs mentions.

### NeoForge APIs
`BlockEntity#applyImplicitComponents`, `collectImplicitComponents`, `removeComponentsFromTag`, `CopyComponentsFunction.copyComponents(Source.BLOCK_ENTITY)` (same as `HeavyRackBlockEntity` and `ServusStorageBlockLoot` already use, 1.21.1), `DataComponentType` registration in `ServusStorageComponents`.

### Testing
GameTests in `PatternGameTests` (rewritten): capacity from JSON and data storage, save without blanks, refusals (`no_storage`, `full`, `duplicate`, `no_match`), delete, break and place keeps patterns (loot drop through `Block.dropResources` and placing the stack with its components), terminal snapshot. `StationJobs` and other tests that put pattern items into storages use `set(slot, pattern)` instead. No pure logic changes, so no new JUnit tests.

### PLAN.md open decisions
None; PLAN.md 2.4 says patterns live in Pattern Storage, which now holds them directly. The wording that mentions pattern items there is updated.

## Risks / Trade-offs

- [Dev worlds from the play test lose Pattern items and saved slots] -> unreleased; the saved key changes, old Pattern Storages load empty.
- [Patterns are now free] -> a capacity per block remains the only cost; balance later if needed.
