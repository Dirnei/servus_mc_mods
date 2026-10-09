# Design: m9-journal

## Context

See proposal.md for why. After `m9-tria-prima` Manufacturing has the Calciner, the Coagulator, the principle fluids and a Research Lab without techs. Foundation offers `Teams.teamOf(ServerPlayer)` (FTB Teams when present, else the player), `OwnedMachine` (set from `MachineBlock.setPlacedBy`) and the machine base. Research code (`research`, `machine/lab`, payloads, screens) is the part that goes.

Modonomicon was verified before this design: `com.klikli_dev:modonomicon-1.21.1-neoforge` version 1.120.7 (2026-09-25) is on `https://dl.cloudsmith.io/public/klikli-dev/mods/maven/`, resolves in the build and loads in the GameTest server. Its jar contains the condition id `modonomicon:advancement` (field `advancement_id`, checked against the reading player's advancements), the item `modonomicon:modonomicon` with the data component `modonomicon:book_id`, and the datagen API `NeoBookProvider.of(GatherDataEvent, BookSubProvider...)`, `SingleBookSubProvider`, `CategoryProvider`, `EntryProvider`, `BookModel`, `BookCategoryModel`, `BookEntryModel` and `BookAdvancementConditionModel` (inspected with `javap`).

## Goals / Non-Goals

**Goals:**
- Chapters and tasks as data, per team progress, advancements as the bridge to the book, recipe locks, the book itself.
- Research code gone, all tests green.

**Non-Goals:**
- Tasks for the Athanor, sight and gnomes (later changes add events and chapters).
- Live progress inside Modonomicon pages.

## Decisions

### D1: Pure ledger for task progress

`JournalLedger` (pure Java): chapters as `ChapterDef(id, parent, order, tasks)`, tasks as `TaskDef(id, event, filter, count)`, and a `TeamJournal` state of counts per task and the finished chapters. `record(state, event, matcher, amount)` adds the amount to every matching task of every open chapter (parent finished or none, itself not finished), caps at the count and returns the chapters that just finished. The filter test is a `Predicate<TaskDef>` from the Minecraft side (item id, `#tag` or principle name), so the ledger stays free of Minecraft types and is covered by JUnit.

### D2: Chapters load as data

`JournalChapters` is a `SimpleJsonResourceReloadListener` on `servus_manufacturing/journal`, decoding a `Codec` of the chapter JSON:

```json
{
  "order": 0,
  "tasks": [
    {"id": "calcine", "event": "calcine", "count": 100},
    {"id": "salt", "event": "principle", "filter": "salt", "count": 1000},
    {"id": "salt_crystal", "event": "coagulate", "filter": "servus_manufacturing:salt_crystal", "count": 1}
  ]
}
```

`parent` is optional. Chapter ids are `<namespace>:<file>`.

### D3: Per team state in SavedData

`JournalSavedData` on the overworld keeps a `TeamJournal` per team UUID. Machine owners are stored as the team UUID resolved at placement (`setOwner` resolves the placing player's team through `Teams.teamOf`), so events of offline owners still go to the right team.

### D4: Advancements as the bridge to the book

Datagen writes a hidden root advancement and one advancement `servus_manufacturing:journal/<chapter>` per shipped chapter, each with a single `minecraft:impossible` criterion `done`. When a chapter finishes, the server awards `done` to every online member (`Teams.teamOf(player) == team`); on `PlayerLoggedInEvent` it awards every finished chapter of the player's team. Awarding is idempotent. Modonomicon entries use `modonomicon:advancement` conditions on the parent chapter's advancement.

Alternative: Modonomicon's own entry read or unlock conditions. Rejected: they are per player and know nothing of teams or our tasks.

### D5: The book through Modonomicon's datagen

`JournalBookProvider extends SingleBookSubProvider` builds book `servus_manufacturing:journal` with one category "Opus" and an entry per chapter (story page, task page, unlock page), wired with `NeoBookProvider.of(event, provider)` in `ServusManufacturingDataGen`. Book texts go into the Manufacturing language file through the sub provider's language consumer. The story text is a first draft with historical claims kept to well known facts (Villach and his father, Schwaz and the mines).

### D6: The book item

The journal is `modonomicon:modonomicon` with component `modonomicon:book_id = servus_manufacturing:journal`. Datagen writes a shapeless recipe (book + coal) with that `ItemStack` result through `ShapelessRecipeBuilder.shapeless(RecipeCategory, ItemStack)` (verified in the NeoForge 21.1.255 patched sources). First login: a `PlayerLoggedInEvent` handler checks a flag in the player's persistent data and gives the stack once.

### D7: Events from machines

`Journal.record(ServerLevel, UUID team, String event, ItemStack|Principle, int amount)` is called from `CalcinerBlockEntity.finishWork` (calcine with the item, principle per principle with mB) and `CoagulatorBlockEntity.finishWork` (coagulate with the result). Tasks match the filter against the item id, an item tag (`#ns:path`) or a principle name.

### D8: Recipe locks

`CoagulationRecipe` gains `Optional<ResourceLocation> chapter`. `CoagulatorMenu.clickMenuButton` refuses a locked recipe; the screen greys locked rows. The screen names the needed chapter in the recipe tooltip; it does not grey rows, since the client has no simple public view of advancement progress.

### D9: Removal

Delete `research/*`, `machine/lab/*`, `ResearchLabMenu`, `ResearchLabScreen`, `TechTreeScreen`, `ResearchSyncPayload`, `SelectResearchPayload`, the techs data registry, the lab registration, lang and stats file, and the `ManufacturingMachines.RESEARCH` constant. The game data export keeps its `techs` section empty.

### NeoForge and Minecraft APIs

`SimpleJsonResourceReloadListener`, `AddReloadListenerEvent`, `SavedData`, `PlayerLoggedInEvent`, `PlayerAdvancements.award`, `ServerAdvancementManager.get`, `AdvancementProvider` with `AdvancementSubProvider`, `RegisterCommandsEvent`, `Player.getPersistentData`. All already in use in this repository or in vanilla 1.21.1.

### Pure and Minecraft logic

Pure, JUnit: `JournalLedger` (open chapters, counting, caps, chapter finish, parent gating, nothing counts twice after finish). GameTest: Calciner and Coagulator events advance Villach; finishing grants the advancement to an online member (mock player); locked coagulation recipe refused. Manual: book look, first login, command output.

## PLAN.md decisions settled

PLAN.md 2.1 left the book format and how chapters gate progress; settled as above. PLAN.md section 5 is not affected.

## Assumptions to confirm with the user

1. **Modonomicon 1.120.7** for 1.21.1, a required dependency.
2. **Villach tasks**: calcine 100 things, collect 1,000 mB Salt, coagulate a first Salt Crystal.
3. **Schwaz** opens after Villach with no tasks of its own yet; its tasks come with the Athanor and gnome changes.
4. **Team progress through advancements**: finished chapters show as advancements in the vanilla advancements screen; task counts only through `/servus_manufacturing journal`.
5. **Machine owner is the team** resolved at placement.
6. **Journal item**: crafted from a book and coal, and given once on first login.
7. **Story draft**: short first person journal style, in English only; facts limited to Villach, his father, metals, Schwaz and its mines.
8. **No recipe is locked yet** in Nigredo; the `chapter` field is ready for Albedo recipes.

## Risks / Trade-offs

- [Modonomicon datagen API differs from what javap shows] → Build the book provider first and run datagen before writing story text; fall back to hand written book JSON from the generated shape.
- [Advancement grant timing] → Award on chapter finish and on every login; awarding twice is a no-op.
- [Coagulator spec delta modifies a spec that only exists after `m9-tria-prima` is archived] → Archive `m9-tria-prima` first.

## Migration Plan

None. Dev worlds lose Research Labs.
