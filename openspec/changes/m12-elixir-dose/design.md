# Design

## Context

See proposal.md for the why. `ElixirOfSightItem.finishUsingItem` adds Elemental Sight (`ElementalSight.DURATION`, a code constant) and records the journal event `sight`. Alchemy data lives in `SimpleJsonResourceReloadListener`s registered in `AlchemyEvents` (`VeinData`, `GnomeGiftData`). The machine table (`MachineStats`) fits machines, not an item, so the elixir gets its own small data file.

## Goals / Non-Goals

**Goals:**
- A poison roll whose numbers are data, whose math is pure and tested, and that GameTests can force without global state.

**Non-Goals:**
- Moving the Sight duration into data (stays a constant, unchanged by this change).
- Remedies, a Dosing Table or other dose mechanics from `PROGRESSION.md`.

## Decisions

### D1 Pure `ElixirDose`
`sight/ElixirDose` (no Minecraft imports) is a record `(poisonChance, extraChanceWhileSighted, poisonTicks, nauseaTicks)` with validation (chances 0 to 1, ticks at least 0). `chance(boolean sighted)` returns `min(1, poisonChance + (sighted ? extra : 0))`; `poisons(boolean sighted, double roll)` is `roll < chance(sighted)` for a roll in [0, 1), so chance 0 never and chance 1 always poisons. `HARMLESS` is all zeros. JUnit `ElixirDoseTest`.

### D2 Data
`sight/ElixirDoseData` loads `data/<ns>/servus_alchemy/elixir_dose/<name>.json`; the file id names the elixir item (`servus_alchemy:elixir_of_sight`). Format:
```json
{ "poison_chance": 0.15, "extra_chance_while_sighted": 0.15, "poison_ticks": 200, "nausea_ticks": 200 }
```
`extra_chance_while_sighted` and both tick fields are optional (0, 0, 0). `forItem(Item)` returns the dose or `HARMLESS`. Registered in `AlchemyEvents`.
- Alternative: a field in the machine table. Rejected: `MachineStats` is a fixed machine shape.

### D3 The drink
`finishUsingItem` (server side) reads `sighted` before adding the new Sight, then calls the static `ElixirOfSightItem.drink(ServerLevel, LivingEntity, ElixirDose)`, which adds Sight, rolls `level.getRandom().nextDouble()` and, when poisoned, adds `MobEffects.POISON` and `MobEffects.NAUSEA` (skipping a 0 duration) and sends `message.servus_alchemy.elixir_of_sight.poisoned` with `Player.sendOverlayMessage` (verified in 26.1.2 sources). `drink` returns whether it poisoned. GameTests call `drink` with a chance 1 or chance 0 dose: the test hook is the dose argument, so no global override can leak into parallel tests.

### D4 Texts
Action bar line: "The dose makes the poison! Too much, you fool, too much." Schwaz tasks page and the Workshop "Elixir of Sight" spotlight get one sentence each about the dose.

## Risks / Trade-offs

- [Poison cannot kill (vanilla leaves 1 heart), so the risk is mild] → Intended: a warning, not a death trap. Packs can raise the numbers.
- [A rare unlucky streak poisons twice in a row] → Plain independent rolls; 15 % keeps it rare.

## Assumptions to confirm with the user

- 15 % chance per drink, Poison 10 s (amplifier 0) plus Nausea 10 s; numbers in `elixir_dose/elixir_of_sight.json`.
- Dose stacking is built: drinking while Sight is still active adds 15 percentage points (so 30 %), capped at 100 %; it does not grow further with more drinks (one extra step only, no counter).
- Sight is always granted, poisoned or not; the Sight duration stays three minutes and is not in data.
- Only the action bar line, no chat line; no sound beyond vanilla's.
- Without a data file the elixir is harmless.
- Journal mentions are one sentence in Schwaz and one in the Workshop entry; the quote is Paracelsus' famous 1538 sentence (historical, fine to keep).

## Ideas

- A real dose counter (each drink under Sight adds more, decaying over time) or a remedy that lowers the chance, for a later Citrinitas "Dosing" stage.
