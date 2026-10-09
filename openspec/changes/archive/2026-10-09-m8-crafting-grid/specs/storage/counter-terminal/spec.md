# Spec Delta

## ADDED Requirements

### Requirement: Crafting grid
The Counter Terminal screen SHALL have a 3x3 crafting grid with a result slot. The result SHALL show the vanilla crafting recipe the grid matches, and taking it SHALL craft like a crafting table, using up one of each ingredient and leaving remainders such as empty buckets in the grid or the player inventory as vanilla does.

#### Scenario: Sticks by hand
- **WHEN** the player puts a plank above a plank into the grid and takes the result
- **THEN** the player gets four sticks and the planks are used up

### Requirement: Grid refills from the warehouse
After every craft, each grid slot that lost an ingredient SHALL be topped up with one of the same item from the instant stock of the warehouses the terminal reaches, crates and foreign inventories but not Heavy Duty Racks. Shift clicking the result SHALL craft again and again while the grid still matches and the player inventory has room.

#### Scenario: Shift craft from a crate
- **WHEN** the grid holds a plank above a plank, a crate on the line holds 6 planks and the player shift clicks the result
- **THEN** the player gets 16 sticks, the crate holds no planks and the grid is empty

#### Scenario: Rack stock is not used
- **WHEN** the only planks are in a Heavy Duty Rack and the player crafts the last sticks the grid can make
- **THEN** the grid stays empty and the rack keeps its planks

### Requirement: Pull a stack to the cursor
Alt clicking a stock entry with an empty cursor SHALL put up to one stack of that item from instant stock onto the cursor. With a carried stack or without instant stock nothing SHALL happen. Plain, shift, Ctrl and middle clicks on entries SHALL keep their request and craft meanings.

#### Scenario: Pull planks
- **WHEN** a crate holds 80 planks and the player Alt clicks the planks entry
- **THEN** the cursor holds 64 planks and the crate 16

### Requirement: Clear the grid
A clear button next to the grid SHALL move every grid stack into the warehouse; what does not fit SHALL go to the player inventory, and what still does not fit SHALL drop at the player.

#### Scenario: Clear
- **WHEN** the grid holds two planks and the player presses clear
- **THEN** the grid is empty and the warehouse holds two more planks

### Requirement: Grid empties on close
When the terminal screen closes, the grid contents SHALL go into the warehouse, and what does not fit into the player inventory.

#### Scenario: Close
- **WHEN** the grid holds a plank and the player closes the screen
- **THEN** the plank is in the warehouse

### Requirement: JEI fills the grid
With JEI installed, the transfer button on a vanilla crafting recipe while the Counter Terminal is open SHALL move the grid contents back to the warehouse and fill the grid with the recipe's ingredients, taking each from instant warehouse stock first and from the player inventory second. Ingredients found in neither SHALL leave their grid slot empty. With max transfer the grid SHALL be filled for as many crafts as fit in a stack.

#### Scenario: Transfer sticks
- **WHEN** a crate holds planks and the player presses JEI's transfer button on the stick recipe
- **THEN** the grid holds a plank above a plank taken from the crate and the result shows four sticks

## MODIFIED Requirements

### Requirement: Screen layout
The Counter Terminal screen SHALL give the search box and filter, the stock grid with its scrollbar, the request list, the output slots, the crafting grid with its result slot and clear button, and the player inventory each their own area that no other element overlaps, and the panel SHALL fit a 1080p screen at the automatic GUI scale.

#### Scenario: Nothing overlaps
- **WHEN** the screen is open with requests listed and a message shown
- **THEN** no label, list row or slot overlaps another area

#### Scenario: Grid has its own area
- **WHEN** the screen is open
- **THEN** the crafting grid, its result slot and the clear button overlap neither the output slots nor the request list
