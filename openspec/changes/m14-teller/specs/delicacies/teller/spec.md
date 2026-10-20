## Purpose

Defines the Teller of Servus: Delicacies: the Plate, plated meals placed as blocks and eaten portion by portion, their crafting, the meal data map, the Properly Full effect, drops and look.

## ADDED Requirements

### Requirement: Plate
Servus: Delicacies SHALL add the Plate, a white ceramic plate with a blue rim, crafted shapeless from Clay Balls and a blue dye (`c:dyes/blue`) (example: three Clay Balls and one blue dye into two Plates). The Plate SHALL be placeable only on a block with a solid top face, SHALL be a flat block no higher than its food, and SHALL drop itself when broken. A placed empty Plate SHALL do nothing on an empty hand right click.

#### Scenario: Placing a plate
- **WHEN** a player places a Plate on a solid block
- **THEN** a flat Plate block stands there and breaking it drops one Plate

### Requirement: Plated meals
Servus: Delicacies SHALL add placed meals that sit on a plate and face the player who placed them: the Roast Pork Plate, the Schnitzel Plate, the Weisswurst Breakfast and the Weisswurst Breakfast with Weissbier. Each SHALL be crafted shapeless from a Plate and its parts:
- Roast Pork Plate: Roast Pork, two items of `servus_delicacies:dumplings` (Bread Dumpling, Potato Dumpling), Braised Red Cabbage and Gravy.
- Schnitzel Plate: Schnitzel and Potato Salad.
- Weisswurst Breakfast: two Weisswurst, a Pretzel and Sweet Mustard.
- Weisswurst Breakfast with Weissbier: the Weisswurst Breakfast's parts and one item of `servus_delicacies:breakfast_beers`.
Crafting with Gravy or Potato Salad SHALL leave their Bowl in the grid. The recipe of the Weisswurst Breakfast with Weissbier SHALL load only when Servus: Brewery is installed; Servus: Delicacies SHALL NOT need Servus: Brewery to load.

#### Scenario: Crafting a Roast Pork Plate
- **WHEN** a player crafts a Plate, Roast Pork, two Bread Dumplings, Braised Red Cabbage and Gravy
- **THEN** the player gets one Roast Pork Plate and keeps a Bowl

#### Scenario: Crafting a Weisswurst Breakfast
- **WHEN** a player crafts a Plate, two Weisswurst, a Pretzel and Sweet Mustard
- **THEN** the player gets one Weisswurst Breakfast

#### Scenario: No beer without Brewery
- **WHEN** Servus: Brewery is not installed
- **THEN** no recipe makes the Weisswurst Breakfast with Weissbier and the Delicacies recipes load without errors

### Requirement: Portions
A placed meal SHALL hold four portions, shown as food boxes on the plate. An empty hand right click by a player who can eat SHALL eat one portion: the player's hunger and saturation rise by the portion values of the meal in the data map `servus_delicacies:meal`, and one food box disappears. A player who cannot eat SHALL eat nothing. A meal without a data map entry SHALL NOT be eaten.

#### Scenario: Eating one portion
- **WHEN** a hungry player right clicks a full Schnitzel Plate with an empty hand
- **THEN** the player's hunger rises by the data value, the meal has three portions left and shows one food box less

#### Scenario: Full player
- **WHEN** a player with full hunger right clicks a meal with an empty hand
- **THEN** the meal keeps its portions

### Requirement: Finishing a meal
Eating the last portion SHALL leave an empty Plate block in the meal's place and SHALL give the player the meal's finish effects from the data map (example: Properly Full for 1200 ticks after the Roast Pork Plate). The Weisswurst Breakfast with Weissbier SHALL also give the player the first item of `servus_delicacies:breakfast_beers`, when that tag has one.

#### Scenario: Last portion
- **WHEN** a hungry player eats the fourth portion of a Roast Pork Plate
- **THEN** an empty Plate stands where the meal stood and the player has the Properly Full effect

### Requirement: Properly Full
Servus: Delicacies SHALL add the beneficial effect Properly Full. While it lasts it SHALL heal the player one health point at a fixed interval that halves with each amplifier level (amplifier 0: every 100 ticks). Its duration and amplifier SHALL come from the meal data map.

#### Scenario: Healing while Properly Full
- **WHEN** a hurt player has Properly Full for longer than its interval
- **THEN** the player's health has risen

### Requirement: Meal drops
Breaking a meal with all four portions SHALL drop the meal item. Breaking a started meal SHALL drop only one Plate.

#### Scenario: Breaking a started meal
- **WHEN** a player breaks a Weisswurst Breakfast with two portions left
- **THEN** one Plate drops and nothing else

#### Scenario: Picking up a full meal
- **WHEN** a player breaks a full Schnitzel Plate
- **THEN** one Schnitzel Plate drops

### Requirement: Meal data
The data map `servus_delicacies:meal` on items SHALL give, per meal item, the food values of one portion (`portion`: `nutrition`, `saturation`) and the finish effects (`effects`, a list of effect instances). It SHALL be synced to clients.

#### Scenario: A datapack changes a portion
- **WHEN** a datapack sets the Schnitzel Plate's portion nutrition to 2
- **THEN** each portion of a Schnitzel Plate raises hunger by 2

### Requirement: Meal look
The Plate SHALL be a flat white plate with a blue rim one pixel high. Each meal SHALL show its food as small boxes on the plate (Roast Pork Plate: a pool of gravy, the roast with a crackling top, two dumplings, red cabbage; Schnitzel Plate: two Schnitzel pieces and two potato salad heaps; Weisswurst Breakfast: two Weisswurst, two Pretzel pieces and a dollop of mustard; with Weissbier also a glass of Weissbier with a foam top). The base parts (gravy, mustard) SHALL stay until the meal is finished. No two faces SHALL share a plane where they overlap. The items SHALL show the full meal model.

#### Scenario: Look while eating
- **WHEN** a player eats portions from a Roast Pork Plate
- **THEN** one food box disappears per portion and the gravy stays until the last portion
