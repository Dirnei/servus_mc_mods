## Purpose

Defines the vegetables of Servus: Delicacies: cabbage, red cabbage, onion, parsley, horseradish and mustard. How they are planted, grow, drop, are first found, tagged for other mods and composted.

## ADDED Requirements

### Requirement: Delicacies crops
Servus: Delicacies SHALL add six crops that behave like vanilla age crops: Cabbage, Red Cabbage, Onion, Parsley, Horseradish and Mustard. Each crop SHALL be planted only on farmland, SHALL grow through eight ages by random ticks when lit like vanilla wheat, SHALL show four growth looks, and SHALL break when its farmland is gone.

#### Scenario: Planting on farmland
- **WHEN** a player uses Cabbage Seeds on farmland with air above it
- **THEN** a cabbage crop at its first age is placed on the farmland and one seed is used

#### Scenario: Not on dirt
- **WHEN** a player uses Parsley Seeds on a dirt or grass block
- **THEN** nothing is planted

#### Scenario: Growth looks
- **WHEN** a crop grows from its first to its last age
- **THEN** it shows a seedling, a young plant, a grown plant and a ripe plant, in that order

### Requirement: What plants each crop
Cabbage, Red Cabbage and Parsley SHALL be planted from their own seed items: Cabbage Seeds, Red Cabbage Seeds and Parsley Seeds. Onion and Horseradish SHALL be planted from the vegetable itself, like a vanilla carrot. Mustard SHALL be planted from Mustard Seeds, which are also its harvest.

#### Scenario: Onion plants itself
- **WHEN** a player uses an Onion on farmland
- **THEN** an onion crop at its first age is placed and one Onion is used

#### Scenario: Pick block on a crop
- **WHEN** a player in creative mode picks a horseradish crop
- **THEN** the picked item is a Horseradish

### Requirement: Bone meal
Bone meal SHALL advance a Delicacies crop the way it advances vanilla wheat, and a ripe crop SHALL not take bone meal.

#### Scenario: Bone meal to ripe
- **WHEN** bone meal is applied to a young cabbage crop repeatedly
- **THEN** the crop reaches its ripe age after a few uses, and further bone meal is not used up on it

### Requirement: Crop drops
Breaking a crop SHALL drop items from its block loot table, like vanilla crops:
- a seeded crop (Cabbage, Red Cabbage, Parsley) that is not ripe drops one of its seeds; a ripe one drops its harvest (one head or one Parsley) plus seeds by a binomial roll that Fortune raises;
- a self planting crop (Onion, Horseradish, Mustard) drops one of its item at any age, and when ripe adds more by a binomial roll that Fortune raises.
The numbers SHALL live in the loot table JSON.

#### Scenario: Ripe cabbage
- **WHEN** a player breaks a ripe cabbage crop
- **THEN** it drops one Cabbage and, for example, zero to three Cabbage Seeds

#### Scenario: Young cabbage
- **WHEN** a player breaks a cabbage crop that is not ripe
- **THEN** it drops exactly one Cabbage Seeds and no Cabbage

#### Scenario: Ripe mustard
- **WHEN** a player breaks a ripe mustard crop
- **THEN** it drops at least one Mustard Seeds, for example one to four

#### Scenario: Fortune
- **WHEN** a player breaks ripe crops with a Fortune pickaxe or hoe
- **THEN** the extra seeds or vegetables are, on average, more than without Fortune

### Requirement: First seeds
A player SHALL find the first Delicacies planting items without any other mod: breaking short or tall grass without shears SHALL sometimes drop one of the six planting items, and the chests of village houses SHALL sometimes hold some of them. Both SHALL be global loot modifiers that add a Delicacies loot table, so the chances and the weights live in data and a pack can change or remove them.

#### Scenario: Seeds from grass
- **WHEN** a player breaks many short grass blocks by hand
- **THEN** some of them drop Cabbage Seeds, Red Cabbage Seeds, Parsley Seeds, Mustard Seeds, an Onion or a Horseradish, besides the vanilla Wheat Seeds

#### Scenario: Shears keep the grass
- **WHEN** a player breaks short grass with shears
- **THEN** it drops the short grass and no Delicacies planting item

#### Scenario: Village house chest
- **WHEN** a village house chest is opened for the first time
- **THEN** it sometimes holds a few Delicacies planting items

### Requirement: Common tags
Every Delicacies crop item SHALL carry the common tags other mods use: the harvest in `c:crops/<crop>` (`c:crops/cabbage`, `c:crops/red_cabbage`, `c:crops/onion`, `c:crops/parsley`, `c:crops/horseradish`, `c:crops/mustard`) and through it in `c:crops`; the seed items in `c:seeds/<crop>` (`c:seeds/cabbage`, `c:seeds/red_cabbage`, `c:seeds/parsley`, `c:seeds/mustard`) and through it in `c:seeds`.

#### Scenario: Other mods find the cabbage
- **WHEN** a recipe of another mod asks for `c:crops/cabbage`
- **THEN** the Delicacies Cabbage is accepted

### Requirement: Vanilla crop tags
Every planting item SHALL be in `minecraft:villager_plantable_seeds`, the small seeds also in `minecraft:chicken_food`. The crop blocks SHALL be in `minecraft:crops` and `minecraft:maintains_farmland`.

#### Scenario: Farmer villagers plant them
- **WHEN** a farmer villager holds an Onion and stands by empty farmland
- **THEN** it may plant an onion crop, like a carrot

#### Scenario: Chickens follow seeds
- **WHEN** a player holds Mustard Seeds near chickens
- **THEN** the chickens follow and can be bred with them like with Wheat Seeds

### Requirement: Compost
Every Delicacies crop item SHALL fill a composter like the vanilla item it resembles: seeds like Wheat Seeds, vegetables like a Carrot. The chances SHALL live in the `neoforge:compostables` data map.

#### Scenario: Compost a cabbage
- **WHEN** a player puts a Cabbage into a composter
- **THEN** the composter level rises with a chance like a Carrot, for example 65 percent

### Requirement: Crop names and sprites
The crop items SHALL be named "Cabbage", "Red Cabbage", "Onion", "Parsley", "Horseradish", "Mustard Seeds", "Cabbage Seeds", "Red Cabbage Seeds" and "Parsley Seeds", and SHALL be listed in the Delicacies creative tab. Raw vegetables SHALL not be edible until a later change gives them food values from data.

#### Scenario: Creative tab
- **WHEN** a player opens the Delicacies creative tab
- **THEN** it lists the six harvests and the three seed items after the Pretzel

### Requirement: Field automation
A Shuttles Field Head SHALL accept every Delicacies planting item as a field crop, and its Harvester SHALL harvest and replant Delicacies crops like vanilla crops, without any Shuttles code knowing Delicacies.

#### Scenario: Harvester on an onion field
- **WHEN** a Field Head serves a field set to Onion and its Harvester passes a ripe onion crop
- **THEN** the Harvester collects the onion drops and replants an onion crop at its first age
