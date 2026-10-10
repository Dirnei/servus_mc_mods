# Spec Delta

## Purpose

Salt leaves the Calciner as Ash, the way alchemists found it in the fire, and the Purifier, a small build of a leaching vat over a catch basin, washes the Ash with water into the liquid Salt that the Coagulator and the Athanor use, leaving Caput Mortuum behind.

## ADDED Requirements

### Requirement: Ash
Manufacturing SHALL add the item Ash, which the Calciner's Ash Pan gives. It SHALL have no crafting recipe of its own.

#### Scenario: Ash from the Calciner
- **WHEN** a Calciner has made Salt and a player empties its Ash Pan
- **THEN** the player holds Ash

### Requirement: Caput Mortuum
Manufacturing SHALL add the item Caput Mortuum, the residue the Purifier leaves after washing Ash. It SHALL have no use in this change; a later stage turns it into carbon and diamond.

#### Scenario: Residue
- **WHEN** a Purifier finishes washing 4 Ash
- **THEN** one Caput Mortuum lies in its Vat

### Requirement: Purification recipes are data
A purification recipe SHALL be a datapack recipe of type `servus_manufacturing:purification` with one counted item ingredient, the mB of water it needs, the mB of Salt it makes, an optional residue item with count and a time in ticks. The Salt one item makes in the first recipe that takes Ash SHALL also be the Salt the Ash Pan collects per Ash.

#### Scenario: Shipped recipe
- **WHEN** the shipped data holds a recipe of 4 Ash and 400 mB water for 400 mB Salt and one Caput Mortuum in 160 ticks
- **THEN** a formed Purifier with 4 Ash and 400 mB water makes 400 mB Salt and one Caput Mortuum in 8 seconds

### Requirement: Purifier layout
The Purifier structure SHALL be one block deep, 3 wide and 2 tall, seen from its front: the Leaching Vat (the Purifier block) in the middle of the top row with planks of any wood left and right of it, and below it the Catch Basin with logs of any wood left and right. The build SHALL read the same from front and back.

#### Scenario: Layout from the front
- **WHEN** a player looks at a complete Purifier structure
- **THEN** they see, top to bottom: `P V P`, `L B L` (V Leaching Vat, B Catch Basin, P planks, L logs)

### Requirement: Forming the Purifier
Right clicking the Leaching Vat with Paracelsus' Journal SHALL check the structure along both horizontal axes and form it along the first that matches, making the player its owner, and SHALL NOT open the book; when nothing matches it SHALL name the first missing block. Breaking the Vat, the Basin, a plank or a log of a formed Purifier SHALL unform it. Only a formed Purifier SHALL work.

#### Scenario: Formed
- **WHEN** a player right clicks the Vat of a complete Purifier with the Journal
- **THEN** the Purifier forms and the player sees "The Purifier is ready."

#### Scenario: Log broken
- **WHEN** a player breaks a log of a formed Purifier
- **THEN** it unforms and stops

### Requirement: The Purifier leaches
The Leaching Vat SHALL hold one item slot for the ingredient, a water tank, a Salt tank and a slot for the residue (tank capacities from machine stats, example 4,000 mB). It SHALL start a cycle only when formed, when it holds the recipe's items and water, the Salt tank has room for the result and the residue slot has room, and SHALL take the items and the water and add the Salt and the residue when the cycle completes. It SHALL need no power and no fuel.

#### Scenario: Leaching
- **WHEN** a formed Purifier holds 8 Ash and 1,000 mB water
- **THEN** after two cycles it holds 800 mB Salt, 200 mB water, two Caput Mortuum and no Ash

#### Scenario: No water
- **WHEN** a formed Purifier holds Ash but no water
- **THEN** it makes nothing

### Requirement: Purifier ports
A formed Purifier SHALL take its ingredient and give its residue through the item capability on every face of the Vat, and accept only water through the fluid capability on every face of the Vat. It SHALL give only Salt through the fluid capability on every face of the Catch Basin and push its Salt into touching fluid handlers outside the build. The planks and logs SHALL expose nothing.

#### Scenario: Pump and tank
- **WHEN** a Fluid Pipe brings water from a pump to the Vat and another pipe leads from the Basin to a Fluid Tank
- **THEN** the water flows into the Vat and the Salt flows out of the Basin to the tank

#### Scenario: Salt refused
- **WHEN** a pipe offers Salt to the Vat
- **THEN** the Vat accepts none

### Requirement: The Purifier by hand
On a formed Purifier, using Ash on the Vat SHALL put it into the item slot; a water bucket on the Vat SHALL pour its water in; an empty bucket on the Basin SHALL fill with Salt when the Salt tank holds enough; an empty hand on the Vat SHALL give the residue; sneaking with an empty hand on the Vat SHALL give the ingredient back. The Purifier SHALL have no GUI.

#### Scenario: Water by bucket
- **WHEN** a player uses a water bucket on the Vat of a formed Purifier with an empty water tank
- **THEN** the water tank holds 1,000 mB and the player holds an empty bucket

#### Scenario: Salt by bucket
- **WHEN** a player uses an empty bucket on the Basin of a formed Purifier holding 1,000 mB Salt
- **THEN** the player holds a Salt bucket and the Salt tank is empty

#### Scenario: Residue by hand
- **WHEN** a player uses the Vat with an empty hand and it holds two Caput Mortuum
- **THEN** the player receives the two Caput Mortuum

### Requirement: The Purifier shows its work
The Vat SHALL show the Ash still to wash as a layer on its floor and the water above it; the Basin SHALL show the Salt at the height of what the Salt tank holds. With Jade installed, looking at the Vat or the Basin SHALL show the items, the water, the Salt and the progress.

#### Scenario: Salt level
- **WHEN** a formed Purifier holds 2,000 mB Salt
- **THEN** the Basin shows Salt at half its height

### Requirement: Purifier schematic
The journal's Purifier entry SHALL show the structure as a multiblock page with Visualize, standing on the clicked ground, and the crafting recipes of the Vat and the Basin.

#### Scenario: Visualize
- **WHEN** a player presses Visualize on the Purifier page and clicks the ground
- **THEN** the ghost of the Purifier stands on that ground

### Requirement: Purifier reports to the journal
The Purifier SHALL remember the player who formed it as its owner and SHALL report every finished cycle to the journal of the owner's team as a `purify` event with the Salt it made.

#### Scenario: First purification
- **WHEN** a player's Purifier finishes its first cycle while Villach is open
- **THEN** the Villach task "purify Ash" is done

### Requirement: Purifier crafting
The Leaching Vat and the Catch Basin SHALL be crafted at a crafting table from vanilla materials without research.

#### Scenario: Recipe
- **WHEN** the player crafts the Vat pattern of planks, a cauldron and a hopper
- **THEN** one Purifier (Leaching Vat) is produced
