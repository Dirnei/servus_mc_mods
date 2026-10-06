# storage/crate Specification

## Purpose

The crate is the warehouse's bulk shelf: one item type, lots of it, readable at a glance from the block face.

## Requirements

### Requirement: One type, bulk capacity
A crate SHALL have one, two or four compartments. Each compartment SHALL hold a single item type up to the crate capacity (`capacity` in `data/<ns>/storage/crate.json`, default 4096) divided by the compartment count. Once a compartment is empty it SHALL accept any item type again.

#### Scenario: Fill
- **WHEN** 4096 cobblestone have been inserted into an empty one compartment crate
- **THEN** further cobblestone is refused and any other item is refused

#### Scenario: Emptied
- **WHEN** the last item is taken out of a compartment
- **THEN** that compartment accepts a different item type

#### Scenario: Quad crate compartment
- **WHEN** 1024 cobblestone have been inserted into one compartment of a quad crate
- **THEN** that compartment refuses more cobblestone while the other three still accept anything

### Requirement: Hand interaction
Right clicking a compartment of the front face SHALL insert the held stack into that compartment when it matches or is empty. A second right click within half a second SHALL move every matching stack from the player's inventory into that compartment. Left clicking a compartment SHALL take up to a full stack out of it into the player's hand or inventory; sneak left click SHALL take one item. On a one compartment crate the whole block counts as that compartment.

#### Scenario: Insert and take
- **WHEN** a player right clicks an empty crate holding 30 cobblestone, then left clicks it
- **THEN** the crate takes the 30 cobblestone and gives back 30 on the left click

#### Scenario: Insert all
- **WHEN** a player double right clicks a cobblestone compartment with cobblestone in several inventory slots
- **THEN** all those stacks move into that compartment

#### Scenario: Aim at a compartment
- **WHEN** a player holding iron ingots right clicks the lower right quarter of a quad crate's front
- **THEN** the ingots go into the lower right compartment and the other compartments are untouched

#### Scenario: Side face
- **WHEN** a player right clicks the side of a quad crate
- **THEN** the stack goes into the first compartment that matches or is empty

### Requirement: Breaking rules
Left clicking a crate that holds items in any compartment SHALL never start breaking it; it only hands out items. An empty crate SHALL break normally. Breaking spills nothing because the crate is empty; sneak wrench dismantling keeps the contents on the dropped item.

#### Scenario: Protected while full
- **WHEN** a survival or creative player holds left click on a crate with items
- **THEN** the crate only hands out items and takes no damage

#### Scenario: Dismantle
- **WHEN** a player sneak wrenches a crate holding 500 iron ingots and places it again
- **THEN** the new crate holds 500 iron ingots

### Requirement: Automation access
A crate SHALL expose an item handler on every side with one slot per compartment. Each slot inserts matching items up to the compartment capacity and extracts in stacks of the item's maximum size. Inserting without a slot preference fills the first compartment that matches, then the first empty one.

#### Scenario: Hopper chain
- **WHEN** a hopper feeds cobblestone into a crate and another hopper draws from it
- **THEN** cobblestone flows through and the crate count follows

#### Scenario: Two types into a double crate
- **WHEN** a hopper feeds cobblestone and then dirt into an empty double crate
- **THEN** cobblestone fills the first compartment and dirt the second

### Requirement: Front display
A crate SHALL show the item and count of every filled compartment on its front face at that compartment's position, updated within a second of a change, and SHALL show nothing for empty compartments.

#### Scenario: Looking at a crate
- **WHEN** a crate holds 128 iron ingots
- **THEN** its front shows an iron ingot and the number 128

#### Scenario: Looking at a quad crate
- **WHEN** a quad crate holds ingots in the upper left and dirt in the lower right compartment
- **THEN** its front shows the ingot top left and the dirt bottom right with their counts, and nothing in the other two quarters

### Requirement: Crate variants
Three crate blocks SHALL exist: Crate with one compartment, Double Crate with two compartments side by side, Quad Crate with four compartments in a two by two grid. The front face SHALL show the compartment divisions. All three SHALL be craftable from vanilla materials and the smaller crate.

#### Scenario: Variants in the tab
- **WHEN** the player opens the Servus creative tab
- **THEN** Crate, Double Crate and Quad Crate are listed in that order

#### Scenario: Crafting a quad crate
- **WHEN** the player combines a crate with two sticks (example recipe)
- **THEN** the result is one Quad Crate
