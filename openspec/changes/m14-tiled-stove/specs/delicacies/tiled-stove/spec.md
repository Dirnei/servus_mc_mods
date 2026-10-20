## Purpose

Defines the Tiled Stove of Servus: Delicacies: the Stove Tile block, how a row of tiles forms a stove with the Configuration Carrot and unforms again, its firebox and fuel, how it looks while it burns, and the "is it hot" contract every cooking spot on top relies on.

## ADDED Requirements

### Requirement: Stove Tile
Servus: Delicacies SHALL add a Stove Tile block with a green glazed tile look on every face. It SHALL be crafted from Clay Balls and a stone material available on the first day, and SHALL drop itself when broken.

#### Scenario: Crafting tiles
- **WHEN** a player crafts eight Clay Balls around one Cobblestone
- **THEN** they get Stove Tiles (example: four)

#### Scenario: A loose tile
- **WHEN** a Stove Tile is placed on its own
- **THEN** it shows green tiles on every face, has no door and no cooking plate, and heats nothing

### Requirement: Forming the stove
A straight horizontal row of unformed Stove Tiles SHALL form a Tiled Stove when a player uses the Configuration Carrot on one of them. The row SHALL run across the face the door goes on, SHALL include the clicked tile, and SHALL consist of every unformed tile joined in that line. The row SHALL be at least two tiles long and at most the maximum length from data (example: four). The clicked tile SHALL get the firebox door on its front: the clicked face when it is a side face, otherwise the side facing the player. Every tile of a formed stove SHALL show an iron cooking plate with a hob ring on top. The player SHALL be told whether the stove formed and how long it is, or why it did not form.

#### Scenario: Two tiles form a stove
- **WHEN** two Stove Tiles stand side by side and a player uses the carrot on the front face of one of them
- **THEN** both tiles form one stove, the clicked tile has the firebox door on that face, both show a cooking plate on top, and the player is told the stove formed two tiles long

#### Scenario: A single tile is refused
- **WHEN** a player uses the carrot on a Stove Tile with no other tile beside it in the row
- **THEN** nothing forms and the player is told a stove needs at least two tiles

#### Scenario: Too long a row is refused
- **WHEN** a row holds more tiles than the maximum length and the carrot is used on one of them
- **THEN** nothing forms and the player is told the maximum length

#### Scenario: Clicked on top
- **WHEN** the carrot is used on the top face of a tile in a row of three that runs east to west, by a player standing south of it
- **THEN** the stove forms with the door on the south face of the clicked tile

#### Scenario: Maximum length from data
- **WHEN** a datapack sets the stove's maximum length to six
- **THEN** a row of six tiles forms and a row of seven is refused

### Requirement: Unforming the stove
Breaking any tile of a formed stove, by hand, by an explosion, or by dismantling it with a sneaking carrot click, SHALL unform the whole stove: the other tiles SHALL go back to loose tiles and the firebox's fuel SHALL drop as items. A sneaking carrot click on any Stove Tile SHALL dismantle it into its item.

#### Scenario: Breaking a tile
- **WHEN** a player breaks one tile of a formed stove of three that holds fuel
- **THEN** that tile drops itself, the other two become loose tiles without door or plate, and the fuel drops as items

#### Scenario: Dismantling with the carrot
- **WHEN** a sneaking player uses the carrot on a tile of a formed stove
- **THEN** the tile drops as an item and the stove unforms

### Requirement: Firebox fuel
Each formed stove SHALL have one firebox with one fuel slot that takes any item a vanilla furnace burns, and nothing else. A player SHALL add fuel by right clicking any tile of the stove with it, and automation SHALL insert fuel through the item capability of any tile. Containers left behind by burnt fuel (such as an empty bucket from a lava bucket) SHALL be extractable through the item capability. An empty hand right click on a tile SHALL tell the player whether the fire burns and how much fuel is left.

#### Scenario: Fuel by hand
- **WHEN** a player right clicks a tile of a formed stove with Coal
- **THEN** the Coal goes into the firebox's fuel slot

#### Scenario: Fuel through a hopper
- **WHEN** a hopper holding Charcoal points into a tile of a formed stove
- **THEN** the Charcoal moves into the fuel slot

#### Scenario: Not a fuel
- **WHEN** something tries to insert Cobblestone into the fuel slot
- **THEN** it is refused

### Requirement: Fire on demand
The firebox SHALL light a new fuel item only while some cooking spot on the stove asks for heat, and SHALL then burn that item for its vanilla furnace burn time, also when the spots stop asking. While the firebox burns, the stove SHALL count as hot. When the fuel item is burnt and nothing asks for heat, the fire SHALL go out. A wood fire SHALL be hot at once when it lights and SHALL go cold as soon as its fuel ends.

#### Scenario: Nothing to cook
- **WHEN** a stove holds fuel and no vessel on it has anything to cook
- **THEN** no fuel is burnt and the stove stays cold

#### Scenario: Lit by a vessel
- **WHEN** a Pot with a full recipe stands on a stove with Coal in its fuel slot
- **THEN** one Coal is taken, the stove is hot within a tick or two, and it stays hot for the Coal's burn time

### Requirement: Lit look
While the firebox burns, the door SHALL glow with light around the stove and its fire SHALL flicker, the cooking plates SHALL show their lit look, and flames and crackling SHALL come from the door now and then. When the fire goes out, the door, plates and light SHALL return to cold.

#### Scenario: Fire goes out
- **WHEN** the last fuel item is burnt
- **THEN** the door stops glowing and flickering, the plates look cold and the light is gone

### Requirement: Is it hot
Every cooking spot SHALL learn whether it is heated by asking the block below it whether it is a firebox that is hot, and SHALL be able to ask for heat. Other Servus mods (such as the Brewery) SHALL be able to offer the same firebox on their own blocks and to reuse the same fire logic. The fire logic SHALL keep one place where a later heat source that runs on FE with a preheat time can replace burning fuel without changing the cooking spots.

#### Scenario: A vessel on a loose tile
- **WHEN** a Pot with a full recipe stands on a loose Stove Tile
- **THEN** it does not cook

#### Scenario: Every tile is a cooking spot
- **WHEN** Pots stand on both tiles of a lit two tile stove whose door is on one of them
- **THEN** both Pots cook
