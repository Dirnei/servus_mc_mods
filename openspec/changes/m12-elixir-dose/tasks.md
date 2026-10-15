# Tasks

## 1. Pure dose

- [ ] 1.1 Pure `ElixirDose` in `alchemy/src/main/java/.../alchemy/sight/` (design D1). JUnit `alchemy/src/test/java/.../alchemy/sight/ElixirDoseTest`: `chanceZeroNeverPoisons`, `chanceOneAlwaysPoisons`, `rollBelowChancePoisons`, `sightedAddsExtraChance`, `extraChanceIsCapped`, `harmlessNeverPoisons`, `rejectsChanceOutsideRange`. Verify: `gradlew :alchemy:test` green.

## 2. Poison on drinking

- [ ] 2.1 `ElixirDoseData` (D2) registered in `AlchemyEvents`; `data/servus_alchemy/servus_alchemy/elixir_dose/elixir_of_sight.json`; `ElixirOfSightItem.drink` and `finishUsingItem` (D3); lang key `message.servus_alchemy.elixir_of_sight.poisoned` in `ServusAlchemyLanguageProvider`; `:alchemy:runData`. GameTests in `AlchemyGameTests`: `elixirChanceOnePoisonsAndGrantsSight`, `elixirChanceZeroNeverPoisons` (50 drinks), `shippedElixirDoseLoads`. Verify: `gradlew build`, `:alchemy:runGameTestServer` green.

## 3. Texts and docs

- [ ] 3.1 Journal: Schwaz tasks page (`JournalEntries`) and Workshop "Elixir of Sight" spotlight (`WorkshopCategory`) mention the dose (D4); `:alchemy:runData`. Docs: `docs/src/content/docs/mods/alchemy.md` and new `docs/src/content/docs/reference/datapack/elixir-dose.md`. Verify: `gradlew build` green, generated journal JSON holds the dose text, no em dash in the diff.

## 4. Integration

- [ ] 4.1 Full `gradlew build test :alchemy:runGameTestServer` green.
- [ ] 4.2 User play check in game:
  - Drinking several elixirs: now and then Poison and Nausea set in, the action bar scolds about the dose, and Elemental Sight is there every time.
  - Drinking again while Sight is active poisons more often (about twice as often).
  - The Schwaz chapter and the Workshop elixir page mention the dose.
