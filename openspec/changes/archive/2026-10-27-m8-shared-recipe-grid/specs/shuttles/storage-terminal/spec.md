# Spec Delta

## ADDED Requirements

### Requirement: Exact or Any in the order grid
The order grid SHALL have the Exact / Any button of the recipe editor, with the same labels, greying and tooltips. It SHALL start as Any when Any is possible for the grid, and as Exact otherwise. Next and Back SHALL keep the chosen mode; Cancel, Escape and a started order SHALL reset it, so the grid opened empty starts as Any again.

#### Scenario: Sticks start as Any
- **WHEN** the player opens the order grid and puts a plank above a plank
- **THEN** the button shows Any, and pressing it switches to Exact

#### Scenario: Rack stays Exact
- **WHEN** the order grid holds a Heavy Duty Rack recipe
- **THEN** the button shows Exact, is greyed out and its tooltip says the result changes with the planks

#### Scenario: Next and Back keep the mode
- **WHEN** the grid holds sticks set to Exact and the player presses Next and then Back
- **THEN** the button still shows Exact

## MODIFIED Requirements

### Requirement: Order grid amount and preview
The order grid SHALL show no amount and no preview. Below it SHALL be Cancel on the left, Clear in the middle, which empties every cell, and Next on the right. Next SHALL open the amount panel of the craft dialog for the grid's result, with Back instead of Cancel; Back SHALL return to the order grid with its cells and mode unchanged. Next SHALL be greyed out, with the reason as its tooltip, while the cells match no recipe. The amount panel's preview SHALL be computed for the grid's recipe, items and mode, and its Start SHALL be greyed out, with the reason as its tooltip, also while no Crafting Station is on the line.

#### Scenario: Preview
- **WHEN** the grid holds a plank above a plank, the player presses Next, sets the amount to 8 and the warehouse holds 16 oak planks
- **THEN** From stock lists 4 oak planks, To craft lists 8 sticks and Start is enabled

#### Scenario: Any preview takes the stocked planks
- **WHEN** the grid holds an oak plank above an oak plank set to Any, the warehouse holds 40 spruce planks and no oak planks, and the player presses Next and sets the amount to 8
- **THEN** From stock lists 4 spruce planks, To craft lists 8 sticks, Missing is empty and Start is enabled

#### Scenario: Exact preview keeps the grid's planks
- **WHEN** the same grid is set to Exact, the warehouse holds 40 spruce planks, no oak planks and no oak logs
- **THEN** Missing lists oak planks and Start is greyed out

#### Scenario: Nothing matches
- **WHEN** the grid holds a single stick
- **THEN** no result shows and Next is greyed out with the reason that the grid matches no recipe

#### Scenario: Clear
- **WHEN** the grid holds four planks and the player presses Clear
- **THEN** every cell is empty, no result shows and the order grid stays open

#### Scenario: Back keeps the grid
- **WHEN** the player presses Next for the stick recipe and then Back
- **THEN** the order grid shows a plank above a plank again

### Requirement: Order grid cells
The order grid SHALL be the shared recipe grid view: a 3x3 grid of ghost cells and, after an arrow, the result of the vanilla crafting recipe the cells match, drawn as a bare item icon without a slot frame, with the count one craft makes and the item's normal tooltip, the Exact / Any button and beside the result the Save button. The result SHALL be what the recipe makes from the cells, with the components the recipe adds (for example the wood of the planks), the same stack a saved pattern has. Clicking a cell with an item on the cursor SHALL set a single ghost copy of it, dragging the cursor's item over cells SHALL set a copy in each cell it passes, and clicking with an empty cursor SHALL clear the cell; the cursor and the player inventory keep their items. No real items SHALL lie in the order grid.

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
With JEI installed, the transfer button on a vanilla crafting recipe while the Storage Terminal is open SHALL open the order grid and set its ghost cells to the recipe's ingredients, the same way it fills the Production Terminal's recipe editor. For each cell it SHALL choose, among the stacks the warehouse can give out that the cell's ingredient accepts, the one the warehouse has the most of, with that stack's components (for example a Rail with its wood); only when the warehouse has none of them SHALL the cell take the ingredient's first item. A transfer that opens the order grid SHALL start it as Any when Any is possible; a transfer into the open order grid SHALL keep its mode. It SHALL take nothing from the player inventory or the warehouse. While no formed Crafting CPU and Crafting Station are on the terminal's data line the transfer button SHALL be refused with that reason. JEI's recipe view in front of the terminal SHALL NOT make the terminal forget what its line has.

#### Scenario: Transfer sticks
- **WHEN** the player presses JEI's transfer button on the stick recipe while the warehouse holds birch planks only
- **THEN** the grid shows a birch plank above a birch plank, the result shows four sticks, the mode shows Any and the inventory is unchanged

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

### Requirement: Order grid start
Start in the amount panel opened from the order grid SHALL make a request of the terminal for the amount of the result, with a work order on a Crafting CPU of the line that crafts exactly the recipe of the grid, also when no pattern of the line has it, without asking. With Exact the order SHALL craft with the grid's items; with Any it SHALL craft, for each ingredient, with the accepted item the warehouse holds most of after earlier reservations, as an Any pattern does. The ingredients SHALL come from the warehouse, the results SHALL land in the output slots like a craft request, and the request SHALL be listed and cancellable like any request. The order SHALL survive a reload with its recipe, items and mode.

#### Scenario: One-off crafting table
- **WHEN** the grid holds four oak planks in a square set to Exact, no pattern of the line makes a crafting table, the warehouse holds 16 oak planks and the player starts 1
- **THEN** a request for a crafting table is listed as producing, a Crafting Station crafts it from 4 planks of the warehouse, the crafting table lands in the output slots and no pattern is stored

#### Scenario: One-off Any sticks use spruce
- **WHEN** the grid holds an oak plank above an oak plank set to Any, no sticks pattern exists, the warehouse holds 3 oak planks and 40 spruce planks and the player starts 16 sticks
- **THEN** the Crafting Station crafts the sticks from 8 spruce planks, the 3 oak planks stay in the warehouse and 16 sticks land in the output slots

#### Scenario: One-off Exact sticks wait for oak
- **WHEN** the same grid is set to Exact, the warehouse holds 40 spruce planks, no oak planks and no oak logs
- **THEN** Start is greyed out and Missing lists oak planks

#### Scenario: Restart during a one-off order
- **WHEN** a one-off Any sticks order has chosen spruce planks and waits for them, and the server restarts
- **THEN** the order goes on after the restart with spruce planks and the sticks land in the output slots

### Requirement: Save the order grid as a pattern
Beside the order grid's result SHALL be a Save button with the tooltip "Save as pattern", shown only while the grid matches a recipe and the line has no pattern this one would duplicate with the chosen mode: the same recipe with the same items, or for Any an Any pattern of the same recipe and result. Pressing it SHALL store the grid as a pattern with the chosen mode, like the Production Terminal's editor, and SHALL NOT start an order; the button SHALL then go. A refused save SHALL keep the grid and the message line SHALL say why.

#### Scenario: Save as pattern
- **WHEN** the grid holds four oak planks in a square, no crafting table pattern exists, a Pattern Storage with room is on the line and the player presses Save
- **THEN** the Pattern Storage holds a crafting table pattern set to Any, no order starts and the Save button goes

#### Scenario: Saved as Exact
- **WHEN** the grid holds a plank above a plank, the player sets the mode to Exact and presses Save
- **THEN** the stored sticks pattern is Exact and the Production Terminal's Patterns tab shows it without the Any mark

#### Scenario: Only this once
- **WHEN** the player does not press Save and starts the same grid from the amount panel
- **THEN** the order starts and no pattern is stored

#### Scenario: Known pattern
- **WHEN** the grid holds a plank above a plank and the line has the Exact oak sticks pattern, and the mode is Exact
- **THEN** no Save button shows

#### Scenario: Any sticks already saved
- **WHEN** the line has an Any sticks pattern saved with oak planks and the grid holds a birch plank above a birch plank set to Any
- **THEN** no Save button shows; switching the mode to Exact shows it

#### Scenario: No room for the pattern
- **WHEN** every Pattern Storage of the line is full and the player presses Save
- **THEN** no pattern is stored, the grid stays and the message line says the Pattern Storage is full
