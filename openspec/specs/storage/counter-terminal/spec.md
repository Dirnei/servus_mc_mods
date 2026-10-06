# storage/counter-terminal Specification

## Purpose

The counter is where the player meets the warehouse: see everything, find it, take it, hand things in.

## Requirements

### Requirement: Stock view with search
The Counter Terminal GUI SHALL list every indexed item of its warehouse network with its total, largest first, in a scrollable grid. A search box SHALL filter the list by item name, case insensitive, as the player types.

#### Scenario: Search
- **WHEN** the warehouse holds cobblestone, iron ingots and bread and the player types "iron"
- **THEN** only iron ingots are shown

### Requirement: Pull to cursor or inventory
Clicking an entry SHALL pull up to one stack of that item onto the cursor; shift clicking SHALL pull up to one stack into the player's inventory. What does not fit stays in the warehouse. When the warehouse has shuttles with a home rail and the item sits in a block a rail reaches, the pull SHALL instead become a pick job served by a shuttle, and the stack SHALL arrive in the player's inventory when the shuttle returns. Pulls of items that only sit in blocks no rail reaches SHALL stay instant.

#### Scenario: Pull a stack
- **WHEN** the player clicks cobblestone with an empty cursor and the warehouse holds 50 and has no shuttles
- **THEN** 50 cobblestone are on the cursor and the warehouse shows none

#### Scenario: Pull into inventory
- **WHEN** the player shift clicks iron ingots and the warehouse holds 7 and has no shuttles
- **THEN** 7 iron ingots land in the inventory

#### Scenario: Pull served by a shuttle
- **WHEN** the player clicks cobblestone, the warehouse has a shuttle on a home rail and the cobblestone sits in a crate beside a rail
- **THEN** the cursor stays empty, a shuttle fetches the stack and it lands in the player's inventory on return

#### Scenario: Instant despite shuttles
- **WHEN** the player clicks dirt that only sits in a crate touching the controller directly, with no rail beside it
- **THEN** the dirt arrives at once

### Requirement: Push into the warehouse
Clicking the grid with a carried stack SHALL store it in the warehouse; shift clicking an inventory slot SHALL store that slot. Items that fit nowhere stay where they were.

#### Scenario: Shift click to store
- **WHEN** the player shift clicks 20 cobblestone in the inventory while an empty crate is on the network
- **THEN** the crate holds 20 cobblestone and the slot is empty

### Requirement: Live stock while open
The stock shown SHALL refresh within half a second after the index changes while the GUI is open, without resending when nothing changed.

#### Scenario: Machine output arrives
- **WHEN** a hopper puts 10 bread into a crate while the GUI is open
- **THEN** the bread count rises within a second

### Requirement: Offline shown
When the network is offline the GUI SHALL show the reason instead of the stock and SHALL refuse pulls and pushes.

#### Scenario: No controller
- **WHEN** the terminal touches crates but no controller
- **THEN** the GUI says the warehouse has no controller

### Requirement: Crafting
The Counter Terminal SHALL be crafted from glass, iron ingots, a chest and planks.

#### Scenario: Recipe
- **WHEN** the player crafts with the pattern
- **THEN** a Counter Terminal is produced
