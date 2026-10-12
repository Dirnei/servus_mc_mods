## MODIFIED Requirements

### Requirement: Shuttle Workshop block
The Shuttle Workshop SHALL be a craftable block without a facing. Right clicking it SHALL open a GUI with one shuttle slot, one install slot, a row of supply slots and the player inventory. The shuttle slot SHALL take one shuttle of any tier. The GUI SHALL show the shuttle's tier, its tool and its Speed and Handling counts with the tier's max. The workshop SHALL keep the shuttle and the supplies when the GUI closes, and SHALL drop them when the block is broken. The install slot SHALL take nothing while the shuttle slot is empty. Sneaking with the Configuration Carrot SHALL dismantle the workshop into one item that carries its wood, the shuttle and the supplies; placing that item SHALL restore them. Dismantling SHALL cancel a hand build in progress.

#### Scenario: Open with a shuttle
- **WHEN** a player puts a Mk1 shuttle with one Speed upgrade into the shuttle slot
- **THEN** the GUI shows Mk1, Speed 1 of 2, Handling 0 of 2 and no tool

#### Scenario: No shuttle, no install
- **WHEN** the shuttle slot is empty and the player tries to put a Speed upgrade into the install slot
- **THEN** it does not go in

#### Scenario: One shuttle at a time
- **WHEN** a player shift clicks a stack of five shuttles into the workshop
- **THEN** one shuttle goes into the shuttle slot and four stay in the inventory

#### Scenario: The workshop keeps its contents
- **WHEN** a player closes the GUI with a shuttle and supplies in the workshop and opens it again
- **THEN** the shuttle and the supplies are still there

#### Scenario: Breaking drops the contents
- **WHEN** a player breaks a workshop holding a shuttle and supplies
- **THEN** the shuttle, with its upgrades, and the supplies drop as items

#### Scenario: Dismantled with the carrot
- **WHEN** a player sneaks and uses the Configuration Carrot on a spruce workshop holding a Mk2 shuttle with one Speed upgrade and 8 iron ingots as supplies
- **THEN** one spruce workshop item drops, nothing else drops, and placing it again shows the Mk2 shuttle with Speed 1 and the 8 iron ingots in the GUI

#### Scenario: Hand build cancelled
- **WHEN** a hand build is at step 2 and the workshop is dismantled with the carrot and placed again
- **THEN** no hand build is running and the shuttle keeps its tier

#### Scenario: Plain carrot click
- **WHEN** a player uses the carrot on the workshop without sneaking
- **THEN** nothing changes

## ADDED Requirements

### Requirement: Workshop look
The workshop SHALL look like the other wood blocks and still stand apart as a block that is not on the warehouse network. All four sides SHALL show the dark iron frame over board panels in its wood, without the iron crossbar of the stations, with a wrench and a yellow-handled screwdriver hanging on the boards. The top SHALL show a steel bench plate with an engraved shuttle outline and a vise at its edge; the bottom SHALL show the plain frame over boards. The workshop SHALL show no connection lamp, no dark recessed port and no teal. The item SHALL look like the block.

#### Scenario: Next to the garage
- **WHEN** a player places an oak workshop beside a Shuttle Garage
- **THEN** both show the dark frame over oak boards, the garage shows its doorway and lamp, and the workshop shows the hanging tools and no lamp

#### Scenario: Wood shows on the boards
- **WHEN** a player places a workshop crafted with dark oak planks
- **THEN** its sides, top and bottom show dark oak boards in the frame
