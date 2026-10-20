## Purpose

Defines the cooking vessels of Servus: Delicacies, the Pot and the Pan, the data driven recipes they cook, how they are filled and emptied by hand and by automation, and the cooked foods with their food values in data.

## ADDED Requirements

### Requirement: Pot and Pan
Servus: Delicacies SHALL add a Pot (family steel, two handles) and a Pan (dark iron, wooden handle) as placed blocks. Each SHALL hold a few input stacks and one output stack; the Pot SHALL also hold one fluid tank whose size comes from data (example: 4000 mB). A vessel SHALL cook only while the block below it is a hot firebox; anywhere else it only holds what is in it. Breaking a vessel SHALL drop the vessel and every item in it.

#### Scenario: No cooking without fire
- **WHEN** a Pot with two Potatoes and water stands on a formed stove with no fuel
- **THEN** nothing cooks, however long it waits

#### Scenario: Breaking a full Pan
- **WHEN** a player breaks a Pan holding an Egg and a Fried Egg
- **THEN** the Pan, the Egg and the Fried Egg drop as items

### Requirement: Pot and pan recipes
Pot recipes (`servus_delicacies:pot`) SHALL name counted item ingredients, an optional counted fluid, a result and a time in ticks. Pan recipes (`servus_delicacies:pan`) SHALL name counted item ingredients, a result and a time. A recipe SHALL match when every ingredient is met by a different input stack holding at least its count, every non empty input stack is used by the recipe, and the Pot's tank holds the recipe's fluid amount. While a recipe matches, the result fits into the output and the firebox is hot, the vessel SHALL count up the recipe's time; when it is reached it SHALL take the ingredient counts and the fluid and put the result into the output, then start again while the inputs last. When the fire goes out the count SHALL pause; when the inputs no longer match it SHALL start over. Recipes SHALL come from datapacks and be shown to clients.

#### Scenario: Boiled Potatoes
- **WHEN** a Pot on a lit stove holds two Potatoes and at least 250 mB of water, with the shipped recipe (example: 200 ticks)
- **THEN** after the recipe's time the Pot holds two Boiled Potatoes, no Potatoes and 250 mB less water

#### Scenario: A stray item blocks the recipe
- **WHEN** a Pan holds an Egg and a Cobblestone that no recipe wants
- **THEN** nothing cooks

#### Scenario: Batch cooking
- **WHEN** a Pan on a lit stove holds three Eggs
- **THEN** it fries them one after another into three Fried Eggs

#### Scenario: Output full
- **WHEN** a Pan's output holds a full stack of Fried Eggs and another Egg is in it
- **THEN** it does not cook until the output has room

### Requirement: Vessels by hand
Right clicking a vessel with an item that some recipe of that vessel takes SHALL put as much of the held stack into its inputs as fits. Right clicking the Pot with a filled fluid container whose fluid some pot recipe uses SHALL empty it into the tank, and with an empty bucket SHALL fill the bucket from the tank. Right clicking a vessel with an empty hand SHALL give the player the whole output; when the output is empty it SHALL give back the last filled input stack. Items no recipe takes SHALL not go in.

#### Scenario: Water from a bucket
- **WHEN** a player right clicks an empty Pot with a Water Bucket
- **THEN** the Pot holds 1000 mB of water and the player holds an empty Bucket

#### Scenario: Taking the result
- **WHEN** a player right clicks a Pot holding two Boiled Potatoes with an empty hand
- **THEN** the player gets the two Boiled Potatoes

#### Scenario: Refused item
- **WHEN** a player right clicks a Pan with Dirt
- **THEN** the Dirt stays in the player's hand

### Requirement: Vessels by automation
Each vessel SHALL expose the standard item capability on every side: insertion SHALL go into the inputs and take only items some recipe of the vessel uses; extraction SHALL take only from the output. The Pot SHALL also expose the standard fluid capability on every side for its tank, taking only fluids some pot recipe uses.

#### Scenario: Hopper feeds a pot
- **WHEN** a hopper holding Potatoes points down into a Pot with water on a lit stove
- **THEN** the Potatoes move into the Pot and are cooked into Boiled Potatoes

#### Scenario: Pipes take only results
- **WHEN** a pipe or hopper below a Pan pulls items while the Pan holds an Egg and a Fried Egg
- **THEN** only the Fried Egg comes out

### Requirement: Vessel look
A vessel SHALL show its input items and its result inside it, and the Pot SHALL show its fluid surface at a height that follows how full the tank is. While a vessel cooks, steam SHALL rise from it.

#### Scenario: Steam while cooking
- **WHEN** a Pot cooks on a lit stove
- **THEN** steam rises from it, and it stops when the Pot stops cooking

### Requirement: Cooked foods
Servus: Delicacies SHALL add the foods Boiled Potatoes, Fried Egg, Fried Onions and Fried Potatoes, and the ingredients Chopped Onion and Shredded Cabbage. The hunger and saturation each food restores SHALL come from a datapack data map (`servus_delicacies:food`) and SHALL be shown to clients; a food a player is too full to eat SHALL not be eaten. An item without an entry in that map SHALL not be edible.

#### Scenario: Eating Fried Potatoes
- **WHEN** a hungry player eats Fried Potatoes with the shipped data (example: 8 hunger, 12.8 saturation)
- **THEN** the player's hunger rises by 8 and the Fried Potatoes are used

#### Scenario: Full player
- **WHEN** a player with a full hunger bar tries to eat a Fried Egg
- **THEN** the Fried Egg is not eaten

#### Scenario: Food values from a datapack
- **WHEN** a datapack sets Boiled Potatoes to restore 2 hunger
- **THEN** eating them restores 2 hunger

### Requirement: Starter cooking recipes
Servus: Delicacies SHALL ship pot and pan recipes so the kitchen is playable on day one: two Potatoes with water into two Boiled Potatoes in the Pot; an Egg into a Fried Egg, two Chopped Onions into Fried Onions, and two Boiled Potatoes with a Chopped Onion into two Fried Potatoes in the Pan. The Pot SHALL be crafted from iron and the Pan from iron and a stick.

#### Scenario: Fried Potatoes chain
- **WHEN** a player boils Potatoes in the Pot, cuts an Onion on the Cutting Board and puts two Boiled Potatoes and a Chopped Onion into a Pan on a lit stove
- **THEN** the Pan makes two Fried Potatoes
