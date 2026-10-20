# Tasks

## 1. Pure logic

- [x] 1.1 `rausch/RauschLevel`, `LevelStats`, `HangoverStats`, `RauschStats`, `Rausch` (D1) in `brewery/src/main/java/io/github/dirnei/mc/servus/brewery/`; JUnit `RauschTest` and `RauschStatsTest` (adds and caps, decay per period, never below 0, levels at each threshold, ticks at level, due day at Drunk not Merry, morning same day and next day, Hair of the Dog, drinking after the Hangover, unordered thresholds, missing level falls back); verify `gradlew :brewery:test`

## 2. Data, effects and player data

- [x] 2.1 `rausch/RauschStatsManager` with `data/servus_brewery/rausch/stats.json` (D2); `rausch/RauschEffect`, `HangoverEffect`, `RauschEffects` (D3); `rausch/RauschAttachment` (D4); registration in `ServusBrewery`; verify `gradlew build`

## 3. Tracker, events and config

- [x] 3.1 `rausch/RauschTracker` and `rausch/RauschEvents` (reload listener, `BeerDrunkEvent`, `PlayerTickEvent.Post`, `LivingKnockBackEvent`) (D5, D6); verify `gradlew build`
- [x] 3.2 `config/ServusBreweryClientConfig` registered as client config in `ServusBrewery`, `client/RauschClient` (D7); verify `gradlew build`

## 4. Icons and lang

- [x] 4.1 `tools/brewery-textures/gen.py`: `tipsy`, `merry`, `drunk`, `plastered`, `hangover` effect icons, 18x18 in `textures/mob_effect/` (D8); run it, check an 8x preview; effect names in `datagen/ServusBreweryLanguageProvider`; run `gradlew :brewery:runData`, commit `src/generated/resources`; verify `gradlew build`

## 5. GameTests

- [x] 5.1 `gametest/RauschGameTests` (D9), listed in `BreweryGameTests`; verify `gradlew :brewery:runGameTestServer` and `gradlew build test`

## 6. Play check

- [ ] 6.1 Play check by the user:
  - Drink one beer: the Tipsy icon with a timer; luck and two armor points. Two beers: Merry. Four: Drunk, the screen wobbles, walking is slower, only the Drunk icon shows. Five: Plastered, now and then a sidestep.
  - Jump down a few blocks while Drunk (half damage) and while Plastered (none).
  - Let a zombie hit you sober and Drunk: Drunk flies farther.
  - Drink milk while Drunk: the Drunk icon comes back within a second.
  - Wait without drinking: the level falls step by step until sober.
  - Set `screenWobble = false` in `config/servus_brewery-client.toml` while Drunk: the wobble stops, the Drunk icon, armor and slowness stay; a pufferfish still wobbles.
  - Get Drunk and sleep: wake up with the Hangover icon, mining is slower, hunger drops faster. Stay up instead: the Hangover comes at dawn. Only Merry: no Hangover.
  - Drink any beer while hungover: the Hangover ends at once, you are Tipsy.
  - Leave and rejoin while Merry: still Merry. Die while Drunk: sober after respawn.
  - The five icons read as a glass, a smiling glass, a tilted glass, a glass lying in a puddle and a cloud.
