# storage/crate Specification

## Purpose

The crate is the warehouse's bulk shelf: one item type, lots of it, readable at a glance from the block face.

## Requirements

### Requirement: One type, bulk capacity
A crate SHALL hold a single item type up to its capacity (`capacity` in `data/<ns>/storage/crate.json`, default 4096). Once empty it SHALL accept any item type again.

#### Scenario: Fill
- **WHEN** 4096 cobblestone have been inserted into an empty crate
- **THEN** further cobblestone is refused and any other item is refused

#### Scenario: Emptied
- **WHEN** the last item is taken out
- **THEN** the crate accepts a different item type

### Requirement: Hand interaction
Right clicking a crate with items SHALL insert the held stack when it matches or the crate is empty. A second right click within half a second SHALL move every matching stack from the player's inventory into the crate. Left clicking SHALL take up to a full stack into the player's hand or inventory; sneak left click SHALL take one item.

#### Scenario: Insert and take
- **WHEN** a player right clicks an empty crate holding 30 cobblestone, then left clicks it
- **THEN** the crate takes the 30 cobblestone and gives back 30 on the left click

#### Scenario: Insert all
- **WHEN** a player double right clicks a cobblestone crate with cobblestone in several inventory slots
- **THEN** all those stacks move into the crate

### Requirement: Breaking rules
Left clicking a crate that holds items SHALL never start breaking it; it only hands out items. An empty crate SHALL break normally. Breaking spills nothing because the crate is empty; sneak wrench dismantling keeps the contents on the dropped item.

#### Scenario: Protected while full
- **WHEN** a survival or creative player holds left click on a crate with items
- **THEN** the crate only hands out items and takes no damage

#### Scenario: Dismantle
- **WHEN** a player sneak wrenches a crate holding 500 iron ingots and places it again
- **THEN** the new crate holds 500 iron ingots

### Requirement: Automation access
A crate SHALL expose an item handler on every side that inserts matching items up to the capacity and extracts in stacks of the item's maximum size.

#### Scenario: Hopper chain
- **WHEN** a hopper feeds cobblestone into a crate and another hopper draws from it
- **THEN** cobblestone flows through and the crate count follows

### Requirement: Front display
A crate SHALL show its item and count on its front face, updated within a second of a change, and SHALL show nothing when empty.

#### Scenario: Looking at a crate
- **WHEN** a crate holds 128 iron ingots
- **THEN** its front shows an iron ingot and the number 128
