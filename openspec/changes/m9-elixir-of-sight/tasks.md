# Tasks

## 1. Sight

- [ ] 1.1 `ElementalSightEffect`, `ElementalSight`, `ElixirOfSightItem` in `manufacturing/.../alchemy/sight`, registration, lang, item model, placeholder item texture and effect icon, the Athanor craft `athanor/elixir_of_sight`. GameTests: drinking adds the effect and leaves a glass bottle; the Athanor makes the elixir at Albedo, not at Nigredo. Verify: build, data run, GameTests green.
- [ ] 1.2 Client `RevealRenderers`, `RevealRenderer` and `RevealEvents` (render only under the sight). Verify: build.

## 2. Journal and docs

- [ ] 2.1 `Journal.recordEvent`, the `sight` event from the elixir, Schwaz's second task, the book's Schwaz text; GameTest that a recorded sight event with Villach finished counts for Schwaz. Verify: GameTests green.
- [ ] 2.2 Docs: Manufacturing page (Elemental Sight, Elixir) and the journal and Athanor reference pages; regenerated `gamedata.json`. Verify: docs tests and build pass.
- [ ] 2.3 Full `gradlew build`, `gradlew test`, GameTests of all three mods green, and the client launches with all three mods: brew and drink the elixir, see the effect.
