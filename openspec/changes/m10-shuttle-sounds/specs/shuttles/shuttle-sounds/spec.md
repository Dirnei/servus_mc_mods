# Spec Delta

## Purpose

The sound of shuttles at work: a quiet drive loop that lets players hear a fleet moving without it becoming noise, and the Muffler that damps it for a shuttle or a whole garage.

## ADDED Requirements

### Requirement: Moving shuttles sound
A shuttle that drives along a rail or rides a lift car SHALL play a looping drive sound at its position that moves with it: a low rumble with sparse, dull wheel clicks and a loose rattle now and then. The sound SHALL be quiet next to a vanilla minecart and SHALL fade out within a short distance (example 10 blocks). A shuttle that stands at a stop, waits, is parked or is inside its garage SHALL be silent.

#### Scenario: A shuttle drives past
- **WHEN** a shuttle drives along a rail three blocks from the player
- **THEN** the player hears the drive sound coming from the shuttle and following it

#### Scenario: Stopped at a rack
- **WHEN** the shuttle stops at a rack to load
- **THEN** its drive sound fades out until it drives on

#### Scenario: Far away
- **WHEN** a shuttle drives 20 blocks from the player
- **THEN** the player does not hear it

### Requirement: Only the nearest shuttles sound
At most a few moving shuttles (example 4), the ones nearest the player, SHALL play their drive sound at once. When a nearer shuttle starts moving, the farthest sounding one SHALL fall silent.

#### Scenario: A busy warehouse
- **WHEN** ten shuttles drive within ten blocks of the player
- **THEN** only the four nearest of them play the drive sound

### Requirement: Shuttles do not sound alike
Each sounding shuttle SHALL play one of several takes of the drive loop, picked at random, at a slightly different pitch from the others, so shuttles side by side are not in step. A faster shuttle SHALL play it at a higher pitch than a slower one.

#### Scenario: Two shuttles side by side
- **WHEN** two shuttles with the same Speed count drive next to each other
- **THEN** their drive sounds are not in step

#### Scenario: Speed upgrades
- **WHEN** a shuttle with two Speed upgrades and one without any drive past the player
- **THEN** the faster shuttle's drive sound is higher

### Requirement: Muffled shuttles
A shuttle SHALL be muffled while it has a Muffler installed or its garage holds a Muffler. A muffled shuttle SHALL play a damped version of the drive sound, as if heard through a wall, much quieter (example a third of the volume) but not silent. Both Mufflers together SHALL damp no further than one. A muffled Lumber shuttle SHALL fell in silence: no chainsaw, hold or crane sounds.

#### Scenario: Muffler on the shuttle
- **WHEN** a shuttle with a Muffler installed drives past the player
- **THEN** the player hears the damped, quieter drive sound

#### Scenario: Muffler in the garage
- **WHEN** a garage holds a Muffler and its three shuttles have none
- **THEN** all three play the damped drive sound

#### Scenario: Garage Muffler taken out
- **WHEN** the player takes the Muffler out of the garage while its shuttles drive
- **THEN** shuttles without their own Muffler play the normal drive sound again

#### Scenario: Muffled Lumber shuttle
- **WHEN** a Lumber shuttle whose garage holds a Muffler fells a tree near the player
- **THEN** the player hears no chainsaw, hold or crane, only the damped drive sound

### Requirement: Muffler item
The Muffler SHALL be a craftable Shuttles item from common materials (example wool around an iron ingot). It SHALL do nothing on its own; it works installed in a shuttle or placed in a garage's Muffler slot. Its icon SHALL be the copper upgrade card of the other Shuttles upgrades with a muted speaker glyph.

#### Scenario: Craft a Muffler
- **WHEN** a player looks up the Muffler recipe
- **THEN** it is a crafting recipe from common materials
