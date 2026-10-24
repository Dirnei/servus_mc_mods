# Proposal

## Why

The Calciner entry of the journal ends in "What Burns", a plain text list of every calcination recipe generated at build time. With the Nether and End matter from `m9-calcination-nether-end` it runs over six pages, gives everything away at once, and ignores datapack recipes. The journal should rather be a record of what the team has found out: a table that fills in as the team burns things, with Paracelsus hinting at what is still unknown.

## What Changes

- The "What Burns" text pages of the Calciner entry become a table page: one row per active calcination recipe, grouped under the headings Overworld, Nether and End, in that order, split cleanly over as many pages as it needs.
- The rows come from the recipes the client has, so datapack recipes show up and removed recipes go.
- A row is revealed for a team once the team's Calciner has burned anything with that recipe. A revealed row shows the item, its name and the Salt, Sulfur and Mercury it gives. A row not yet tried shows a question mark instead of the item, no name and no amounts, and a hint in Paracelsus' voice.
- Each team keeps the set of tried recipes in its journal world data; it survives a restart and reaches every online member at once, like the task progress.
- Hints are lang keys derived from the recipe id, with a generic "Not yet tried" text for recipes without one. Every shipped recipe gets a short English hint.
- JEI stays as it is and shows every recipe.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `alchemy/journal`: "Recipe lists from the shipped recipes" no longer covers calcination; new requirements for the calcination table, its reveal per team and its hints.

## Impact

- Journal state: `JournalLedger.TeamJournal` gets the tried recipes, `JournalSavedData` saves them, `JournalProgressPayload` and `ClientJournalProgress` carry them.
- `Journal` gets a calcination event that marks a recipe tried; `CalcinerBlockEntity.finishWork` reports the recipe it used.
- New pure classes for the table layout and the hint keys, with JUnit tests.
- New Modonomicon page type and renderer (`CalcinationTablePage`, `CalcinationTablePageRenderer`) registered like the task page; a datagen page model; the Calciner entry in `WorkshopCategory` uses it.
- `ServusAlchemyLanguageProvider`: table texts, a name and a hint per shipped recipe.
- GameTest: a calcine reveals the row for the team.
- `alchemy/CHANGELOG.md` gets its line at archive time.

## Non-goals

- Hiding recipes in JEI.
- Revealing rows by any other way than the Calciner (commands, chapters).
- Tables for the Coagulator or the Athanor.
