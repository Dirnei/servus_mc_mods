## Purpose

Defines malting in Servus: Brewery: soaking barley and wheat, the Malting Floor on which soaked grain germinates into green malt, the malt and grist items the Brewhouse needs, and the milling recipes that turn malt into grist.

## ADDED Requirements

### Requirement: Soaking grain
Eight Barley around a Water Bucket in a crafting grid SHALL make eight Soaked Barley, and eight Wheat around a Water Bucket SHALL make eight Soaked Wheat. The Water Bucket SHALL come back as an empty Bucket.

#### Scenario: Soaking barley
- **WHEN** a player crafts eight Barley around a Water Bucket
- **THEN** they get eight Soaked Barley and an empty Bucket stays in the grid

### Requirement: Malting Floor block
The Malting Floor SHALL be a low wooden floor block, 3 px tall, that shows whether it is empty, holds soaked grain or holds green malt. It SHALL hold one stack of one kind of grain.

#### Scenario: Floor shows its grain
- **WHEN** soaked grain lies on a Malting Floor and later becomes green malt
- **THEN** the floor shows the soaked grain first and the green malt afterwards

### Requirement: Spreading soaked grain
Right click on a Malting Floor with grain that a germinating recipe takes SHALL put as much of the held stack on the floor as fits, when the floor is empty or holds the same soaked grain. Every time soaked grain is added, the batch SHALL start germinating from the beginning. Any other grain SHALL not go onto an occupied floor.

#### Scenario: Spreading soaked barley
- **WHEN** a player right clicks an empty Malting Floor with sixteen Soaked Barley
- **THEN** the sixteen Soaked Barley lie on the floor and the player's stack is empty

#### Scenario: Other grain on an occupied floor
- **WHEN** a player right clicks a floor with Soaked Barley on it while holding Soaked Wheat
- **THEN** nothing is added

### Requirement: Germination
Soaked grain on a Malting Floor SHALL turn into the result of its germinating recipe, with the same count, once its germination time has passed. The time SHALL count game time, also while the chunk is not loaded. The time SHALL come from the recipe in datapack JSON (example: one in-game day, 24000 ticks).

#### Scenario: Barley becomes green malt
- **WHEN** Soaked Barley has lain on a Malting Floor for its germination time
- **THEN** the floor holds the same number of Green Malt

#### Scenario: Before the time
- **WHEN** Soaked Barley has lain on a floor for less than its germination time
- **THEN** it is still Soaked Barley

### Requirement: Turning the grain
Right click with an empty hand on a floor with soaked grain SHALL turn the grain: the rest of the germination time SHALL shrink by the recipe's turn bonus. A floor SHALL be turned at most once per the recipe's turn cooldown; a right click during the cooldown SHALL do nothing. The bonus and cooldown SHALL come from the recipe (example: 2400 ticks bonus, 1200 ticks cooldown).

#### Scenario: Turning speeds it up
- **WHEN** a player turns a floor of Soaked Barley
- **THEN** the batch finishes the bonus earlier than without turning

#### Scenario: Turning twice at once
- **WHEN** a player turns a floor and right clicks it again within the cooldown
- **THEN** the second click gives no bonus

### Requirement: Taking grain off the floor
Right click with an empty hand on a floor with green malt SHALL give the whole stack to the player. Sneaking right click with an empty hand SHALL give the player whatever lies on the floor. Breaking the floor SHALL drop the floor block and its grain.

#### Scenario: Collecting green malt
- **WHEN** a player right clicks a floor holding Green Malt with an empty hand
- **THEN** the player gets the Green Malt and the floor is empty

#### Scenario: Breaking a full floor
- **WHEN** a player breaks a floor holding Soaked Barley
- **THEN** the floor and the Soaked Barley drop

### Requirement: Malting Floor automation
The Malting Floor SHALL expose the standard item capability on every side. Inserting SHALL follow the rules for spreading grain by hand. Extracting SHALL only take finished green malt; soaked grain SHALL not be extracted.

#### Scenario: A pipe fills the floor
- **WHEN** a pipe inserts Soaked Barley into an empty Malting Floor
- **THEN** the grain lies on the floor and starts germinating

#### Scenario: A pipe cannot take soaked grain
- **WHEN** a pipe tries to extract from a floor with Soaked Barley
- **THEN** nothing is extracted

#### Scenario: A pipe takes green malt
- **WHEN** a pipe extracts from a floor with Green Malt
- **THEN** it gets the Green Malt

### Requirement: Malt and grist items
Brewery SHALL have the items Green Malt, Green Wheat Malt, Pale Malt, Dark Malt, Wheat Malt, Pale Malt Grist, Dark Malt Grist and Wheat Malt Grist. Each grist SHALL be its own item, so a recipe can ask for pale, dark or wheat grist. Pale, Dark and Wheat Malt SHALL only be made by the Malt Kiln of the Brewhouse; no other recipe of this mod SHALL make them.

#### Scenario: Grists are told apart
- **WHEN** a recipe asks for Dark Malt Grist
- **THEN** Pale Malt Grist does not match it

### Requirement: Milling malt to grist
Brewery SHALL ship `servus_foundation:milling` recipes as data: one Pale Malt to one Pale Malt Grist, one Dark Malt to one Dark Malt Grist, one Wheat Malt to one Wheat Malt Grist.

#### Scenario: The Mill grinds pale malt
- **WHEN** a driven Mill holds Pale Malt
- **THEN** it grinds it into Pale Malt Grist

#### Scenario: The Mill grinds dark malt
- **WHEN** a driven Mill holds Dark Malt
- **THEN** it grinds it into Dark Malt Grist
