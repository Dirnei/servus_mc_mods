## Purpose

Defines the Rausch of Servus: Brewery: a hidden per player counter that beer raises and time lowers, its levels Tipsy, Merry, Drunk and Plastered with their effects, the Hangover the next morning, the Hair of the Dog, the balance data and the screen wobble switch.

## ADDED Requirements

### Requirement: Rausch counter
Every player SHALL have a hidden Rausch counter that starts at 0. Each beer a player finishes drinking SHALL raise it by the per drink amount (example: 1), up to a cap (example: 8). The counter SHALL fall by one point after each decay period the player spends in the game (example: 2400 ticks) and never below 0. It SHALL be saved with the player, so it survives leaving and joining the world, and SHALL start at 0 again after the player dies.

#### Scenario: A beer raises the Rausch
- **WHEN** a sober player drinks a Glass of Helles
- **THEN** their Rausch is 1

#### Scenario: The cap
- **WHEN** a player at the cap drinks another beer
- **THEN** their Rausch stays at the cap

#### Scenario: Decay
- **WHEN** a player with a Rausch of 3 spends one decay period in the game
- **THEN** their Rausch is 2

#### Scenario: Relog
- **WHEN** a player with a Rausch of 3 leaves the world and joins again
- **THEN** their Rausch is still 3

#### Scenario: Death
- **WHEN** a player with a Rausch of 4 dies and respawns
- **THEN** their Rausch is 0 and they have no Rausch effect

### Requirement: Rausch levels
A player's Rausch SHALL give the highest level whose threshold it reaches, each shown as its own status effect with an icon: Tipsy (example: from 1), Merry (from 2), Drunk (from 4) and Plastered (from 5). A sober player SHALL have none of them, and a player SHALL have at most one at a time. The effect's timer SHALL show how long the player stays at that level if they drink nothing more. Drinking milk SHALL NOT sober a player up: the level's effect SHALL come back within a second while the Rausch lasts.

#### Scenario: Tipsy after one beer
- **WHEN** a sober player drinks one beer
- **THEN** they have the Tipsy effect and no other Rausch effect

#### Scenario: Higher level replaces the lower
- **WHEN** a player with a Rausch of 3 drinks one beer
- **THEN** the Merry effect is replaced by the Drunk effect

#### Scenario: Decay lowers the level
- **WHEN** a Drunk player at a Rausch of 4 spends one decay period without drinking
- **THEN** they are Merry

#### Scenario: Sober again
- **WHEN** the Rausch of a Tipsy player falls to 0
- **THEN** they have no Rausch effect

#### Scenario: Milk
- **WHEN** a Drunk player drinks milk
- **THEN** they are Drunk again within a second

### Requirement: Level effects
Each level SHALL change the player's attributes while it lasts, with amounts from data (examples): Tipsy Luck +1 and Armor +2; Merry Luck +2 and Armor +4; Drunk Luck +2, Armor +6, half fall damage, walking speed -15%; Plastered Luck +3, Armor +8, no fall damage, walking speed -15%. The modifiers SHALL end with the level.

#### Scenario: Tipsy luck and armor
- **WHEN** a player without armor becomes Tipsy
- **THEN** their luck is 1 and their armor is 2

#### Scenario: Drunk fall damage
- **WHEN** a Drunk player falls from a height that deals 10 damage to a sober player
- **THEN** they take 5 damage

#### Scenario: Plastered fall damage
- **WHEN** a Plastered player falls from any height
- **THEN** they take no fall damage

#### Scenario: Modifiers end with the level
- **WHEN** a Tipsy player becomes sober
- **THEN** their luck and armor are back to what they were before

### Requirement: Knockback taken
A Drunk or Plastered player SHALL be knocked back farther by their level's knockback factor (examples: Drunk x1.5, Plastered x2), on top of whatever knockback resistance does. Levels without a factor SHALL leave knockback as it is.

#### Scenario: Drunk knockback
- **WHEN** a sober player and a Drunk player are knocked back by the same hit
- **THEN** the Drunk player is pushed 1.5 times as far

#### Scenario: Tipsy knockback
- **WHEN** a Tipsy player is knocked back
- **THEN** they are pushed as far as a sober player

### Requirement: Screen wobble and stumbling
Drunk and Plastered (by data) SHALL make the screen wobble as vanilla Nausea does, without an extra Nausea icon. A Plastered player SHALL now and then stumble: a small sidestep to the left or right while standing on the ground (examples: chance 0.25 per second, push strength 0.25).

#### Scenario: Wobble when Drunk
- **WHEN** a player becomes Drunk
- **THEN** their screen wobbles and the only effect icon is Drunk

#### Scenario: Pufferfish while Drunk
- **WHEN** a Drunk player is poisoned by a pufferfish
- **THEN** the pufferfish's Nausea lasts its own time, also after the player sobers up, and the Rausch's wobble comes back after it while they are still Drunk

#### Scenario: Stumbling
- **WHEN** a Plastered player stands on the ground for a while
- **THEN** they are pushed sideways now and then

#### Scenario: No stumbling when Drunk
- **WHEN** a Drunk player stands on the ground for a while
- **THEN** nothing pushes them

### Requirement: Screen wobble switch
Servus: Brewery SHALL register a client config `config/servus_brewery-client.toml` with a boolean option `screenWobble`, default true, that is read when used. With it false, the Rausch SHALL NOT make the screen wobble, and every other Rausch effect SHALL stay.

#### Scenario: Wobble switched off
- **WHEN** `screenWobble` is false and the player becomes Drunk
- **THEN** the screen does not wobble, and the player still has the Drunk effect, its armor and its knockback

#### Scenario: Wobble switched back on
- **WHEN** `screenWobble` is set back to true while the player is Drunk
- **THEN** the screen wobbles again without another drink

#### Scenario: Other nausea stays
- **WHEN** `screenWobble` is false and a pufferfish poisons the player
- **THEN** the pufferfish's Nausea still wobbles the screen

### Requirement: Hangover
A player whose Rausch reached the hangover threshold (example: 4, Drunk) SHALL get the Hangover effect the next morning: when they wake up from sleep or when the day turns, whichever comes first. The Hangover SHALL set the Rausch to 0, last a while (example: 6000 ticks), slow mining (example: -30%) and make the player hungry faster (example: 0.1 exhaustion per second). Like the levels, milk SHALL NOT end it.

#### Scenario: Waking up hungover
- **WHEN** a Drunk player sleeps through the night
- **THEN** they wake up with the Hangover, their Rausch is 0, and they mine slower

#### Scenario: No hangover when only Merry
- **WHEN** a player who was never more than Merry sleeps through the night
- **THEN** they wake up without the Hangover

#### Scenario: Hangover passes
- **WHEN** the Hangover's time runs out
- **THEN** the player mines at normal speed again

### Requirement: Hair of the Dog
A player who drinks any beer while hungover SHALL lose the Hangover at once, and their Rausch SHALL be set to the Hair of the Dog value (example: 1, Tipsy) instead of rising by the drink.

#### Scenario: Hair of the Dog
- **WHEN** a hungover player drinks a Glass of Weissbier
- **THEN** the Hangover is gone and they are Tipsy

### Requirement: Rausch balance data
The per drink amount, the decay period, the cap, every level's threshold and effect amounts (luck, armor, fall damage factor, walking speed, knockback factor, screen wobble, stumbling chance and strength), the hangover threshold, duration, mining speed and hunger, and the Hair of the Dog value SHALL be read from `data/<namespace>/rausch/stats.json` (file `servus_brewery:stats`); every field SHALL be optional and fall back to the shipped values.

#### Scenario: A pack makes beer stronger
- **WHEN** a datapack sets the per drink amount to 2
- **THEN** one beer makes a sober player Merry
