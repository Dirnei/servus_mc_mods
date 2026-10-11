## Purpose

The Shuttle Workshop is where players fit modules into shuttles, so that shuttles differ: a Cargo Hold lets a shuttle carry several stacks, and tool modules added by later changes make shuttles for field work.

## ADDED Requirements

### Requirement: Shuttle modules
A shuttle SHALL have two module slots: a tool slot and a utility slot. Each slot holds at most one module, and each module SHALL fit only the slot of its kind. The modules SHALL be stored on the Shuttle item, so a shuttle keeps them when it moves between inventories, garages and workshops. A Shuttle item's tooltip SHALL list its modules. A shuttle with no modules SHALL be the same item as a newly crafted shuttle and stack with it.

#### Scenario: Modules stay on the item
- **WHEN** a player fits a Cargo Hold into a shuttle, puts the shuttle into a garage and takes it out again
- **THEN** the shuttle still has the Cargo Hold and its tooltip says so

#### Scenario: Wrong slot
- **WHEN** a player tries to put a Cargo Hold into the tool slot
- **THEN** it does not go in

#### Scenario: Back to plain
- **WHEN** a player removes the only module of a shuttle
- **THEN** the shuttle stacks with a newly crafted shuttle

### Requirement: Shuttle Workshop block
The Shuttle Workshop SHALL be a craftable block. Right clicking it SHALL open a GUI with one shuttle slot, a tool slot, a utility slot and the player inventory. The shuttle slot SHALL take one Shuttle item. The module slots SHALL show the modules of the shuttle in the shuttle slot and SHALL be empty and closed while the shuttle slot is empty. The workshop SHALL keep no items: closing the GUI SHALL return the shuttle to the player.

#### Scenario: Open with a shuttle
- **WHEN** a player puts a shuttle with a Cargo Hold into the shuttle slot
- **THEN** the utility slot shows the Cargo Hold and the tool slot is empty

#### Scenario: No shuttle, no modules
- **WHEN** the shuttle slot is empty and the player tries to put a Cargo Hold into the utility slot
- **THEN** it does not go in

#### Scenario: One shuttle at a time
- **WHEN** a player shift clicks a stack of five shuttles into the workshop
- **THEN** one shuttle goes into the shuttle slot and four stay in the inventory

#### Scenario: Closing returns the shuttle
- **WHEN** a player closes the GUI with a shuttle in the shuttle slot
- **THEN** the shuttle is back in the player's inventory with its modules

### Requirement: Fitting and removing modules
Putting a module into a module slot of the workshop SHALL fit it into the shuttle in the shuttle slot at once. Taking a module out of a module slot SHALL remove it from the shuttle and give the module item to the player. Shift click SHALL move a module from the inventory into the free slot of its kind and a module or the shuttle back into the inventory.

#### Scenario: Fit a Cargo Hold
- **WHEN** a player puts a plain shuttle into the shuttle slot and a Cargo Hold into the utility slot, then takes the shuttle out
- **THEN** the player holds a shuttle with a Cargo Hold and the Cargo Hold item is gone from the inventory

#### Scenario: Remove a module
- **WHEN** a player takes the Cargo Hold out of the utility slot of a shuttle in the workshop
- **THEN** the player holds the Cargo Hold item and the shuttle has no module

### Requirement: Cargo Hold module
The Cargo Hold SHALL be a craftable utility module. A shuttle without a Cargo Hold SHALL carry one stack per trip. A shuttle with a Cargo Hold SHALL carry up to `cargo_hold_stacks` stacks per trip (in `data/<ns>/storage/warehouse.json`, example 4).

#### Scenario: Capacity from the data file
- **WHEN** `cargo_hold_stacks` is 4
- **THEN** a Cargo Hold shuttle takes up to four stacks on one trip and a plain shuttle one

### Requirement: Tool modules mark field work
A tool module SHALL be able to mark its shuttle as field only. A field only shuttle SHALL never take a warehouse pick or put-away. A shuttle with no tool module, or with a tool module that is not field only, SHALL take warehouse jobs. This change adds no tool module; the Harvester module added later is field only.

#### Scenario: No tool module
- **WHEN** a garage holds a shuttle with only a Cargo Hold
- **THEN** that shuttle takes warehouse picks and put-aways like a plain shuttle
