# Spec Delta

## ADDED Requirements

### Requirement: Input row
The Storage Terminal SHALL have an Input row of 9 slots below the item grid. A stack placed into an input slot SHALL be stored into the warehouse right away along the push path (zones, crates and foreign inventories only); what does not fit SHALL stay in the slot and be tried again later. The input slots SHALL NOT be indexed as warehouse stock and SHALL be saved with the terminal and dropped when it breaks.

#### Scenario: Hand in cobblestone
- **WHEN** the player puts 20 cobblestone into an input slot while an empty crate is on the line
- **THEN** the crate holds 20 cobblestone and the input slot is empty

#### Scenario: No room
- **WHEN** the player puts bread into an input slot while every crate is full
- **THEN** the bread stays in the input slot

### Requirement: Automation inserts
A hopper, duct or packet inserting into the terminal SHALL put items an open request of the terminal still waits for into the output slots, and every other item into the input slots, which store it. Nothing SHALL be extracted from the terminal by automation.

#### Scenario: Hopper hands in
- **WHEN** a hopper above the terminal holds cobblestone and no request waits for cobblestone
- **THEN** the cobblestone goes through the input slots into the warehouse

#### Scenario: Delivery lands in the output
- **WHEN** a request waits for sticks and packets deliver sticks
- **THEN** the sticks land in the output slots

### Requirement: Crafting popup
A Craft button on the title row SHALL open a popup over the item grid with the 3x3 crafting grid, its arrow, result slot and clear button. While the popup is closed its slots SHALL NOT take clicks or show. Closing the popup SHALL store the grid contents like the clear button. JEI's transfer button SHALL open the popup before filling the grid.

#### Scenario: Open and craft
- **WHEN** the player presses Craft, puts a plank above a plank into the popup grid and takes the result
- **THEN** the player gets four sticks

#### Scenario: Close stores
- **WHEN** the popup grid holds two planks and the player closes the popup
- **THEN** the grid is empty and the warehouse holds two more planks

## MODIFIED Requirements

### Requirement: Push into the warehouse
Clicking the grid with a carried stack SHALL store it in the warehouse; shift clicking an inventory slot SHALL move that slot into the input row, which stores it. Pushes SHALL follow the zone routing but SHALL only go into crates and foreign inventories; racks are filled through a Inbound Station. Items that fit nowhere stay where they were or in the input row.

#### Scenario: Shift click to store
- **WHEN** the player shift clicks 20 cobblestone in the inventory while an empty crate is on the network
- **THEN** the crate holds 20 cobblestone and the slot is empty

#### Scenario: Only racks have room
- **WHEN** the player pushes bread while every crate is full and a rack has room
- **THEN** the bread stays with the player

### Requirement: Crafting
The Storage Terminal SHALL be crafted from glass, iron ingots, a chest and planks.

#### Scenario: Recipe
- **WHEN** the player crafts with the pattern
- **THEN** a Storage Terminal is produced

### Requirement: Output slots
The terminal SHALL have `terminal_output_slots` output slots (example 9, from `warehouse.json`, at most 9 shown) in one Output row below the Input row. Deliveries SHALL land there and the player SHALL take them out by hand or shift click. The output slots SHALL NOT be indexed as warehouse stock. Automation SHALL only insert into them as the automation inserts requirement says; nothing SHALL be extracted from them by hoppers or ducts. Breaking the terminal SHALL drop their contents.

#### Scenario: Collect
- **WHEN** packets have delivered 64 cobblestone into the output slots
- **THEN** the player shift clicks the slot and the cobblestone is in the inventory

#### Scenario: Not stock
- **WHEN** 64 cobblestone sit in the output slots
- **THEN** the stock view does not count them

#### Scenario: No extraction
- **WHEN** a hopper sits below a terminal holding cobblestone in its output slots
- **THEN** the hopper takes nothing

### Requirement: Screen layout
The Storage Terminal screen SHALL have a column of view buttons left of the panel, the title with a Craft button and the search box, the item grid nine wide with its scrollbar, a message line, the Input row, the Output row, and the player inventory with the hotbar. Each SHALL have its own area that no other element overlaps; the crafting popup lies over the item grid only. The grid SHALL show as many rows as the height setting asks for, small, medium, tall or auto (as many as fit the screen), but never more than fit the screen, and the panel SHALL fit a 1080p screen at GUI scales 2, 3 and 4.

#### Scenario: Nothing overlaps
- **WHEN** the screen is open with requests in the grid and a message shown
- **THEN** no label, button, grid cell or slot overlaps another area

#### Scenario: Grid has its own area
- **WHEN** the screen is open
- **THEN** the input row lies above the output row, both below the item grid, and the crafting popup covers only the item grid

#### Scenario: Tall on a small screen
- **WHEN** the height is tall at GUI scale 4 on a 1080p screen
- **THEN** the grid shows only as many rows as fit and the panel stays on the screen

### Requirement: Crafting grid
The crafting popup SHALL have a 3x3 crafting grid with a result slot. The result SHALL show the vanilla crafting recipe the grid matches, and taking it SHALL craft like a crafting table, using up one of each ingredient and leaving remainders such as empty buckets in the grid or the player inventory as vanilla does.

#### Scenario: Sticks by hand
- **WHEN** the player puts a plank above a plank into the grid and takes the result
- **THEN** the player gets four sticks and the planks are used up

### Requirement: Clear the grid
A clear button in the crafting popup SHALL move every grid stack into the warehouse; what does not fit SHALL go to the player inventory, and what still does not fit SHALL drop at the player.

#### Scenario: Clear
- **WHEN** the grid holds two planks and the player presses clear
- **THEN** the grid is empty and the warehouse holds two more planks

### Requirement: JEI fills the grid
With JEI installed, the transfer button on a vanilla crafting recipe while the Storage Terminal is open SHALL open the crafting popup, move the grid contents back to the warehouse and fill the grid with the recipe's ingredients, taking each from instant warehouse stock first and from the player inventory second. Ingredients found in neither SHALL leave their grid slot empty. With max transfer the grid SHALL be filled for as many crafts as fit in a stack.

#### Scenario: Transfer sticks
- **WHEN** a crate holds planks and the player presses JEI's transfer button on the stick recipe
- **THEN** the grid holds a plank above a plank taken from the crate and the result shows four sticks
