## MODIFIED Requirements

### Requirement: Order grid
A Craft button with a hammer icon, right of the panel at the height of the first view button, SHALL open the order grid and SHALL be shown only while a formed Crafting CPU and a Crafting Station are on the terminal's data line. The order grid and the amount panel SHALL open as a popup over the terminal, wider than the item grid, as high as its content and centred on the terminal's base rows, with the rest of the terminal above the player inventory dimmed and taking no clicks; the player inventory and the hotbar SHALL stay usable. While the order grid is closed its cells SHALL NOT take clicks or show. Cancel, Escape and a started order SHALL clear the cells; Back and Next keep them, and so does closing the screen until it shows again.

#### Scenario: Open the order grid
- **WHEN** a formed CPU and a Crafting Station are on the line and the player presses Craft
- **THEN** the order grid opens with an empty grid and the player inventory stays usable

#### Scenario: No Craft button without a CPU
- **WHEN** the terminal's data line has a Crafting Station but no formed Crafting CPU
- **THEN** no Craft button shows

#### Scenario: Same popup at every height
- **WHEN** the grid height is tall and the player opens the order grid
- **THEN** the popup sits over the base rows with the same size as at the small height, and the rows above are dimmed

#### Scenario: Cancel clears the grid
- **WHEN** the grid holds a plank above a plank and the player presses Cancel, then opens the order grid again
- **THEN** the grid is empty

#### Scenario: Start clears the grid
- **WHEN** the player starts an order from the order grid and opens it again
- **THEN** the grid is empty

### Requirement: Craft dialog
Clicking a craftable item that is not in stock with any button, or Ctrl clicking or middle clicking a craftable item that is in stock, SHALL open the amount panel for that item. The amount panel SHALL show the title "Craft" with the item's name, the Foundation number stepper with +1, +10 and +stack above the amount and -1, -10 and -stack below it, starting at 1, and beside the amount the item to make with the count that gets crafted. Below SHALL be three scrollable item lists of the server's preview for that amount, computed with the order planner: From stock, To craft and Missing; Missing SHALL always show, empty when nothing is missing, and all three SHALL use the panel's dark text colour. Cancel SHALL be on the left and Start on the right. Start SHALL always show; it SHALL be greyed out, with the reason as its tooltip, while the preview is on its way, raw materials are missing or no Crafting CPU is on the line, and SHALL have no tooltip when it can start. The panel SHALL show no status line.

#### Scenario: Preview
- **WHEN** the player opens the craft dialog for 8 sticks and the warehouse holds 16 oak planks
- **THEN** From stock lists 4 oak planks, To craft lists 8 sticks, Missing is empty, and Start is enabled without a tooltip

#### Scenario: Missing material
- **WHEN** the warehouse holds no planks and no logs
- **THEN** Missing lists oak planks and Start is greyed out with a tooltip saying items are missing

#### Scenario: No CPU
- **WHEN** no formed Crafting CPU is on the line
- **THEN** Start is greyed out and its tooltip says there is no CPU

#### Scenario: Alternatives in the preview
- **WHEN** the player opens the craft dialog for 10 charcoal with a furnace adapter on the line and the warehouse holds 6 oak logs and 4 birch logs but no acacia
- **THEN** From stock lists 6 oak logs and 4 birch logs, To craft lists 10 charcoal and Missing is empty

#### Scenario: Craft entry on a plain click
- **WHEN** the grid shows sticks with "Craft" and the player left clicks them
- **THEN** the amount panel opens for sticks with the amount 1 and no request is made

#### Scenario: Crafted count beside the amount
- **WHEN** the amount panel for sticks shows the amount 5 and one craft makes 4 sticks
- **THEN** the stick beside the amount shows 8

#### Scenario: Ingredients crafted too
- **WHEN** the warehouse holds oak logs but no planks, a pattern for oak planks is on the line and the player opens the craft dialog for 8 sticks
- **THEN** From stock lists oak logs and To craft lists 8 sticks and oak planks

#### Scenario: Long preview scrolls
- **WHEN** the preview has more ingredients than fit in From stock
- **THEN** the list shows a scrollbar and the mouse wheel over it shows the rest

### Requirement: Screen layout
The Storage Terminal screen SHALL have a column of view buttons left of the panel, the Craft button right of the panel, the title and the search box, the item grid nine wide with its scrollbar, a message line, the Input row, the Output row, and the player inventory with the hotbar. Each SHALL have its own area that no other element overlaps; the dialogs lie over the item grid, the message line and the Input and Output rows only, the order grid and amount panel popups may reach past the panel's sides, and all of them leave the player inventory free. A small arrow beside the Input row SHALL point up, into the warehouse, and one beside the Output row SHALL point down, to the player. The grid SHALL show as many rows as the height setting asks for, small, medium, tall or auto (as many as fit the screen), but never more than fit the screen, and the panel SHALL fit a 1080p screen at GUI scales 2, 3 and 4. A stack the player carries SHALL be drawn above the grid, the dialogs, the order grid and the amount panel.

#### Scenario: Nothing overlaps
- **WHEN** the screen is open with requests on the Output row and a message shown
- **THEN** no label, button, grid cell, slot or request mark overlaps another area

#### Scenario: Grid has its own area
- **WHEN** the screen is open
- **THEN** the input row lies above the output row, both below the item grid, and the order grid leaves the player inventory uncovered

#### Scenario: Tall on a small screen
- **WHEN** the height is tall at GUI scale 4 on a 1080p screen
- **THEN** the grid shows only as many rows as fit and the panel stays on the screen

#### Scenario: Row arrows
- **WHEN** the screen is open
- **THEN** the arrow beside the Input row points up and the arrow beside the Output row points down

#### Scenario: Carried stack over a dialog
- **WHEN** the craft dialog is open and the player carries a stack from the inventory over it
- **THEN** the stack is drawn above the dialog

### Requirement: Order grid amount and preview
The order grid SHALL show no amount and no preview. Below it SHALL be Cancel on the left, Clear in the middle, which empties every cell, and Next on the right. Next SHALL open the amount panel of the craft dialog for the grid's result, with Back instead of Cancel; Back SHALL return to the order grid with its cells unchanged. Next SHALL be greyed out, with the reason as its tooltip, while the cells match no recipe. The amount panel's preview SHALL be computed for the grid's recipe and items, and its Start SHALL be greyed out, with the reason as its tooltip, also while no Crafting Station is on the line.

#### Scenario: Preview
- **WHEN** the grid holds a plank above a plank, the player presses Next, sets the amount to 8 and the warehouse holds 16 oak planks
- **THEN** From stock lists 4 oak planks, To craft lists 8 sticks and Start is enabled

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
The order grid SHALL have a 3x3 grid of ghost cells and, after an arrow, the result of the vanilla crafting recipe the cells match, drawn as a bare item icon without a slot frame, with the count one craft makes and the item's normal tooltip; the grid, arrow, result and Save button SHALL be centred in the panel. Clicking a cell with an item on the cursor SHALL set a single ghost copy of it, dragging the cursor's item over cells SHALL set a copy in each cell it passes, and clicking with an empty cursor SHALL clear the cell; the cursor and the player inventory keep their items. No real items SHALL lie in the order grid.

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

### Requirement: JEI fills the order grid
With JEI installed, the transfer button on a vanilla crafting recipe while the Storage Terminal is open SHALL open the order grid and set its ghost cells to the recipe's ingredients, choosing for each cell the accepted item the warehouse has the most of, else the first one. It SHALL take nothing from the player inventory or the warehouse. While no formed Crafting CPU and Crafting Station are on the terminal's data line the transfer button SHALL be refused with that reason. JEI's recipe view in front of the terminal SHALL NOT make the terminal forget what its line has.

#### Scenario: Transfer sticks
- **WHEN** the player presses JEI's transfer button on the stick recipe while the warehouse holds birch planks only
- **THEN** the grid shows a birch plank above a birch plank, the result shows four sticks and the inventory is unchanged

#### Scenario: No CPU
- **WHEN** the terminal's data line has no formed Crafting CPU and the player looks at the stick recipe in JEI
- **THEN** the transfer button is refused with a tooltip saying a Crafting CPU and a Crafting Station are needed

#### Scenario: Recipe view opened from the terminal
- **WHEN** a formed CPU and a Crafting Station are on the line, the player opens JEI's recipe view for sticks from the terminal and presses the transfer button
- **THEN** the button is not refused and the order grid opens with the recipe

### Requirement: Order grid start
Start in the amount panel opened from the order grid SHALL make a request of the terminal for the amount of the result, with a work order on a Crafting CPU of the line that crafts exactly the recipe and items of the grid, also when no pattern of the line has them, without asking. The ingredients SHALL come from the warehouse, the results SHALL land in the output slots like a craft request, and the request SHALL be listed and cancellable like any request. The order SHALL survive a reload with its recipe.

#### Scenario: One-off crafting table
- **WHEN** the grid holds four oak planks in a square, no pattern of the line makes a crafting table, the warehouse holds 16 oak planks and the player starts 1
- **THEN** a request for a crafting table is listed as producing, a Crafting Station crafts it from 4 planks of the warehouse, the crafting table lands in the output slots and no pattern is stored

#### Scenario: Restart during a one-off order
- **WHEN** a one-off crafting table order waits for its planks and the server restarts
- **THEN** the order goes on after the restart and the crafting table lands in the output slots

### Requirement: Save the order grid as a pattern
Beside the order grid's result SHALL be a Save button with the tooltip "Save as pattern", shown only while the grid matches a recipe and no pattern of the line has that recipe with the same items. Pressing it SHALL store the grid as a pattern like the Production Terminal's editor and SHALL NOT start an order; the button SHALL then go. A refused save SHALL keep the grid and the message line SHALL say why.

#### Scenario: Save as pattern
- **WHEN** the grid holds four oak planks in a square, no crafting table pattern exists, a Pattern Storage with room is on the line and the player presses Save
- **THEN** the Pattern Storage holds a crafting table pattern, no order starts and the Save button goes

#### Scenario: Only this once
- **WHEN** the player does not press Save and starts the same grid from the amount panel
- **THEN** the order starts and no pattern is stored

#### Scenario: Known pattern
- **WHEN** the grid holds a plank above a plank and the line has the sticks pattern
- **THEN** no Save button shows

#### Scenario: No room for the pattern
- **WHEN** every Pattern Storage of the line is full and the player presses Save
- **THEN** no pattern is stored, the grid stays and the message line says the Pattern Storage is full
