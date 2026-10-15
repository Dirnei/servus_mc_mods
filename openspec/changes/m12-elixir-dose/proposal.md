## Why

`alchemy_plan.md` (Story) decides "the dose makes the poison": drinking the Elixir of Sight has a random chance to poison the player. Today the elixir is harmless, so nothing reflects Paracelsus' most famous sentence or the cost of leaning on the elixir, which also prepares the homunculus story (later the player should no longer need to drink it).

## What Changes

- Drinking an Elixir of Sight rolls a chance (example 15 %) to poison the drinker: Poison (example 10 s) plus Nausea (example 10 s). Elemental Sight is granted either way.
- Dose stacking: drinking while Elemental Sight is still active adds an extra chance (example +15 %), capped at certain poisoning.
- The numbers live in a datapack file `data/<ns>/servus_alchemy/elixir_dose/<elixir>.json`; without a file the elixir is harmless.
- A poisoned player gets an action bar line in Paracelsus' voice.
- Journal: the Schwaz chapter and the Workshop entry "Elixir of Sight" mention the dose. Docs: the Alchemy page and a new datapack reference page.

This change builds on the applied but unarchived `m12-hidden-hollow`, `m12-gnome-gifts`, `m12-athanor-citrinitas-rubedo` and `m12-principle-dropper`. None of them touches `alchemy/elemental-sight`, so the delta is written against the main spec.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `alchemy/elemental-sight`: the Elixir of Sight requirement gains the poison chance, dose stacking, the data file and the action bar line.

## Impact

- `alchemy/.../sight/`: new pure `ElixirDose`, new reload listener `ElixirDoseData`, `ElixirOfSightItem` rolls the dose.
- `AlchemyEvents` registers the listener.
- Data: `data/servus_alchemy/servus_alchemy/elixir_dose/elixir_of_sight.json`.
- Datagen: lang (`ServusAlchemyLanguageProvider`), journal texts (`JournalEntries`, `WorkshopCategory`).
- Docs: `docs/src/content/docs/mods/alchemy.md`, new `docs/src/content/docs/reference/datapack/elixir-dose.md`.
- Tests: JUnit `ElixirDoseTest`, GameTests in `AlchemyGameTests`.
