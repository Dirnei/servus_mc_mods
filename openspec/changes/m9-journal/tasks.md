# Tasks

## 1. Dependency and removal

- [x] 1.1 Add the KliKli Maven repository and `modonomicon_version` to `build.gradle` and `gradle.properties`, `implementation` in `manufacturing/build.gradle`, `runtimeOnly` for the combined runs, and a required `modonomicon` dependency in `manufacturing/src/main/templates/META-INF/neoforge.mods.toml`. Verify: build green, Modonomicon loads in the Manufacturing GameTest server.
- [x] 1.2 Remove `research/*`, `machine/lab/*`, `ResearchLabMenu`, `ResearchLabScreen`, `TechTreeScreen`, the research payloads and their registration, the techs registry, the lab's registry entries, datagen, stats file, textures and lang, `ManufacturingMachines.RESEARCH` and `ResearchStateTest`. Verify: build, data run, GameTests and docs tests green.

## 2. Ledger and chapters

- [x] 2.1 Pure `JournalLedger`, `ChapterDef`, `TaskDef`, `TeamJournal` in `manufacturing/.../journal` with JUnit `JournalLedgerTest` (first chapter open, parent gating, counting with a matcher, cap at count, chapter finishes once, finished chapters ignore events, state round trip of counts). Verify: `gradlew test`.
- [x] 2.2 `JournalChapters` reload listener with the chapter codec, `JournalSavedData`, `Journal.record(...)` resolving filters (item id, `#tag`, principle) and awarding advancements on finish and at login; shipped `villach.json` and `schwaz.json`. Verify: build, `/reload` logs the chapter count.

## 3. Machines, recipes and advancements

- [x] 3.1 Calciner and Coagulator store the team owner (`OwnedMachine`, resolved at placement) and report `calcine`, `principle` and `coagulate` events; `CoagulationRecipe` gains `chapter`; the Coagulator refuses locked recipes and its screen greys them using client advancements. GameTests: the Calciner's work counts for its team; team events finish Villach and open a chapter locked recipe; the Villach advancement exists. Awarding to a player is owed to the play check (Modonomicon syncs book data on award, which a mock connection cannot carry). Verify: GameTests green.
- [x] 3.2 Datagen of the root and chapter advancements (`minecraft:impossible` criterion `done`), and the `/servus_manufacturing journal` command. Verify: data run writes the advancements; build green.

## 4. The book

- [x] 4.1 `JournalBookProvider` with Modonomicon's datagen: book, category, an entry per chapter with story, tasks and unlock pages, advancement conditions on later chapters; the book recipe (book + coal) and the first login gift. Verify: data run writes `data/servus_manufacturing/modonomicon/books/journal/...` and the recipe; GameTest that the recipe gives a stack with `modonomicon:book_id` set to the journal.

## 5. Docs and integration

- [ ] 5.1 Docs: Manufacturing page (journal instead of research), a datapack reference page for journal chapters replacing `techs.md`, regenerated `gamedata.json`. Verify: docs tests and docs build pass.
- [ ] 5.2 Full `gradlew build`, `gradlew test`, GameTests of all three mods green, and the client launches with all three mods: the journal is given on first login, Villach's tasks finish by calcining and coagulating, the Schwaz entry unlocks.
