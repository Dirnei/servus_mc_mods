# Design

## Context

- The Calciner entry (`WorkshopCategory.calciner()`) ends in `e.list("burns", "What Burns", ...)`: text pages built at datagen from `AlchemyRecipeTable.calcination()`.
- The task page is the precedent for a page drawn from team state: `JournalTasksPage` (a Modonomicon `BookPage` registered with `BookPageTypeRegistry`), `JournalTasksPageModel` (datagen), `JournalTasksPageRenderer` (registered with `PageRendererRegistry` in `ServusAlchemyClient`), fed by `JournalProgressPayload` into `ClientJournalProgress`. `JournalSync` sends it at login, after `/reload` (`OnDatapackSyncEvent`) and at the end of every tick for teams marked changed.
- Team journal state is `JournalLedger.TeamJournal` (pure), saved by `JournalSavedData` under `Teams`.
- `m9-calcination-nether-end` added `CalcinationRecipe.dimension()`, `CalcinationRecipes.forItem/burns` and the rule that the most specific recipe wins.
- Modonomicon 2.17.0 for 26.1.2: `BookEntryScreen.PAGE_WIDTH` 124, `PAGE_HEIGHT` 155; the task page draws its title at y 0 and its content from y 19.

## Goals / Non-Goals

**Goals:**
- The table always matches the recipes the server runs, datapacks included.
- Reveal state is per team, persisted and live.

**Non-Goals:**
- Reveal state for JEI or for other recipe types.
- A way to forget tried recipes.

## Decisions

**Rows at runtime from the synced recipes.** Calcination recipes reach every client already: `ServusAlchemy` calls `ServusRecipes.syncToClients(CALCINATION)`, which sends the type with NeoForge's `OnDatapackSyncEvent.sendRecipes` (26.1 no longer syncs recipes to clients by default; this is the opt in), and the client stores them on `RecipesReceivedEvent` for `ServusRecipes.recipeMap(level)`. JEI reads the same map. So the renderer builds its rows from `ServusRecipes.all(clientLevel, CALCINATION)`, filtered to recipes whose `CalcinationRecipes.burns` is not empty, which keeps the book on the active recipes after every `/reload`. Alternative: bake rows into the page JSON at datagen. Rejected: datapack recipes would never show, removed ones would stay, and the shown items would not follow the most specific rule.

**Static pages, runtime content.** Modonomicon pages are fixed book data, so datagen writes a fixed number of `servus_alchemy:calcination_table` pages, each with its `part` (0, 1, ...) and `last` (true on the final one). Datagen computes the number of parts with the same pure layout from the shipped rows. At runtime the renderer lays out the active rows with `parts` as the page limit and draws its own part:
- fewer active rows than parts: the trailing parts show "These pages are still blank."
- more: the last part reserves a footer line "%s more: see JEI" with the count left out.
Alternative: one page with its own arrows to scroll. Rejected: Modonomicon already pages, and its buttons would fight the book's own page turning.

**Pure layout.** `alchemy/journal/CalcinationTable` (no Minecraft imports, JUnit `CalcinationTableTest`):
- `record Heights(int first, int rest, int heading, int row, int footer)`; `CalcinationTable.PAGE` is `(125, 144, 12, 20, 10)`: the first page loses 19 px to the title and every page 11 px to the column header line.
- `sealed interface Line<T>` with `Heading<T>(CalcinationDimension)` and `Entry<T>(T row)`.
- `layout(rows, id, dimension, heights, maxPages)` returns `Table<T>(List<List<Line<T>>> pages, int leftOut)`: rows grouped by dimension in enum order, sorted by id inside a group; a heading only goes where it and one row fit; a group that continues on a new page gets its heading again; at `maxPages` the last page keeps room for the footer and the rest counts as left out.
- `hintKey(id)` and `nameKey(id)`: `journal.servus_alchemy.calcination.<namespace>.<name>.hint` / `.name`, the name being the path without a leading `calcination/` and with `/` as `.`.
- Tests: group order and id order, a heading never last on a page, a continued group repeats its heading, page counts for the shipped numbers, the footer and left out count at a page limit, an empty group has no heading, the key derivation with and without the `calcination/` prefix and with nested paths.

```
first page                     next pages
+--------------------------+   +--------------------------+
|        What Burns        |   |  Salt  Sulfur  Mercury   |
|  Salt  Sulfur  Mercury   |   | Overworld                |
| Overworld                |   | [i] Stone                |
| [i] Bones                |   |     100    -      10     |
|     100    -      -      |   | [?] Hint text that may   |
| [?] Hint text that may   |   |     wrap onto two lines  |
|     wrap onto two lines  |   | ...                      |
+--------------------------+   +--------------------------+
```

**Row look.** A row is 20 px: a 16 px item at x 0, text from x 19 on two 9 px lines.
- Revealed: line one the name, line two three amount cells at x 19, 54 and 89 under the column header "Salt Sulfur Mercury" (the Calciner's Ash Pan, left tank, right tank order is the enum order Salt, Sulfur, Mercury), a 0 shown as "-". The icon cycles once a second through the items the recipe burns. The name is the lang key `nameKey(id)`, falling back to the hover name of the first item.
- Hidden: a parchment box with a "?" where the icon goes, and the hint, faded, wrapped to two lines, cut with "..." if longer.
- Ink colours on the parchment: the principle colours of `Principle` are for fluids on dark screens (Salt is nearly white), so the page uses darker inks of its own.

**Tried recipes per team.** `TeamJournal` gets `markTried(String recipeId)` (true when new) and `tried()`; `JournalLedgerTest` covers both. `JournalSavedData` writes them as a `Tried` string list per team and reads them back; `load` becomes public so a GameTest can round trip it. `Journal.recordCalcination(level, team, recipeId)` marks the recipe and, when it was new, sets the data dirty and queues the team in `JournalSync`. `CalcinerBlockEntity.finishWork` calls it with the id of the recipe it used, next to the existing `calcine` event; a Calciner without a team reports nothing, as today.

**Sync.** `JournalProgressPayload` becomes `(chapters, tried)` with the tried ids as a string list; `JournalSync.payload(TeamJournal)` builds it for `send` and for tests. `ClientJournalProgress` keeps `tried()` as a set. A newly tried recipe is sent at the end of the tick to every online member, so an open book redraws with the row revealed. The payload channel version stays "1": client and server always ship together.

**Lang.** `ServusAlchemyLanguageProvider` writes the title, column headers, group headings (`journal.servus_alchemy.calcination.dimension.<id>`), the blank page and footer texts, the generic hint "Not yet tried", and for every row of `AlchemyRecipeTable.calcination()` its name (the row label) and its hint. Hints are written there only, short (about 40 characters) so they fit two lines, and never name the item.

**Book.** `WorkshopCategory.calciner()` replaces `e.list("burns", ...)` with `e.calcinationTable()` on `JournalEntries.TextEntry`, which adds the parts `burns`, `burns_2`, ... through `CalcinationTablePageModel`. The old `burns_*` text pages and their lang lines disappear from the generated data.

**Verification.** GameTests in `AlchemyGameTests`: `calcineRevealsTheRowForTheTeam` (a Calciner of team A burns Nether Wart; A has `servus_alchemy:calcination/nether_wart` tried and in its payload, a team B does not) and `triedRecipesSurviveASave` (save and load of `JournalSavedData` keeps them). The page itself is checked in the play check.

## Risks / Trade-offs

- [A datapack adds many rows] → the last page names how many are left out and points to JEI.
- [Long hints from a translator] → cut with "..." after two lines; the lang keys stay editable.
- [Names of tag rows fall back to one item's name for datapack recipes] → a pack can add the `.name` key.
