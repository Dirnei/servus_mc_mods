## Why

`m15-beer` lets players drink Helles, Dunkles and Weissbier, but a beer only restores a little hunger; it posts a `BeerDrunkEvent` and nothing listens (PLAN.md, Open: M15 Brewery, `m15-rausch`: "the Rausch levels, the hangover and the Hair of the Dog; verify whether more knockback needs NeoForge's knockback event"). This change gives drinking its consequences: a growing Rausch with luck and armor, clumsiness at the top, a hangover the next morning and the Hair of the Dog.

## What Changes

- **Rausch counter**: a hidden number per player, kept with the player's data (survives relog, starts at 0 again after death). Each beer adds to it (example: 1 per drink), it falls by one point every so often (example: every 2400 ticks) and is capped (example: 8).
- **Levels**, each its own status effect with a visible icon and a timer that shows how long the level lasts: **Tipsy** (example from 1: Luck +1, Armor +2), **Merry** (from 2: Luck +2, Armor +4), **Drunk** (from 4: Luck +2, Armor +6, half fall damage, slower walking, screen wobble, knockback taken x1.5), **Plastered** (from 5: Luck +3, Armor +8, no fall damage, slower walking, screen wobble, stumbling sidesteps, knockback taken x2). Luck, armor, fall damage and walking speed are attribute modifiers of the effects; the knockback factor goes through NeoForge's `LivingKnockBackEvent`, since knockback resistance stops at 0. Milk does not sober a player up: the effects come back while the Rausch lasts.
- **Hangover**: a player who reached Drunk or worse wakes up with a Hangover the next morning (after sleeping, or when the day turns): slower mining and faster hunger for a while (example: 6000 ticks); the Rausch is gone.
- **Hair of the Dog**: drinking any beer while hungover ends the Hangover at once and sets the Rausch to Tipsy.
- **Accessibility**: a client config switch `screenWobble` (default on) in `config/servus_brewery-client.toml`; off removes the screen wobble and keeps every other effect.
- **Balance data** in `data/servus_brewery/rausch/stats.json`: per drink amount, decay, cap, every level's threshold, luck, armor, fall damage, walking speed, knockback, wobble and stumbling, the hangover's threshold, duration, mining speed, hunger and the Hair of the Dog level.
- Effect icons Tipsy, Merry, Drunk, Plastered and Hangover in `tools/brewery-textures/gen.py`, lang via datagen.
- Pure logic with JUnit tests: the counter, levels, decay, level duration, hangover and Hair of the Dog. GameTests: drinking raises the level and applies its effects, decay lowers it, knockback is larger when Drunk, the hangover arrives the next day, the Hair of the Dog cures it.

## Capabilities

### New Capabilities
- `brewery/rausch`: the Rausch counter, its levels and their effects, the hangover, the Hair of the Dog, the balance data and the screen wobble switch.

### Modified Capabilities
None. `brewery/beer` (delta of the unarchived `m15-beer`) already posts `BeerDrunkEvent` once per drink; this change only listens.

## Non-goals

- Effects for other drinks than the three beers, a Mass, Radler.
- Showing the Rausch number itself (it stays hidden; only the level effects show).
- Rausch for mobs or villagers.
- A sobering item other than the Hair of the Dog.

## Impact

- New in `brewery/`: `rausch/` (pure counter and stats, effects, attachment, tracker, stats loader, events), `client/RauschClient`, `config/ServusBreweryClientConfig`, `gametest/RauschGameTests`, JUnit tests, effect icons, `data/servus_brewery/rausch/stats.json`.
- Changed: `ServusBrewery` (registers the effects, the attachment and the client config), `BreweryGameTests`, the language provider, `tools/brewery-textures/gen.py`.
- No Foundation, Delicacies or `brewhouse/` changes.
