# logistics/utility-duct Specification

## Purpose

The Utility Duct is Logistics' one pipe: channels for items and energy in the same block, configured per side.

## Requirements

### Requirement: Channels are placed as items
An Item Channel or Energy Channel item SHALL place a Utility Duct carrying that channel, or add the channel to a duct that lacks it. A duct SHALL drop its channel items when broken or dismantled, and removing the last channel SHALL remove the duct.

#### Scenario: Add a second channel
- **WHEN** a player uses an Energy Channel on a duct that carries only an item channel
- **THEN** the duct carries both and one Energy Channel is consumed

#### Scenario: Broken duct
- **WHEN** a duct with both channels is broken
- **THEN** one Item Channel and one Energy Channel drop

### Requirement: Per side modes per channel
Each channel of a duct SHALL have one of the modes in and out, pull in, push out or disabled per side, default in and out, set through the duct GUI and kept with the block. A disabled side SHALL not connect visually or logically.

#### Scenario: Set a mode
- **WHEN** the player sets the item channel's west side to pull in
- **THEN** the duct keeps that mode after reload and the GUI shows it

### Requirement: Item channel behaviour
Items pushed into a duct on a side whose item mode allows input SHALL be routed round robin to inventories on sides whose item mode allows output, never back to the inserting inventory, within the per tick budget. Every `pull_interval` ticks each pull in side SHALL extract up to `items_per_pull` items from its inventory and route them.

#### Scenario: Pull from a chest into another
- **WHEN** chest A touches a duct's pull in side, chest B touches a push out side and chest A holds 20 cobblestone
- **THEN** within a few seconds chest B holds the 20 cobblestone

#### Scenario: Disabled side
- **WHEN** chest B's side is disabled
- **THEN** chest B receives nothing

### Requirement: Energy channel behaviour
Every tick an energy channel network SHALL move energy from storages on sides allowing input that can extract to storages on sides allowing output that can receive, water filling, up to `energy_per_tick`.

#### Scenario: Generator to machine through ducts
- **WHEN** a coal generator touches one end of three energy ducts and a powered machine the other
- **THEN** the machine's buffer fills and it works

### Requirement: Separate channel networks
Ducts SHALL connect per channel: an item channel network only spans ducts that carry the item channel, an energy channel network only ducts that carry the energy channel.

#### Scenario: Gap in a channel
- **WHEN** three ducts in a row carry items but only the outer two carry energy
- **THEN** items flow end to end and energy does not

### Requirement: Crafting
Item Channels SHALL be crafted from iron ingots, glass and a chest; Energy Channels from copper ingots, redstone and glass; eight each.

#### Scenario: Recipes
- **WHEN** the player crafts either pattern
- **THEN** eight channels are produced
