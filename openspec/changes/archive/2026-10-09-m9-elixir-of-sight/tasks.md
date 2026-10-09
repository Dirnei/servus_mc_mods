# Tasks

## 1. Sight

- [x] 1.1 `ElementalSightEffect`, `ElementalSight`, `ElixirOfSightItem` in `manufacturing/.../alchemy/sight`, registration, lang, item model, placeholder item texture and effect icon, the Athanor craft `athanor/elixir_of_sight`. GameTests: drinking adds the effect and leaves a glass bottle; the Athanor makes the elixir at Albedo, not at Nigredo. Verify: build, data run, GameTests green.
- [x] 1.2 Client `RevealRenderers` with its `RevealRenderer` interface and render event handler (render only under the sight). Verify: build.

## 2. Journal and docs

- [x] 2.1 `Journal.recordEvent`, the `sight` event from the elixir, Schwaz's second task, the book's Schwaz text; GameTest that a recorded sight event with Villach finished counts for Schwaz. Verify: GameTests green.
- [x] 2.2 Docs: Manufacturing page (Elemental Sight, Elixir) and the journal and Athanor reference pages; regenerated `gamedata.json`. Verify: docs tests and build pass.
- [x] 2.3 Full `gradlew build`, `gradlew test`, GameTests of all three mods green, and the client launches with all three mods: brew and drink the elixir, see the effect. (Build, unit tests, docs tests and build, and GameTests green in the worktree: Foundation 70, Manufacturing 20, Storage 233; the Storage test shaftBreakDropsRiderCargo failed once on timing and passed on the rerun. The client part is owed to the play check from main.) Play checked by the user from main on 2026-10-09.
