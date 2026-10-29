# Spec Delta

## MODIFIED Requirements

### Requirement: Shuttle modules
A shuttle SHALL have one tool place, a Speed count, a Handling count and a Muffler place. The tool place holds at most one tool module, such as the Harvester; the Muffler place holds at most one Muffler, independent of the tool. The tool, the counts and the Muffler SHALL be stored on the Shuttle item, so a shuttle keeps them when it moves between inventories, garages and workshops. A Shuttle item's tooltip SHALL list its tool, counts and Muffler. A shuttle with no tool, no counts and no Muffler SHALL be the same item as a newly made shuttle of its tier and stack with it.

#### Scenario: Modules stay on the item
- **WHEN** a player installs a Speed upgrade into a shuttle, puts the shuttle into a garage and takes it out again
- **THEN** the shuttle still has the Speed upgrade and its tooltip says so

#### Scenario: Back to plain
- **WHEN** a player takes the only upgrade out of a Mk1 shuttle
- **THEN** the shuttle stacks with a newly crafted Mk1 shuttle

#### Scenario: Muffler next to a tool
- **WHEN** a shuttle with a Harvester gets a Muffler installed
- **THEN** it keeps the Harvester, has the Muffler and its tooltip lists both

#### Scenario: Muffler survives a build
- **WHEN** a Mk1 shuttle with a Muffler is built into a Mk2
- **THEN** the Mk2 shuttle has the Muffler

### Requirement: Shuttle Workshop block
The Shuttle Workshop SHALL be a craftable block without a facing. Right clicking it SHALL open a GUI with one shuttle slot, one install slot, a row of supply slots and the player inventory. The shuttle slot SHALL take one shuttle of any tier. The GUI SHALL show the shuttle's tier, its tool, its Speed and Handling counts with the tier's max, and whether it is muffled. The workshop SHALL keep the shuttle and the supplies when the GUI closes, and SHALL drop them when the block is broken. The install slot SHALL take nothing while the shuttle slot is empty. Sneaking with the Configuration Carrot SHALL dismantle the workshop into one item that carries its wood, the shuttle and the supplies; placing that item SHALL restore them. Dismantling SHALL cancel a hand build in progress.

#### Scenario: Open with a shuttle
- **WHEN** a player puts a Mk1 shuttle with one Speed upgrade into the shuttle slot
- **THEN** the GUI shows Mk1, Speed 1 of 2, Handling 0 of 2, no tool and not muffled

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
Putting an item into the install slot SHALL install it into the shuttle in the shuttle slot at once and the slot SHALL stay empty. A Speed or Handling upgrade SHALL add one count per item up to the tier's max; a stack SHALL install as many as fit and the rest SHALL stay with the player. A tool module SHALL replace the fitted tool, and the old tool SHALL go to the player. A Muffler SHALL go in only while the shuttle has none; one item of a stack goes in. Items that are none of these SHALL NOT go in. Each count, the tool and the Muffler SHALL have a button that takes one upgrade, the tool or the Muffler out of the shuttle and gives it to the player. Shift click SHALL move an upgrade, tool or Muffler from the inventory into the install slot, a shuttle into the shuttle slot, other items into the supply slots, and anything in the workshop back to the inventory.

#### Scenario: Install a stack
- **WHEN** a player puts five Speed upgrades into the install slot of a Mk1 shuttle with a max of 2
- **THEN** the shuttle has two Speed upgrades and the player holds three

#### Scenario: Swap a tool
- **WHEN** a shuttle has a Harvester and the player installs another tool module
- **THEN** the shuttle has the new tool and the Harvester is in the player's inventory

#### Scenario: Remove a count
- **WHEN** a player presses the remove button of Speed on a shuttle with two Speed upgrades
- **THEN** the shuttle has one Speed upgrade and the player gets one Speed Upgrade item

#### Scenario: Install a Muffler
- **WHEN** a player puts three Mufflers into the install slot of a shuttle without one
- **THEN** the shuttle is muffled and the player holds two Mufflers

#### Scenario: Second Muffler
- **WHEN** the shuttle already has a Muffler and the player puts another into the install slot
- **THEN** it does not go in

#### Scenario: Remove the Muffler
- **WHEN** a player presses the remove button of the Muffler
- **THEN** the shuttle is no longer muffled and the player gets the Muffler item

#### Scenario: Not an upgrade
- **WHEN** a player puts cobblestone into the install slot
- **THEN** it does not go in

## ADDED Requirements

### Requirement: The workshop shuttle turns
The large view of the shuttle in the workshop GUI SHALL turn slowly about its upright axis (example one turn in ten seconds) while no build by hand runs. Dragging the view with the mouse SHALL turn the shuttle by hand; after letting go it SHALL go on turning slowly from there. When a build by hand starts, the shuttle SHALL swing to the work angle in a short moment and hold still there until the build ends or is cancelled.

#### Scenario: Idle workshop
- **WHEN** a player opens the workshop with a shuttle and no build runs
- **THEN** the shuttle turns slowly in the large view

#### Scenario: Drag to look
- **WHEN** the player drags the large view to the left
- **THEN** the shuttle turns with the mouse and keeps turning slowly after the player lets go

#### Scenario: Build starts
- **WHEN** the player starts a build by hand while the shuttle shows its back
- **THEN** the shuttle swings to the work angle and the first step's spots sit on the shuttle
