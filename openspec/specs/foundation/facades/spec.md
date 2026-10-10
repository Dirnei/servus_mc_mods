# foundation/facades Specification

## Purpose

Facades hide ducts behind the look of ordinary blocks; the overlay brings them back for whoever needs to see them.

## Requirements

### Requirement: Facade crafting
Crafting any full, block entity free block together with one nugget SHALL give six Facades of that block. Any item in the common nuggets tag SHALL count, such as an iron or gold nugget or a copper nugget from another mod. The facade item SHALL name its block.

#### Scenario: Stone facade
- **WHEN** stone and an iron nugget are in the crafting grid
- **THEN** six "Facade (Stone)" result

#### Scenario: Any nugget
- **WHEN** stone and a gold nugget are in the crafting grid
- **THEN** six "Facade (Stone)" result

#### Scenario: Not a full block
- **WHEN** a slab and an iron nugget are in the grid
- **THEN** nothing results

#### Scenario: Ingot no longer works
- **WHEN** stone and an iron ingot are in the grid
- **THEN** nothing results

### Requirement: Applying and removing
Using a Facade or Hollow Facade on a duct SHALL place a panel on the clicked face as described above. Sneaking with the Configuration Carrot on a panel SHALL take off that panel only, drop its item and keep the duct and its other panels. Sneaking with the carrot on a part of the duct that is not a panel SHALL remove the duct as before. Breaking the duct SHALL drop every panel's item too.

#### Scenario: Apply and take off
- **WHEN** a player puts a stone panel on the north face and an oak panel on the top face of a duct, then sneaks with the carrot on the stone panel
- **THEN** a stone Facade drops, the north face is open again and the oak panel stays

#### Scenario: Break with panels
- **WHEN** a player breaks a duct with a solid and a hollow panel
- **THEN** the duct's lines, one Facade and one Hollow Facade drop

### Requirement: Example facades in the creative tab
The Servus creative tab SHALL list Facades of a few common full blocks, for example stone, stone bricks, oak planks, bricks and deepslate tiles, each named after its block.

#### Scenario: Creative tab
- **WHEN** a player opens the Servus creative tab
- **THEN** it shows "Facade (Stone)", "Facade (Oak Planks)" and the other example facades

### Requirement: Facades in JEI
When JEI is installed, every example facade SHALL appear in JEI's item list as its own entry, told apart by its block. Each SHALL have a crafting recipe showing its block and one nugget giving six facades of that block. Each SHALL also have an information page saying that any full block without a block entity works the same way and that the facade is used on a Utility Duct. Without JEI the game SHALL load and facades SHALL work unchanged.

#### Scenario: Search
- **WHEN** a player types "facade" in JEI's search
- **THEN** every example facade shows as a separate entry

#### Scenario: Recipe of a facade
- **WHEN** a player looks up the recipe of "Facade (Oak Planks)" in JEI
- **THEN** a crafting recipe shows oak planks and a nugget giving six "Facade (Oak Planks)"

#### Scenario: Any block hint
- **WHEN** a player opens the information page of any example facade
- **THEN** it says that any full block plus one nugget gives six facades of that block

#### Scenario: Uses of a nugget
- **WHEN** a player looks up the uses of an iron nugget in JEI
- **THEN** the facade example recipes are among them

#### Scenario: No JEI
- **WHEN** the game runs without JEI
- **THEN** it loads, and the example facades still show in the creative tab

### Requirement: Facades are panels on one face
A Facade SHALL be a panel one pixel thick that covers one face of a Utility Duct. Using a Facade on a duct SHALL place it on the face the player clicked and consume one item, unless that face already carries a panel. A duct SHALL carry at most one panel per face, so up to six, each of its own block. A Facade SHALL NOT be placeable on any other block.

#### Scenario: Panel on the clicked face
- **WHEN** a player uses a stone Facade on the north face of a duct
- **THEN** a stone panel covers the north face, the duct's other faces stay open and one Facade is used up

#### Scenario: Face already covered
- **WHEN** a player uses a Facade on a face that already carries a panel
- **THEN** nothing changes and the item stays

#### Scenario: Six panels
- **WHEN** a player covers all six faces of a duct with stone panels
- **THEN** the duct looks like a stone block from every side

#### Scenario: Not a duct
- **WHEN** a player uses a Facade on a chest
- **THEN** nothing is placed

### Requirement: Solid panels close their side
A solid panel SHALL close its face of the duct for every line: no arm SHALL be drawn there, and the duct SHALL neither join a neighbouring duct nor reach a machine or inventory through that face. Item, energy, fluid and data networks SHALL split at a closed face. The same SHALL hold when the panel sits on the neighbouring duct's face that touches this duct. Taking the panel off SHALL open the face again.

#### Scenario: Two ducts kept apart
- **WHEN** two item ducts touch and one carries a solid panel on the touching face
- **THEN** neither draws an arm toward the other and they belong to two separate item networks

#### Scenario: Machine cut off
- **WHEN** a duct beside a chest carries a solid panel on the face toward the chest
- **THEN** the duct has no endpoint there and moves no items into or out of that chest

#### Scenario: Panel removed
- **WHEN** the solid panel between two item ducts is taken off
- **THEN** the arms come back and the ducts join one item network

### Requirement: Hollow panels let the duct through
A Hollow Facade SHALL place like a Facade and cover its face with a frame around a hole in the middle. The hole SHALL be large enough for the thickest duct arm. A hollow panel SHALL NOT close its face: arms, links and endpoints through that face SHALL stay as they are without the panel.

#### Scenario: Pipe through the wall
- **WHEN** a hollow stone panel covers the face between two item ducts
- **THEN** the arm passes through the hole and both ducts stay in one item network

### Requirement: Hollow Facade crafting
Eight Facades of the same block arranged in a ring around an empty centre slot of a three by three grid SHALL craft eight Hollow Facades of that block. Facades of different blocks in the ring SHALL craft nothing. A Hollow Facade SHALL be named after its block, for example "Hollow Facade (Stone)".

#### Scenario: Stone ring
- **WHEN** eight stone Facades fill a crafting table except the centre
- **THEN** eight "Hollow Facade (Stone)" result

#### Scenario: Mixed ring
- **WHEN** the ring holds seven stone Facades and one oak Facade
- **THEN** nothing results

### Requirement: Panel shape
Each panel SHALL add its own outline and collision to the duct's shape: a full one pixel slab on its face for a solid panel and a frame around the hole for a hollow panel. The duct's own parts SHALL keep their outline.

#### Scenario: Standing on a panel
- **WHEN** a duct lies in the floor with a solid panel on its top face
- **THEN** a player can stand on the panel and sees its outline when looking at it

### Requirement: Hollow examples
The Servus creative tab and JEI SHALL list a Hollow Facade for each example block next to the Facades, and JEI SHALL show the ring recipe for each.

#### Scenario: Look up a hollow facade
- **WHEN** a player looks up the recipe of "Hollow Facade (Bricks)" in JEI
- **THEN** a crafting recipe shows eight brick Facades around an empty centre giving eight "Hollow Facade (Bricks)"

### Requirement: Panels render with their block
Each panel SHALL render with the textures and tint of its block's matching face, one pixel thick, next to the duct's own parts. A solid panel SHALL fill its whole face, a hollow panel SHALL leave the hole open so the arm behind it is visible. A duct whose six faces carry solid panels of one block SHALL look like that block.

#### Scenario: Looking at a wall
- **WHEN** a row of ducts in a stone wall carries stone panels on the faces toward the room
- **THEN** the wall looks uniform from the room

#### Scenario: Grass tint
- **WHEN** a grass block panel covers the top face of a duct
- **THEN** the panel's top is tinted like the grass around it

### Requirement: Panels turn see-through while revealing
While a player wears Engineer Goggles or holds the Configuration Carrot, every panel the player's client has loaded SHALL render half transparent, so the duct behind it shows. When the player stops revealing, the panels SHALL render opaque again. The duct itself SHALL render unchanged.

#### Scenario: Carrot in hand
- **WHEN** a player takes the Configuration Carrot into the hand next to a wall of stone panels over ducts
- **THEN** the panels turn half transparent and the ducts behind them show; putting the carrot away makes the panels opaque again
