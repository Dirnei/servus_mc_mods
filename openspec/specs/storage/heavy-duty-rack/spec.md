# storage/heavy-duty-rack Specification

## Purpose

The Heavy Duty Rack is the warehouse's mixed storage bay: a chest sized inventory that joins neighbouring bays into one continuous rack and shows what it mostly holds.

## Requirements

### Requirement: Chest sized inventory with a GUI
A Heavy Duty Rack SHALL hold any items in a fixed number of slots (`slots` in `data/<ns>/storage/rack.json`, example 27). Right clicking it with an empty hand or a non tool item SHALL open a chest style GUI with the rack slots above the player inventory, with shift click moving stacks between them.

#### Scenario: Open and store
- **WHEN** a player right clicks a rack and shift clicks a stack of planks from the inventory
- **THEN** the planks are in a rack slot and the GUI shows them

#### Scenario: Full rack
- **WHEN** all slots hold items and a player shift clicks another stack
- **THEN** the stack stays in the player's inventory

### Requirement: Automation access
A rack SHALL expose an item handler on every side covering all of its slots.

#### Scenario: Hopper into a rack
- **WHEN** a hopper feeds cobblestone into a rack
- **THEN** the cobblestone appears in the rack's slots and in the warehouse index

### Requirement: Bays connect visually
Racks with the same facing that stand in a row SHALL form sections of up to `bays_per_section` bays (`rack.json`, example 3). A bay joins the section beside it on placement only while that section has room; otherwise it starts a new section. Joins SHALL be decided when a bay is placed and SHALL NOT move when further bays are placed anywhere. A bay placed on top of a row SHALL copy the section boundaries of the row below wherever a rack stands below it or below its neighbour. Within a section the shared posts disappear and the floor board, the middle shelf and the beams run through; posts stand at section ends. A rack directly above or below another rack SHALL continue the posts without a top or bottom frame between them. A rack placed directly on top of another rack SHALL take that rack's facing. Each rack SHALL keep its own inventory. The frame SHALL render without flickering faces.

#### Scenario: Row of three
- **WHEN** three racks facing the same way are placed one after another in a row
- **THEN** only the two outer edges show posts and the shelves and beams run along all three

#### Scenario: Row of five
- **WHEN** five racks are placed one after another from left to right
- **THEN** the first three form a section, the fourth and fifth form a second section, and a post stands between the third and the fourth

#### Scenario: Fourth bay stands alone
- **WHEN** a fourth rack is placed at either end of a row of three
- **THEN** the three keep their joins and the fourth shows posts on both sides

#### Scenario: Bay removed from a row
- **WHEN** the middle bay of a section of three is broken
- **THEN** the two remaining bays each show posts towards the gap and nothing else changes

#### Scenario: Shifted upper row
- **WHEN** a row of three and a row of two stand on the floor and a second row is placed on top, shifted by one bay
- **THEN** the upper row splits above the same post as the lower row

#### Scenario: Different facing
- **WHEN** two racks stand side by side but face different directions
- **THEN** each renders as its own bay

#### Scenario: Separate inventories
- **WHEN** a player fills the left rack of a row
- **THEN** the right rack's GUI stays empty

#### Scenario: Placed on top
- **WHEN** a player looking south places a rack on top of a rack that faces east
- **THEN** the new rack faces east and the two render as one stacked bay

### Requirement: Sample display
A rack SHALL show the item types it holds the most of, up to a display count (`display_count` in `rack.json`, example 4), as item models standing on its shelf boards inside the frame, two per shelf level, without counts, updated within a second of a change. Ties SHALL be broken by slot order. An empty rack SHALL show nothing.

#### Scenario: Five types, four shown
- **WHEN** a rack holds 64 cobblestone, 32 dirt, 16 sand, 8 gravel and 1 apple
- **THEN** the shelves show cobblestone, dirt, sand and gravel

#### Scenario: Counts change
- **WHEN** 100 apples are added to that rack
- **THEN** within a second the shelves show apples instead of gravel

#### Scenario: Standing on the shelf
- **WHEN** a rack holds cobblestone and an iron ingot
- **THEN** a small cobblestone cube and an upright ingot stand on the lower shelf between the posts, visible from the aisle and from the sides

### Requirement: Rack is a storage block
A rack SHALL be a stock holding node of the warehouse network: its contents are indexed, it joins through touching storage blocks on any side and through rails on its front side only, and rails dock onto its front only. A rack SHALL NOT carry a connection lamp. Rotating a rack SHALL refresh which rails it links to.

#### Scenario: Rack beside a rail
- **WHEN** a rack is placed with a rail of an online warehouse in front of it
- **THEN** the rail shows a dock towards it and the terminal lists its contents

#### Scenario: Rail at the side
- **WHEN** a rail of an online warehouse runs along the side or the back of a rack and nothing else touches it
- **THEN** the rail shows no dock, and the rack's contents are not listed

### Requirement: Rack is obtainable and dismantled
The rack SHALL be craftable from vanilla materials, SHALL spill its contents when broken, and SHALL keep them on the dropped item when dismantled by sneaking with the Configuration Carrot.

#### Scenario: Crafting
- **WHEN** the player places iron ingots and planks in the rack pattern (for example ingots in the corners and planks across the middle)
- **THEN** the result is one Heavy Duty Rack

#### Scenario: Dismantle
- **WHEN** a player sneak dismantles a rack with two stacks and places it again
- **THEN** the new rack holds the two stacks
