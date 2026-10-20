## Context

See proposal.md for the why. Decided in the explore of 2026-10-20 (`temp/delicacies-brief.md`): a hidden per player counter, +1 per drink, decaying over time; levels 1 Tipsy (Luck +1, Armor +2), 2-3 Merry (Luck +2, Armor +4), 4 Drunk (Luck +2, Armor +6, half fall damage, Nausea, Slowness, knockback taken x1.5), 5+ Plastered (Luck +3, Armor +8, no fall damage, stumbling, knockback taken x2); a Hangover the next morning after Drunk or worse with Mining Fatigue and more hunger, cured at once by the Hair of the Dog (any beer), which sets the Rausch back to 1; a config switch for the screen wobble; numbers as datapack examples.

`m15-beer` (applied, not archived) left `beer/BeerItem`, which posts `beer/BeerDrunkEvent` (a `PlayerEvent` with `getDrink()`) on `NeoForge.EVENT_BUS` once per drink on the server. Another change (`m14-heating-element`) edits `brewhouse/` and Brewery's shared event class at the same time, so this change keeps its listeners in its own class and leaves `brewhouse/` alone. Sources checked: `minecraft-patched-26.1.2.116-sources.jar`, `neoforge-26.1.2.116-sources.jar`.

## Goals / Non-Goals

**Goals:** a pure, JUnit tested Rausch model; five own status effects with icons; every number in data; knockback through the NeoForge event; a hangover keyed to the morning; the Hair of the Dog; the wobble switch; GameTests for drinking, decay, knockback, the hangover and its cure.

**Non-Goals:** see proposal.md, Non-goals.

## Decisions

### D1 Pure model (`rausch/Rausch`, `RauschStats`, `RauschLevel`, `LevelStats`, `HangoverStats`)
No Minecraft imports. `RauschLevel` is the enum `TIPSY`, `MERRY`, `DRUNK`, `PLASTERED` (ascending, with a lower case `key()` for data). `LevelStats(int from, double luck, double armor, double fallDamage, double speed, double knockback, boolean wobble, double stumbleChance, double stumbleStrength)`. `HangoverStats(int from, int duration, double miningSpeed, double exhaustion, int hairOfTheDog)`. `RauschStats(int perDrink, int decayTicks, int max, Map<RauschLevel, LevelStats> levels, HangoverStats hangover)` with `DEFAULTS` and `level(int points)`: the highest level, in enum order, whose `from` the points reach; empty at 0 points.

`Rausch(int points, int progress, long dueDay, long hangoverUntil)` is an immutable record, `SOBER = (0, 0, -1, 0)`:
- `drink(amount, stats, day, gameTime)`: hungover at `gameTime` means Hair of the Dog: `(hairOfTheDog, 0, -1, 0)`; otherwise points rise to at most `max`, and reaching `hangover.from` sets `dueDay` to `day` if it is not set yet.
- `pass(ticks, stats)`: `progress` counts up; each full `decayTicks` takes one point; at 0 points `progress` is 0.
- `morning(day, gameTime, stats)`: with `dueDay >= 0` and `day > dueDay` the Hangover starts: `(0, 0, -1, gameTime + duration)`.
- `level(stats)`, `ticksAtLevel(stats)` (ticks until the points fall below the current level's `from`: `(points - from) * decayTicks + decayTicks - progress`, 0 when sober), `hungover(gameTime)`, `hangoverLeft(gameTime)`.

JUnit `RauschTest`: drinking adds and caps, decay takes one point per period and keeps the rest, never below 0, levels at each threshold (0 none, 1 Tipsy, 2 and 3 Merry, 4 Drunk, 5 and 8 Plastered), ticks at level, due day set at Drunk and not at Merry, morning on the same day does nothing, morning on the next day starts the Hangover and clears the points, Hair of the Dog ends the Hangover and sets 1, drinking after the Hangover ran out adds normally. `RauschStatsTest`: unordered thresholds still give the highest reached level; a level missing from data falls back to the default.

### D2 Data (`rausch/RauschStatsManager`)
`SimpleJsonResourceReloadListener` over `FileToIdConverter.json("rausch")`, file `servus_brewery:stats`, shape of `CellarStatsManager`; registered with `AddServerReloadListenersEvent` in `rausch/RauschEvents`. Every field is `optionalFieldOf` with the default's value, so a pack can set one number. Shipped `data/servus_brewery/rausch/stats.json`:

```json
{
  "per_drink": 1, "decay_ticks": 2400, "max": 8,
  "levels": {
    "tipsy":     {"from": 1, "luck": 1, "armor": 2},
    "merry":     {"from": 2, "luck": 2, "armor": 4},
    "drunk":     {"from": 4, "luck": 2, "armor": 6, "fall_damage": 0.5, "speed": -0.15, "knockback": 1.5, "wobble": true},
    "plastered": {"from": 5, "luck": 3, "armor": 8, "fall_damage": 0.0, "speed": -0.15, "knockback": 2.0, "wobble": true,
                  "stumble_chance": 0.25, "stumble_strength": 0.25}
  },
  "hangover": {"from": 4, "duration": 6000, "mining_speed": -0.3, "exhaustion": 0.1, "hair_of_the_dog": 1}
}
```

Level defaults for absent fields: luck 0, armor 0, fall damage 1, speed 0, knockback 1, wobble false, stumbling 0. The stats are server data; the client never needs them (the effects and their modifiers sync as usual).

### D3 Effects (`rausch/RauschEffects`, `RauschEffect`, `HangoverEffect`)
`DeferredRegister<MobEffect>` (`Registries.MOB_EFFECT`, as Alchemy's `ElementalSight`): `tipsy`, `merry` (`BENEFICIAL`), `drunk`, `plastered` (`NEUTRAL`) as `RauschEffect(RauschLevel)`, and `hangover` (`HARMFUL`) as `HangoverEffect`.

Attribute modifiers use NeoForge's `MobEffect#addAttributeModifier(Holder<Attribute>, Identifier, Operation, Int2DoubleFunction curve)` (verified in the patched `MobEffect`: `AttributeTemplate#create(amplifier)` calls the curve when the effect is added, so the curve reads the loaded data then; the amplifier is ignored, every level is its own effect at amplifier 0). Per level:
- `Attributes.LUCK`, `ADD_VALUE`, `luck`;
- `Attributes.ARMOR`, `ADD_VALUE`, `armor`;
- `Attributes.FALL_DAMAGE_MULTIPLIER`, `ADD_MULTIPLIED_TOTAL`, `fallDamage - 1` (verified: `LivingEntity#calculateFallDamage` multiplies by this attribute, range 0 to 100, so 0.5 halves and 0 removes fall damage; no fall event needed);
- `Attributes.MOVEMENT_SPEED`, `ADD_MULTIPLIED_TOTAL`, `speed` (the brief's Slowness without a second icon).

Hangover: `Attributes.BLOCK_BREAK_SPEED`, `ADD_MULTIPLIED_TOTAL`, `miningSpeed` (the brief's Mining Fatigue without a second icon); `shouldApplyEffectTickThisTick` every 20 ticks and `applyEffectTick` calls `Player#causeFoodExhaustion(exhaustion)` (as vanilla `HungerMobEffect`).

Modifier ids `servus_brewery:effect.<name>.<attribute>`.

### D4 Player data (`rausch/RauschAttachment`)
`DeferredRegister<AttachmentType<?>>` (`NeoForgeRegistries.Keys.ATTACHMENT_TYPES`), `rausch`: `AttachmentType.builder(() -> Rausch.SOBER).serialize(MAP_CODEC)` (`serialize(MapCodec)` verified in 26.1 `AttachmentType.Builder`). Not `copyOnDeath`, so a respawned player starts sober (decided). Fields `points`, `progress`, `due_day`, `hangover_until`.

### D5 Tracker (`rausch/RauschTracker`, server only)
- `drink(Player, ItemStack)`: from `BeerDrunkEvent` in `RauschEvents`; the amount is `per_drink`; `Rausch#drink` with the day and game time, then `sync`.
- `advance(Player, int ticks, long day)`: `pass`, then `morning`, then `sync`, then the stumble roll. `RauschEvents` calls it from `PlayerTickEvent.Post` (verified, `event/tick/PlayerTickEvent`) on the server every 20 ticks with `ticks = 20` and `day = level.getOverworldClockTime() / 24000`. 26.1 replaced the day time with world clocks: `Level#getOverworldClockTime()` is the overworld clock's total ticks, and sleeping moves it to the `wake_up_from_sleep` marker at tick 0 of the 24000 period (verified in `Timelines` and `ServerLevel`), so "the day turned" covers both waking up and dawn. GameTests call `advance` with their own ticks and day.
- `sync(Player)`: the wanted level effect gets `new MobEffectInstance(effect, ticksAtLevel + 20, 0, false, false, true)` (no particles, icon shown). Other Rausch level effects are removed. The current one is replaced when missing or when its remaining duration is more than 40 ticks off (after a drink); so milk (`removeAllEffects`) is undone on the next update. Wobble: vanilla `MobEffects.NAUSEA` as `new MobEffectInstance(NAUSEA, same duration, 0, true, false, false)` (ambient, no particles, no icon), removed when the level has no wobble, but only if the present Nausea is such an instance. Hangover: while `hungover`, the Hangover effect with `hangoverLeft` (replaced when missing); removed when not hungover.
- Stumble: on the ground, not sneaking, not riding, with chance `stumbleChance` per update: `Entity#push` of `stumbleStrength` perpendicular to the look direction, left or right at random, and `hurtMarked = true` so the server sends the motion to the client (as vanilla knockback on players).

### D6 Knockback (`RauschEvents`)
`LivingKnockBackEvent` (verified, `event/entity/living/LivingKnockBackEvent`, posted by `LivingEntity#knockback` through `CommonHooks.onLivingKnockBack` before knockback resistance is applied): for a player with a Rausch level effect, `setStrength(getStrength() * knockback)` of that level. Knockback resistance clamps at 0 and cannot express "more than normal", so the event is the right place.

### D7 Screen wobble switch (`config/ServusBreweryClientConfig`, `client/RauschClient`)
`ModConfigSpec` like `ServusFoundationConfig`, registered as `ModConfig.Type.CLIENT` in the mod constructor, file `servus_brewery-client.toml`, section `rausch`, `screenWobble` default true, read when used. `RauschClient` (`Dist.CLIENT`) on `ClientTickEvent.Post`: with `screenWobble` false, a Nausea on the local player that is ambient and shows no icon (only the Rausch's, D5) is removed client side with `removeEffectNoUpdate`. The wobble (`GameRenderer` and `Gui` read `getEffectBlendFactor(NAUSEA)` of the local player, verified) stops; the server keeps everything else. A pufferfish's Nausea shows its icon and stays.

### D8 Icons (`tools/brewery-textures/gen.py`)
Mob effect sprites are 18x18 in `textures/mob_effect/` (the gui atlas maps `mob_effect/` to that folder, verified in vanilla `atlases/gui.json`). New sprites drawn on the 16x16 canvas with the family helpers and padded to 18x18: `tipsy` a small half full glass, `merry` a full glass with a smiling face in the foam, `drunk` a tilted glass with foam spilling, `plastered` a glass lying on its side in a puddle, `hangover` a grey storm cloud with a raindrop. Lang: "Tipsy", "Merry", "Drunk", "Plastered", "Hangover".

### D9 GameTests (`gametest/RauschGameTests`, listed in `BreweryGameTests`)
With `makeMockPlayer` and `finishUsingItem` (as `BeerGameTests`):
- `oneBeerMakesTipsy` (Tipsy effect, luck 1, armor 2), `fourBeersMakeDrunk` (Drunk only, wobble Nausea present without icon, fall damage multiplier 0.5), `fiveBeersMakePlastered` (fall damage multiplier 0), `decayLowersTheLevel` (Drunk, `advance` one decay period: Merry; long enough: sober, modifiers gone), `milkDoesNotSoberUp`, `drunkKnockbackIsLarger` (two players, same `knockback`, horizontal speed ratio 1.5), `hangoverNextDay` (Drunk, `advance` with the next day: Hangover, Rausch 0, block break speed lowered), `noHangoverWhenMerry`, `hairOfTheDogCuresHangover`. The reset on death and relog are left to the play check (a mock player cannot respawn or log out).

## Settles from PLAN.md
PLAN.md asks to verify whether more knockback needs NeoForge's knockback event: it does (D6). PLAN.md is not edited (the run's instructions); for the user to carry over: the fall damage uses the vanilla `fall_damage_multiplier` attribute, not an event; the Rausch is reset on death; a client config file now exists for Brewery.

## Assumptions

- Decay runs only while the player is in the game (like hunger); time spent logged out does not sober.
- The Rausch resets on death.
- Every beer counts the same by default (`per_drink`); a per drink amount by item is not added.
- Plastered also wobbles and walks slower, like Drunk (data).
- Slowness and Mining Fatigue are attribute modifiers of the Rausch and Hangover effects, not the vanilla effects, so only the five own icons show.
- The Hangover clears the remaining Rausch; drinking through the night still brings the Hangover at dawn.
- The Hair of the Dog sets the Rausch to 1 and does not make a new Hangover due.
- Milk does not end the levels or the Hangover; they come back on the next update.
- Stumbling: 25% chance per second of a 0.25 sideways push while on the ground (data).
- The screen wobble is the vanilla Nausea look; the switch is per client.

## Risks / Trade-offs

- [Client side removal of Nausea] The client removes the Rausch's Nausea instance only on its own player; the server still has it, which only affects other mods that read the server's Nausea. Instances are told apart by ambient plus no icon; a mod that adds an icon-less ambient Nausea would be hidden too while the switch is off.
- [Data reload during an effect] Modifier amounts are read when an effect is added; a reload changes them on the next level change.
- [Mock players] GameTests drive `advance` directly; the 20 tick schedule is covered by the play check.

## Migration Plan

None. Nothing is released.
