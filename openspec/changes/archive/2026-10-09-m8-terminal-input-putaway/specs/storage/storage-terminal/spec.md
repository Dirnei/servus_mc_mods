# Spec Delta

## MODIFIED Requirements

### Requirement: Push into the warehouse
Clicking the grid with a carried stack SHALL move it into the Input row, and shift clicking an inventory slot SHALL move that slot into the Input row; the Input row stores it. What does not fit into the Input row SHALL stay on the cursor or in the inventory slot. Stored items reach racks, crates and foreign inventories through an Access Point like any intake.

#### Scenario: Shift click to store
- **WHEN** the player shift clicks 20 cobblestone in the inventory while an Access Point of the warehouse is on the line
- **THEN** the slot is empty and the warehouse holds 20 more cobblestone once they are put away

#### Scenario: Only racks have room
- **WHEN** the player pushes bread while every crate is full and a rack has room
- **THEN** a shuttle puts the bread into the rack

### Requirement: Input row
The Storage Terminal SHALL have an Input row of 9 slots below the item grid. Stacks in the Input row SHALL be sent as packets over the data line to the Access Point on the terminal's data line nearest to the terminal, which puts them away like any intake (racks by shuttle, zones, crates and foreign inventories). A stack SHALL leave its slot only when a packet takes it; items a packet cannot deliver SHALL come back to the terminal. Without an Access Point on the line the stacks SHALL stay and the message line SHALL say that no Access Point is on the data line. The input slots SHALL NOT be indexed as warehouse stock and SHALL be saved with the terminal and dropped when it breaks.

#### Scenario: Hand in cobblestone
- **WHEN** the player puts 20 cobblestone into an input slot while an Access Point of the warehouse is on the line
- **THEN** the input slot is empty and the cobblestone ends in the warehouse after the packets and the put-away

#### Scenario: No room
- **WHEN** the player puts bread into an input slot and no Access Point is on the data line
- **THEN** the bread stays in the input slot and the message line says "No Access Point on this data line"

#### Scenario: Racks only
- **WHEN** the warehouse has only Heavy Duty Racks with room and the player puts planks into an input slot
- **THEN** the planks end in a rack

### Requirement: Clear the grid
A clear button in the crafting popup SHALL move every grid stack into the Input row, which stores it; what does not fit SHALL go to the player inventory, and what still does not fit SHALL drop at the player.

#### Scenario: Clear
- **WHEN** the grid holds two planks and the player presses clear
- **THEN** the grid is empty and the two planks are stored through the Input row

### Requirement: Grid empties on close
When the terminal screen closes, the grid contents SHALL go into the Input row, which stores them, and what does not fit into the player inventory.

#### Scenario: Close
- **WHEN** the grid holds a plank and the player closes the screen
- **THEN** the plank is stored through the Input row

### Requirement: Screen layout
The Storage Terminal screen SHALL have a column of view buttons left of the panel, the title with a Craft button and the search box, the item grid nine wide with its scrollbar, a message line, the Input row, the Output row, and the player inventory with the hotbar. Each SHALL have its own area that no other element overlaps; the crafting popup lies over the item grid only. A small arrow beside the Input row SHALL point up, into the warehouse, and one beside the Output row SHALL point down, to the player. The grid SHALL show as many rows as the height setting asks for, small, medium, tall or auto (as many as fit the screen), but never more than fit the screen, and the panel SHALL fit a 1080p screen at GUI scales 2, 3 and 4.

#### Scenario: Nothing overlaps
- **WHEN** the screen is open with requests in the grid and a message shown
- **THEN** no label, button, grid cell or slot overlaps another area

#### Scenario: Grid has its own area
- **WHEN** the screen is open
- **THEN** the input row lies above the output row, both below the item grid, and the crafting popup covers only the item grid

#### Scenario: Tall on a small screen
- **WHEN** the height is tall at GUI scale 4 on a 1080p screen
- **THEN** the grid shows only as many rows as fit and the panel stays on the screen

#### Scenario: Row arrows
- **WHEN** the screen is open
- **THEN** the arrow beside the Input row points up and the arrow beside the Output row points down
