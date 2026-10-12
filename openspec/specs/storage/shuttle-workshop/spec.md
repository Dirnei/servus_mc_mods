# storage/shuttle-workshop Specification

## Purpose

The Shuttle Workshop is where players fit modules into shuttles, so that shuttles differ: a Cargo Hold lets a shuttle carry several stacks, and tool modules added by later changes make shuttles for field work.

## Requirements

### Requirement: Shuttle modules
A shuttle SHALL have one tool place, a Speed count and a Handling count. The tool place holds at most one tool module, such as the Harvester. The tool and the counts SHALL be stored on the Shuttle item, so a shuttle keeps them when it moves between inventories, garages and workshops. A Shuttle item's tooltip SHALL list its tool and counts. A shuttle with no tool and no counts SHALL be the same item as a newly made shuttle of its tier and stack with it.

#### Scenario: Modules stay on the item
- **WHEN** a player installs a Speed upgrade into a shuttle, puts the shuttle into a garage and takes it out again
- **THEN** the shuttle still has the Speed upgrade and its tooltip says so

#### Scenario: Back to plain
- **WHEN** a player takes the only upgrade out of a Mk1 shuttle
- **THEN** the shuttle stacks with a newly crafted Mk1 shuttle

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

### Requirement: Fitting and removing modules
Putting an item into the install slot SHALL install it into the shuttle in the shuttle slot at once and the slot SHALL stay empty. A Speed or Handling upgrade SHALL add one count per item up to the tier's max; a stack SHALL install as many as fit and the rest SHALL stay with the player. A tool module SHALL replace the fitted tool, and the old tool SHALL go to the player. Items that are neither SHALL NOT go in. Each count and the tool SHALL have a button that takes one upgrade or the tool out of the shuttle and gives it to the player. Shift click SHALL move an upgrade or tool from the inventory into the install slot, a shuttle into the shuttle slot, other items into the supply slots, and anything in the workshop back to the inventory.

#### Scenario: Install a stack
- **WHEN** a player puts five Speed upgrades into the install slot of a Mk1 shuttle with a max of 2
- **THEN** the shuttle has two Speed upgrades and the player holds three

#### Scenario: Swap a tool
- **WHEN** a shuttle has a Harvester and the player installs another tool module
- **THEN** the shuttle has the new tool and the Harvester is in the player's inventory

#### Scenario: Remove a count
- **WHEN** a player presses the remove button of Speed on a shuttle with two Speed upgrades
- **THEN** the shuttle has one Speed upgrade and the player gets one Speed Upgrade item

#### Scenario: Not an upgrade
- **WHEN** a player puts cobblestone into the install slot
- **THEN** it does not go in

### Requirement: Tool modules mark field work
A tool module SHALL be able to mark its shuttle as field only. A field only shuttle SHALL never take a warehouse pick or put-away. A shuttle with no tool module, or with a tool module that is not field only, SHALL take warehouse jobs. The Harvester and the Spreader are tool modules and are field only; a field only shuttle SHALL take only the field jobs of its own tool.

#### Scenario: No tool module
- **WHEN** a garage holds a shuttle with only Speed upgrades
- **THEN** that shuttle takes warehouse picks and put-aways like any shuttle

#### Scenario: Harvester shuttle
- **WHEN** a garage holds a shuttle with a Harvester and two Speed upgrades
- **THEN** that shuttle takes no warehouse pick or put-away

#### Scenario: Spreader shuttle
- **WHEN** a garage holds a shuttle with a Spreader
- **THEN** that shuttle takes no warehouse pick or put-away and no harvest run

### Requirement: Workshop look
The workshop SHALL look like the other wood blocks and still stand apart as a block that is not on the warehouse network. All four sides SHALL show the dark iron frame over board panels in its wood, without the iron crossbar of the stations, with a wrench and a yellow-handled screwdriver hanging on the boards. The top SHALL show a steel bench plate with an engraved shuttle outline and a vise at its edge; the bottom SHALL show the plain frame over boards. The workshop SHALL show no connection lamp, no dark recessed port and no teal. The item SHALL look like the block.

#### Scenario: Next to the garage
- **WHEN** a player places an oak workshop beside a Shuttle Garage
- **THEN** both show the dark frame over oak boards, the garage shows its doorway and lamp, and the workshop shows the hanging tools and no lamp

#### Scenario: Wood shows on the boards
- **WHEN** a player places a workshop crafted with dark oak planks
- **THEN** its sides, top and bottom show dark oak boards in the frame
