## MODIFIED Requirements

### Requirement: Order grid cells
The order grid SHALL have a 3x3 grid of ghost cells and, after an arrow, the result of the vanilla crafting recipe the cells match, drawn as a bare item icon without a slot frame, with the count one craft makes and the item's normal tooltip; the grid, arrow, result and Save button SHALL be centred in the panel. The result SHALL be what the recipe makes from the cells, with the components the recipe adds (for example the wood of the planks), the same stack the amount panel's preview and a saved pattern have. Clicking a cell with an item on the cursor SHALL set a single ghost copy of it, dragging the cursor's item over cells SHALL set a copy in each cell it passes, and clicking with an empty cursor SHALL clear the cell; the cursor and the player inventory keep their items. No real items SHALL lie in the order grid.

#### Scenario: Sticks by ghost
- **WHEN** the player clicks the top middle and the centre cell with oak planks on the cursor
- **THEN** both cells show a plank, the result shows four sticks and the player still holds every plank

#### Scenario: Drag over cells
- **WHEN** the player holds oak planks and drags with the mouse button down over the top three cells
- **THEN** all three cells show a plank and the player still holds every plank

#### Scenario: Clear a cell
- **WHEN** a cell shows a plank and the player clicks it with an empty cursor
- **THEN** the cell is empty

#### Scenario: Result is no slot
- **WHEN** the result shows four sticks and the player clicks it
- **THEN** nothing is taken and no item is put on the cursor

#### Scenario: Rack with its wood
- **WHEN** the grid holds iron in the four corners and three spruce planks across the middle row, no rack pattern exists and a Pattern Storage with room is on the line
- **THEN** the result shows three Heavy Duty Racks whose tooltip names spruce wood, and the Save button shows

#### Scenario: Rack can be started
- **WHEN** that grid is in the order grid, the warehouse holds iron and spruce planks, and the player presses Next and sets the amount to 3
- **THEN** From stock lists 4 iron ingots and 3 spruce planks, To craft lists 3 spruce Heavy Duty Racks and Start is enabled

### Requirement: JEI fills the order grid
With JEI installed, the transfer button on a vanilla crafting recipe while the Storage Terminal is open SHALL open the order grid and set its ghost cells to the recipe's ingredients. For each cell it SHALL choose, among the stacks the warehouse can give out that the cell's ingredient accepts, the one the warehouse has the most of, with that stack's components (for example a Rail with its wood); only when the warehouse has none of them SHALL the cell take the ingredient's first item. It SHALL take nothing from the player inventory or the warehouse. While no formed Crafting CPU and Crafting Station are on the terminal's data line the transfer button SHALL be refused with that reason. JEI's recipe view in front of the terminal SHALL NOT make the terminal forget what its line has.

#### Scenario: Transfer sticks
- **WHEN** the player presses JEI's transfer button on the stick recipe while the warehouse holds birch planks only
- **THEN** the grid shows a birch plank above a birch plank, the result shows four sticks and the inventory is unchanged

#### Scenario: Stocked item with its wood
- **WHEN** the warehouse holds 40 oak Rails and 10 spruce Rails and the player presses the transfer button on the Elevator recipe
- **THEN** the centre cell shows an oak Rail with its wood, the result shows oak Elevators and the amount panel takes the Rail from stock

#### Scenario: Nothing stocked
- **WHEN** the warehouse holds no planks and the player presses the transfer button on the stick recipe
- **THEN** each plank cell shows the first plank the recipe accepts

#### Scenario: No CPU
- **WHEN** the terminal's data line has no formed Crafting CPU and the player looks at the stick recipe in JEI
- **THEN** the transfer button is refused with a tooltip saying a Crafting CPU and a Crafting Station are needed

#### Scenario: Recipe view opened from the terminal
- **WHEN** a formed CPU and a Crafting Station are on the line, the player opens JEI's recipe view for sticks from the terminal and presses the transfer button
- **THEN** the button is not refused and the order grid opens with the recipe
