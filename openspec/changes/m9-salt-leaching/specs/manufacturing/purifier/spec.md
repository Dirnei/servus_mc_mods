# Spec Delta

## Purpose

Salt leaves the Calciner as Ash, the way alchemists found it in the fire, and the Purifier leaches the Ash with water into the liquid Salt that the Coagulator and the Athanor use.

## ADDED Requirements

### Requirement: Ash
Manufacturing SHALL add the item Ash, which the Calciner's Ash Pan gives. It SHALL have no crafting recipe of its own.

#### Scenario: Ash from the Calciner
- **WHEN** a Calciner has made Salt and a player empties its Ash Pan
- **THEN** the player holds Ash

### Requirement: Purification recipes are data
A purification recipe SHALL be a datapack recipe of type `servus_manufacturing:purification` with one counted item ingredient, the mB of water it needs, the mB of Salt it makes and a time in ticks. The Salt one Ash makes in the first recipe that takes Ash SHALL also be the Salt the Ash Pan collects per Ash.

#### Scenario: Shipped recipe
- **WHEN** the shipped data holds a recipe of one Ash and 100 mB water for 100 mB Salt in 40 ticks
- **THEN** a Purifier with one Ash and 100 mB water makes 100 mB Salt in 2 seconds

### Requirement: The Purifier leaches
The Purifier SHALL hold one item slot for its ingredient, a water tank and a Salt tank (capacities from machine stats, example 4,000 mB each). It SHALL start a cycle only when it holds the recipe's item and water and the Salt tank has room for the result, and SHALL take the item and the water and add the Salt when the cycle completes. It SHALL need no power and no fuel.

#### Scenario: Leaching
- **WHEN** a Purifier holds 3 Ash and 1,000 mB water
- **THEN** after three cycles it holds 300 mB Salt, 700 mB water and no Ash

#### Scenario: No water
- **WHEN** a Purifier holds Ash but no water
- **THEN** it makes nothing

### Requirement: Purifier ports
The Purifier SHALL take its ingredient through the item capability on every face and give no items. Through the fluid capability on every face it SHALL accept only water, into its water tank, and give only Salt, from its Salt tank. It SHALL push its Salt into touching fluid handlers that accept it.

#### Scenario: Pump and tank
- **WHEN** a Fluid Pipe brings water from a pump to the Purifier and another pipe leads from it to a Fluid Tank
- **THEN** the water flows into the Purifier and the Salt flows out to the tank

#### Scenario: Salt refused
- **WHEN** a pipe offers Salt to the Purifier
- **THEN** the Purifier accepts none

### Requirement: The Purifier by hand
Using Ash on the Purifier SHALL put it into the item slot; using a water bucket SHALL pour its water into the water tank; using an empty bucket SHALL fill it with Salt when the Salt tank holds enough; using it with an empty hand while sneaking SHALL give the item slot back. The Purifier SHALL have no GUI.

#### Scenario: Water by bucket
- **WHEN** a player uses a water bucket on a Purifier with an empty water tank
- **THEN** the water tank holds 1,000 mB and the player holds an empty bucket

#### Scenario: Salt by bucket
- **WHEN** a player uses an empty bucket on a Purifier holding 1,000 mB Salt
- **THEN** the player holds a Salt bucket and the Salt tank is empty

### Requirement: The Purifier shows its work
The Purifier SHALL be a wooden vat whose open top shows the brine: the Salt when it holds any, else the water, at the height of what it holds. With Jade installed, looking at it SHALL show the item, the water, the Salt and the progress.

#### Scenario: Brine level
- **WHEN** a Purifier holds 2,000 mB water and no Salt
- **THEN** the vat shows water at half its height

### Requirement: Purifier reports to the journal
The Purifier SHALL remember the player who placed it as its owner and SHALL report every finished cycle to the journal of the owner's team as a `purify` event with the Salt it made.

#### Scenario: First purification
- **WHEN** a player's Purifier finishes its first cycle while Villach is open
- **THEN** the Villach task "purify Ash" is done

### Requirement: Purifier crafting
The Purifier SHALL be crafted at a crafting table from vanilla materials without research.

#### Scenario: Recipe
- **WHEN** the player crafts the Purifier pattern of planks, a cauldron and a hopper
- **THEN** one Purifier is produced
