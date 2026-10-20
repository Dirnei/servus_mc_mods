## Purpose

Defines the Bavarian sides of Servus: Delicacies: Sauerkraut, Cheese, Red Cabbage, Bread Dumpling, Potato Dumpling and Potato Salad, their recipes, tags and food values, and milk as a fluid.

## ADDED Requirements

### Requirement: Milk as a fluid
Servus: Delicacies SHALL turn on NeoForge's milk fluid, so a Milk Bucket empties into fluid tanks that take milk. Recipes SHALL ask for milk by the fluid tag `c:milk`.

#### Scenario: Milk in the Pot
- **WHEN** a player right clicks an empty Pot with a Milk Bucket
- **THEN** the Pot holds 1000 mB of milk and the player holds an empty Bucket

### Requirement: Sauerkraut
Servus: Delicacies SHALL add Sauerkraut, fermented in the Fermenting Crock from Shredded Cabbage and Salt (`c:dusts/salt`) (example: four Shredded Cabbage and one Salt into four Sauerkraut in three in-game days). Sauerkraut SHALL NOT be edible on its own; shapeless crafting of one Sauerkraut and a Bowl SHALL give a Bowl of Sauerkraut, which is edible and gives the Bowl back when eaten.

#### Scenario: Sauerkraut after time
- **WHEN** a crock holds four Shredded Cabbage and one Salt and the recipe's time passes
- **THEN** the crock holds four Sauerkraut

#### Scenario: Serving Sauerkraut
- **WHEN** a hungry player eats a Bowl of Sauerkraut
- **THEN** the player's hunger rises by the data map value and the player keeps an empty Bowl

### Requirement: Cheese
Servus: Delicacies SHALL add Cheese wedges, fermented in the Fermenting Crock from milk and Salt (example: 1000 mB of milk and one Salt into four Cheese in two in-game days). Cheese SHALL be edible and SHALL be in the tags `c:foods/cheese` and `c:foods`.

#### Scenario: Cheese after time
- **WHEN** a crock holds 1000 mB of milk and one Salt and the recipe's time passes
- **THEN** the crock holds four Cheese and no milk

### Requirement: Red Cabbage
Servus: Delicacies SHALL add Shredded Red Cabbage, cut from a Red Cabbage on the Cutting Board, and Braised Red Cabbage, cooked in the Pot from Shredded Red Cabbage, an Apple, Sugar and water (example: two Shredded Red Cabbage, one Apple, one Sugar and 250 mB of water into two Braised Red Cabbage). Braised Red Cabbage SHALL NOT be edible on its own; shapeless crafting of one Braised Red Cabbage and a Bowl SHALL give a Bowl of Red Cabbage, which is edible and gives the Bowl back.

#### Scenario: Red Cabbage in the Pot
- **WHEN** a Pot on a lit stove holds two Shredded Red Cabbage, an Apple, Sugar and water
- **THEN** after the recipe's time it holds two Braised Red Cabbage

### Requirement: Bread Dumpling
Servus: Delicacies SHALL add the Bread Dumpling, simmered in the Pot from Bread Crumbs, an Egg (`c:eggs`), Chopped Onion and Parsley in milk (example: two Bread Crumbs, one Egg, one Chopped Onion, one Parsley and 250 mB of milk into two Bread Dumplings). It SHALL be edible.

#### Scenario: Bread Dumplings in the Pot
- **WHEN** a Pot on a lit stove holds two Bread Crumbs, an Egg, a Chopped Onion, a Parsley and milk
- **THEN** after the recipe's time it holds two Bread Dumplings

### Requirement: Potato Dumpling
Servus: Delicacies SHALL add Grated Potato, cut from a Potato on the Cutting Board, and the Potato Dumpling, simmered in the Pot from Grated Potato, Boiled Potatoes and Salt in water (example: two Grated Potato, one Boiled Potatoes, one Salt and 250 mB of water into two Potato Dumplings). The Potato Dumpling SHALL be edible.

#### Scenario: Potato Dumplings in the Pot
- **WHEN** a Pot on a lit stove holds two Grated Potato, one Boiled Potatoes, one Salt and water
- **THEN** after the recipe's time it holds two Potato Dumplings

### Requirement: Potato Salad
Servus: Delicacies SHALL add Potato Salad, crafted shapeless from two Boiled Potatoes, one Chopped Onion and a Bowl. It SHALL be edible and give the Bowl back.

#### Scenario: Crafting Potato Salad
- **WHEN** a player crafts two Boiled Potatoes, a Chopped Onion and a Bowl
- **THEN** the player gets one Potato Salad

### Requirement: Side food values and tags
The hunger and saturation of every edible side SHALL come from the `servus_delicacies:food` data map, moderate values for a side rather than a full meal (example: Bread Dumpling 6 hunger and 7.2 saturation). Every edible side SHALL be in `c:foods`, the bowl foods also in `c:foods/soup`.

#### Scenario: Eating a dumpling
- **WHEN** a hungry player eats a Bread Dumpling with the shipped data
- **THEN** the player's hunger rises by 6 and the dumpling is used
