# alchemy/coagulator Specification

## Purpose
The Coagulator joins the three principles again: from a fixed ratio of Salt, Sulfur and Mercury, sometimes with an item, it makes the materials of alchemy and everyday goods.

## Requirements

### Requirement: Coagulation recipes are data
A coagulation recipe SHALL be a datapack recipe of type `servus_alchemy:coagulation` with the mB of Salt, Sulfur and Mercury it needs (each may be 0), at most one item ingredient with a count, one result item with a count and a time in ticks.

#### Scenario: Salt Crystal
- **WHEN** the shipped data holds a recipe of 250 mB Salt for one Salt Crystal in 40 ticks
- **THEN** a Coagulator with that recipe selected and 250 mB Salt makes one Salt Crystal in 2 seconds

### Requirement: The player picks the recipe
The Coagulator SHALL run exactly one selected coagulation recipe. Its GUI SHALL list every coagulation recipe with its result and needs, and clicking one SHALL select it, unless the recipe names a journal chapter the owner's team has not finished. The selection SHALL persist across reloads and while the structure is unformed and formed again. Without a selection the Coagulator SHALL do nothing.

#### Scenario: Switch recipe
- **WHEN** the player clicks the Gunpowder recipe in a Coagulator that was making Salt Crystals
- **THEN** the Coagulator makes Gunpowder from then on and the selection survives a world reload

#### Scenario: Locked by the journal
- **WHEN** the player clicks a recipe that needs a chapter their team has not finished
- **THEN** the selection stays as it was

### Requirement: Coagulator works on time alone
The Coagulator SHALL need no power: no FE buffer, no fuel and no energy capability. The joined principles simply take the recipe's time to cool and set. It SHALL work only while its structure is formed. A cycle SHALL start only when its tanks hold the recipe's principles, its ingredient slot holds the recipe's item, and its output slot has room; the principles and the item SHALL be taken when the cycle completes.

#### Scenario: Missing Mercury
- **WHEN** the selected recipe needs 50 mB Mercury and the Mercury tank holds 20 mB
- **THEN** the Coagulator waits and keeps no progress

#### Scenario: No power needed
- **WHEN** a formed Coagulator holds 250 mB Salt with the Salt Crystal recipe selected and nothing touches it
- **THEN** it makes one Salt Crystal after the recipe's time

#### Scenario: Unformed does nothing
- **WHEN** an unformed Coagulator holds 250 mB Salt with the Salt Crystal recipe selected
- **THEN** it makes nothing

### Requirement: Coagulator GUI
The Coagulator GUI SHALL show the ingredient and output slots, progress, the three principle gauges and the recipe select button; it has no side panel. It SHALL open with an empty hand on a formed Coagulator. The recipe popup SHALL mark the selected recipe, SHALL show a scrollbar whenever it holds more recipes than fit, and SHALL close on Escape or a click outside it.

#### Scenario: Selected recipe marked
- **WHEN** the player opens the recipe popup of a Coagulator making Salt Crystals
- **THEN** the Salt Crystal entry in the popup is highlighted

#### Scenario: More recipes than fit
- **WHEN** the popup holds more recipes than its visible rows
- **THEN** a scrollbar shows beside the grid and the mouse wheel or dragging the bar scrolls it

#### Scenario: Close without choosing
- **WHEN** the player presses Escape or clicks outside the open popup
- **THEN** the popup closes, the GUI stays open and the selection is unchanged

### Requirement: Coagulator crafting
The Coagulator SHALL be crafted at a crafting table from vanilla materials without research.

#### Scenario: Recipe
- **WHEN** the player crafts the Coagulator pattern of iron ingots, glass, a cauldron and redstone
- **THEN** one Coagulator is produced

### Requirement: First alchemical materials
Alchemy SHALL add the items Salt Crystal, Sulfur and Quicksilver, made only in the Coagulator, as the first materials later alchemy builds on.

#### Scenario: Sulfur
- **WHEN** a Coagulator runs the shipped Sulfur recipe
- **THEN** it makes the Sulfur item from Sulfur principle

### Requirement: Buckets pour in
A filled principle bucket or container SHALL pour into the matching tank when the player uses it on the Vat of that principle, and when the player clicks any principle gauge in the GUI with it on the cursor. The player SHALL get the empty container back.

#### Scenario: Salt bucket on the block
- **WHEN** a player uses a Salt bucket on the Salt Vat of a formed Coagulator with room in its Salt tank
- **THEN** the Salt tank gains 1,000 mB and the player holds an empty bucket

### Requirement: Coagulator reports to the journal
The Coagulator SHALL remember the player who formed it as its owner and SHALL report every finished cycle to the journal of the owner's team as a `coagulate` event for its result with the result's count.

#### Scenario: Salt Crystal counts
- **WHEN** a Coagulator a player formed makes a Salt Crystal
- **THEN** the player's team gets a `coagulate` event for one Salt Crystal

### Requirement: The player can stop the Coagulator
The Coagulator's recipe popup SHALL offer a "No recipe" entry before the recipes. Picking it SHALL clear the selection, so the Coagulator does nothing until a recipe is picked again. While nothing is selected the popup SHALL mark the "No recipe" entry and the select button SHALL say so. Items already in the output slot stay there.

#### Scenario: Stop a running Coagulator
- **WHEN** a Coagulator makes Salt Crystals and the player picks "No recipe"
- **THEN** it stops making Salt Crystals, its principles stay in the tanks, and the select button reads "No recipe"

#### Scenario: Nothing selected is marked
- **WHEN** the player opens the popup of a Coagulator without a recipe
- **THEN** the "No recipe" entry is highlighted

### Requirement: Nigredo goods
Alchemy SHALL ship coagulation recipes that make the plain goods of Nigredo from the principles alone, without an item and without a chapter: Clay Ball (example 100 mB Salt and 50 mB Mercury) and Bone Meal (example 60 mB Salt and 20 mB Sulfur). The Coagulator's Workshop entry SHALL list them.

#### Scenario: Clay
- **WHEN** a formed Coagulator with Clay Ball selected holds the principles of its recipe
- **THEN** it makes one Clay Ball and the principles are used up

#### Scenario: Bone Meal
- **WHEN** a formed Coagulator with Bone Meal selected holds the principles of its recipe
- **THEN** it makes one Bone Meal
