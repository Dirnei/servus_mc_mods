## Purpose

Defines the meat dishes of Servus: Delicacies: the cuts made on the Cutting Board, Schnitzel and Cordon Bleu from the Pan, Ham, Weisswurst and Sweet Mustard from the Pot, the sliceable Leberkaese loaf and Roast Pork from the Baking Oven, Gravy, and their food values.

## ADDED Requirements

### Requirement: Meat cuts
The Cutting Board SHALL cut with a knife a raw Porkchop into a Pork Cutlet, a Pork Cutlet into Minced Pork and raw Beef into Minced Beef (examples: one cutlet per porkchop, two minced pieces per cutlet or beef). Pork Cutlet, Minced Pork and Minced Beef SHALL NOT be edible.

#### Scenario: Cutting a porkchop
- **WHEN** a raw Porkchop lies on the Cutting Board and a player right clicks it with the Kitchen Knife
- **THEN** the Porkchop is gone and the board holds a Pork Cutlet

#### Scenario: Mincing beef
- **WHEN** raw Beef is cut on the board with the knife, with the shipped recipe (example: two pieces)
- **THEN** the board holds two Minced Beef

### Requirement: Schnitzel
Servus: Delicacies SHALL add a Breaded Cutlet, crafted shapeless from a Pork Cutlet, Flour (any item in `c:flours/wheat`), an Egg (any item in `c:eggs`) and Bread Crumbs, and SHALL ship a pan recipe that fries a Breaded Cutlet into a Schnitzel. The Breaded Cutlet SHALL NOT be edible.

#### Scenario: Breading a cutlet
- **WHEN** a player crafts a Pork Cutlet, a Flour, an Egg and Bread Crumbs
- **THEN** the player gets a Breaded Cutlet

#### Scenario: Frying the Schnitzel
- **WHEN** a Pan on a lit Tiled Stove holds a Breaded Cutlet
- **THEN** after the recipe's time the Pan holds a Schnitzel and no Breaded Cutlet

#### Scenario: No Schnitzel without fire
- **WHEN** a Pan holding a Breaded Cutlet stands on a stove without fuel
- **THEN** nothing fries

### Requirement: Ham
The Pot SHALL simmer a raw Porkchop with Salt (any item in `c:dusts/salt`) and water into Ham (example: one porkchop, one salt and 250 mB of water into two Ham). Ham SHALL be edible.

#### Scenario: Simmering ham
- **WHEN** a Pot on a lit stove holds a raw Porkchop, a Salt and at least 250 mB of water
- **THEN** after the recipe's time the Pot holds Ham and 250 mB less water

### Requirement: Cordon Bleu
Servus: Delicacies SHALL add a Raw Cordon Bleu, crafted shapeless from a Pork Cutlet, Ham, Cheese (any item in `c:foods/cheese`), Flour, an Egg and Bread Crumbs, and SHALL ship a pan recipe that fries it into a Cordon Bleu. Servus: Delicacies SHALL declare the item tag `c:foods/cheese`, so the recipe loads whether or not any cheese is installed. The Raw Cordon Bleu SHALL NOT be edible.

#### Scenario: Crafting with any cheese
- **WHEN** an item is in `c:foods/cheese` and a player crafts it with a Pork Cutlet, Ham, Flour, an Egg and Bread Crumbs
- **THEN** the player gets a Raw Cordon Bleu

#### Scenario: Frying the Cordon Bleu
- **WHEN** a Pan on a lit stove holds a Raw Cordon Bleu
- **THEN** after the recipe's time the Pan holds a Cordon Bleu

### Requirement: Weisswurst
Servus: Delicacies SHALL add Sausage Meat, crafted shapeless from Minced Pork, Minced Beef, Parsley (any item in `c:crops/parsley`) and Salt (example: two Sausage Meat), and SHALL ship a pot recipe that simmers Sausage Meat in water into Weisswurst (example: two Sausage Meat and 250 mB of water into two Weisswurst). There SHALL be no casing item. Sausage Meat SHALL NOT be edible.

#### Scenario: Weisswurst in the Pot
- **WHEN** a Pot on a lit stove holds two Sausage Meat and at least 250 mB of water
- **THEN** after the recipe's time the Pot holds two Weisswurst, no Sausage Meat and 250 mB less water

#### Scenario: No water, no Weisswurst
- **WHEN** a Pot on a lit stove holds two Sausage Meat and no water
- **THEN** nothing cooks

### Requirement: Sweet Mustard
Servus: Delicacies SHALL ship a `servus_foundation:milling` recipe that grinds Mustard Seeds (any item in `c:crops/mustard`) into Mustard Flour on the Foundation Mill, and a pot recipe that cooks Mustard Flour, Sugar and water into Sweet Mustard (example: two Mustard Flour, one Sugar and 250 mB of water into two Sweet Mustard). Mustard Flour SHALL be in `c:flours/mustard`. Sweet Mustard is a jar of mustard and SHALL NOT be edible on its own.

#### Scenario: Milling mustard
- **WHEN** a driven Mill holds Mustard Seeds
- **THEN** after the recipe's work the Mill holds Mustard Flour

#### Scenario: Cooking sweet mustard
- **WHEN** a Pot on a lit stove holds two Mustard Flour, one Sugar and at least 250 mB of water
- **THEN** after the recipe's time the Pot holds two Sweet Mustard

### Requirement: Leberkaese loaf
The Baking Oven SHALL bake Minced Pork, Minced Beef and Salt into a Leberkaese loaf (example: two Minced Pork, two Minced Beef and one Salt into one loaf). The loaf SHALL be placeable on a solid block and give four slices. A right click with any item in `c:tools/knife` SHALL cut one Leberkaese Slice into the player's inventory, cost the knife one durability and shorten the loaf; the last cut SHALL remove the block. Anything else SHALL do nothing.

#### Scenario: Baking the loaf
- **WHEN** a Baking Oven on a lit stove holds two Minced Pork, two Minced Beef and a Salt
- **THEN** after the recipe's time it holds one Leberkaese loaf

#### Scenario: Slicing with the knife
- **WHEN** a player right clicks an uncut placed loaf with the Kitchen Knife
- **THEN** the player gets one Leberkaese Slice, the knife lost one durability and the loaf is one slice shorter

#### Scenario: The last slice
- **WHEN** a player has cut three slices off a loaf and cuts once more
- **THEN** the player gets the fourth slice and the loaf block is gone

#### Scenario: No knife, no slice
- **WHEN** a player right clicks a placed loaf with an empty hand or a stick
- **THEN** the loaf stays as it is and the player gets nothing

### Requirement: Loaf drops and board slicing
Breaking an uncut loaf SHALL drop the loaf, breaking a cut loaf SHALL drop its remaining slices. The Cutting Board SHALL cut a loaf item with a knife into the same number of slices as the placed loaf gives.

#### Scenario: Breaking a cut loaf
- **WHEN** a player breaks a loaf with one slice cut off
- **THEN** it drops three Leberkaese Slices

#### Scenario: Slices on the board
- **WHEN** a loaf item lies on the Cutting Board and is cut with the knife
- **THEN** the board holds four Leberkaese Slices

### Requirement: Leberkaese Roll
Servus: Delicacies SHALL add a Leberkaese Roll, crafted shapeless from a Leberkaese Slice and a Bread Roll.

#### Scenario: Making a roll
- **WHEN** a player crafts a Leberkaese Slice with a Bread Roll
- **THEN** the player gets a Leberkaese Roll

### Requirement: Roast Pork and Gravy
The Baking Oven SHALL roast raw Porkchops with an Onion (any item in `c:crops/onion`) and Salt into Roast Pork with its crackling (example: two porkchops, one onion and one salt into two Roast Pork). The Pot SHALL make Gravy from Chopped Onion, Flour, a Bowl and water (example: one of each and 250 mB of water into one Gravy). Gravy SHALL be eaten like a stew and give the Bowl back.

#### Scenario: Roasting in the oven
- **WHEN** a Baking Oven on a lit stove holds two raw Porkchops, an Onion and a Salt
- **THEN** after the recipe's time it holds two Roast Pork and none of the ingredients

#### Scenario: Gravy gives the bowl back
- **WHEN** a hungry player eats Gravy
- **THEN** the player's hunger rises by the data value and the player holds an empty Bowl

### Requirement: Meat dish food values
Schnitzel, Cordon Bleu, Ham, Weisswurst, Leberkaese Slice, Leberkaese Roll, Roast Pork and Gravy SHALL be edible with hunger and saturation from the `servus_delicacies:food` data map. The shipped values of Schnitzel, Cordon Bleu, Leberkaese Roll and Roast Pork SHALL be higher than vanilla Cooked Porkchop (example: Schnitzel 10 hunger and 16 saturation, Cordon Bleu 12 and 19.2). The meat dishes SHALL be in `c:foods/cooked_meat`, every edible dish in `c:foods`, and Gravy in `c:foods/soup`.

#### Scenario: Eating a Schnitzel
- **WHEN** a hungry player eats a Schnitzel with the shipped data (example: 10 hunger)
- **THEN** the player's hunger rises by 10 and the Schnitzel is used

#### Scenario: Raw cuts are no food
- **WHEN** a player tries to eat a Pork Cutlet
- **THEN** nothing happens

### Requirement: Meat dishes in JEI
When JEI is installed, the meat recipes SHALL show in the existing Pot, Pan, Baking Oven, Cutting Board and milling categories, and the Leberkaese loaf SHALL have an info page that says to place it and cut it with a knife.

#### Scenario: Looking up the Schnitzel
- **WHEN** a player looks up how to make a Schnitzel in JEI
- **THEN** the Pan category shows the Breaded Cutlet, the Schnitzel and the time
