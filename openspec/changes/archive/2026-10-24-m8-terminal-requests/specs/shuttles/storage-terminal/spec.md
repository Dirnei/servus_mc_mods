## MODIFIED Requirements

### Requirement: Stock view with search
The Storage Terminal GUI SHALL list every indexed item of the warehouses it reaches over its data line with its requestable count in a scrollable grid, sorted by the player's sort setting (name, amount or mod, ascending or descending; amount descending by default). The requestable count SHALL be the stock in the racks less the items shuttle picks have locked; items on their way into storage SHALL count once they are in a rack. An item with nothing left to request SHALL NOT be listed as stock. A search box SHALL filter the list by item name, case insensitive, as the player types.

#### Scenario: Search
- **WHEN** the warehouse holds cobblestone, iron ingots and bread and the player types "iron"
- **THEN** only iron ingots are shown

#### Scenario: Sort by name
- **WHEN** the warehouse holds 50 cobblestone, 10 apples and 20 bread and the sort is name ascending
- **THEN** the grid shows apple, bread, cobblestone in that order

#### Scenario: Sort by amount
- **WHEN** the same stock is sorted by amount descending
- **THEN** the grid shows cobblestone, bread, apple

#### Scenario: Request lowers the count
- **WHEN** the warehouse holds 50 cobblestone and the player requests 20
- **THEN** the grid shows 30 cobblestone within half a second, before the shuttle has picked them

#### Scenario: Last item requested
- **WHEN** the warehouse holds one configuration item and the player requests it
- **THEN** the configuration item leaves the stock entries while the shuttle fetches it

#### Scenario: Fully reserved but craftable
- **WHEN** every stick in stock is locked by picks and a sticks pattern, a Crafting Station and a CPU are on the line
- **THEN** sticks show as a craftable entry with "Craft" instead of a count

### Requirement: Live stock while open
The terminal SHALL send its full content (stock, craftable items, requests, rules) when the game opens the screen, without waiting for the periodic update. The client SHALL keep the last content of each terminal for the session and SHALL show it at once when that terminal is opened again, replacing it when the fresh content arrives; the screen SHALL show a loading hint only while it has no content for that terminal yet. While the GUI is open the stock shown SHALL refresh within half a second after the index changes or a pick locks or releases items, without resending when nothing changed.

#### Scenario: Machine output arrives
- **WHEN** a machine's output reaches an Inbound Station and a shuttle puts 10 bread from it into a rack while the GUI is open
- **THEN** the bread count rises within a second

#### Scenario: Instant content
- **WHEN** the player opens the terminal
- **THEN** the stock is sent in the same tick the screen opens

#### Scenario: Opened again
- **WHEN** the player closes the terminal and opens it again in the same session
- **THEN** the last stock shows at once without a loading hint and is replaced by the fresh content

#### Scenario: Cancelled request
- **WHEN** the player cancels a request whose pick had locked 20 cobblestone that still lie in the rack
- **THEN** the 20 cobblestone count again within half a second

### Requirement: Request items
Clicking an entry SHALL open an amount prompt with buttons for -stack, -10, -1, +1, +10 and +stack and a text field, and confirming SHALL create a request for that amount. +stack SHALL raise the amount to the next full stack above it and -stack SHALL lower it to the full stack below it; the amount SHALL stay at least 1 and at most 99999. The prompt SHALL start at one stack, or at the requestable count when that is smaller, and SHALL NOT go above the requestable count unless the rest can be crafted. Shift clicking an entry SHALL request one stack at once, or all that is requestable when that is less than one stack. A request SHALL be shipped through the Packing Station with the shortest shuttle trip that shares the terminal's data line: shuttles pick the items from the racks into that Packing Station, and they travel on as packets into the output slots. A request SHALL be refused with the reason when no Packing Station shares the line, when no shuttle can reach the racks holding the item (needs a shuttle), when the Packing Station touches no rail, or when nothing of the item is requestable and it cannot be crafted (out of stock).

#### Scenario: Request a stack
- **WHEN** the player shift clicks cobblestone, the warehouse holds 50 in a rack on the rails and has a garage with a shuttle
- **THEN** a request for 50 is made, a shuttle brings them to the Packing Station, 50 cobblestone arrive in the output slots as packets and nothing is left missing

#### Scenario: Request an amount
- **WHEN** the player clicks iron ingots, of which 200 are in a rack beside a rail with a shuttle available, presses +10 three times and confirms
- **THEN** a request for 94 is listed, a shuttle fetches two loads to the Packing Station, and 94 ingots land in the output slots over time

#### Scenario: Stack steps
- **WHEN** the amount is 1 and the player presses +stack for an item that stacks to 64 and is requestable 200 times
- **THEN** the amount is 64, a second +stack makes it 128, and -stack from 70 makes it 64

#### Scenario: Never below one
- **WHEN** the amount is 64 and the player presses -stack, or the amount is 5 and the player presses -10
- **THEN** the amount is 1

#### Scenario: Prompt starts at what is there
- **WHEN** the player clicks bread, of which 10 are requestable and none can be crafted
- **THEN** the prompt shows 10, and typing 64 or pressing +stack leaves it at 10

#### Scenario: Rack needs a shuttle
- **WHEN** the player requests bread that sits in a rack and the warehouse has no garage
- **THEN** the request is refused and the GUI says the item needs a shuttle

#### Scenario: No Packing Station
- **WHEN** the terminal reaches a warehouse only through its controller and the warehouse has no Packing Station on that data line
- **THEN** the request is refused and the GUI says there is no Packing Station

#### Scenario: Partly in stock
- **WHEN** a stale screen asks for 100 bread, the warehouse holds 70 requestable and no Crafting CPU is on the line
- **THEN** a request for 70 is made, 70 are delivered and nothing is listed as missing

#### Scenario: Second request for the last item
- **WHEN** one configuration item is in stock, a request for it is open and the player requests it again from a screen that still shows it
- **THEN** the second request is refused and the GUI says it is out of stock

#### Scenario: Shortage ordered
- **WHEN** the player requests 8 sticks, none are in stock, and a Crafting Station on the line with a formed CPU can make sticks from a pattern with planks in stock
- **THEN** the request is listed as producing and the sticks arrive in the output slots once crafted

### Requirement: Open requests
The terminal's open requests SHALL be the first entries of the item grid, before the stock entries and independent of the sort, drawn on a tinted background with the delivered and requested amounts and a mark for the state: picking while items are on their way to the Packing Station, sending while packets are on the line, waiting for room in the output slots, producing, or missing stock. A request SHALL show missing stock only when a shipment already under way brought fewer items than planned and the rest is not in stock yet. Their tooltip SHALL name the item, the amounts and the state in words. The search filters them like stock; the view setting does not hide them. Clicking a request entry SHALL cancel it: picks not yet started are dropped and their locks released, items already picked or at the Packing Station go back into storage, packets already on the line still land in the output slots, and a work order of the request is cancelled.

#### Scenario: Progress
- **WHEN** a request for 128 cobblestone has 64 delivered and the rest on its way to the Packing Station
- **THEN** the first grid entry shows cobblestone with 64 of 128 and the state picking

#### Scenario: Sending
- **WHEN** the items of a request are travelling as packets
- **THEN** its grid entry shows the state sending

#### Scenario: Cancel
- **WHEN** the player clicks a request entry that is waiting for a shuttle
- **THEN** it leaves the grid and no shuttle is sent for it

#### Scenario: Producing
- **WHEN** a request's shortfall is on a work order
- **THEN** its grid entry shows the state producing

#### Scenario: Requests come first
- **WHEN** the grid is sorted by name and a request for zinc is open while apples are in stock
- **THEN** the zinc request is the first entry, before the apples

#### Scenario: Shipment short
- **WHEN** a shipment for 64 cobblestone arrives with only 60 because the Packing Station was broken and rebuilt, and no cobblestone is left in stock
- **THEN** the request shows 4 missing until cobblestone arrives in a rack or it is cancelled

### Requirement: Craftable entries
The stock grid SHALL also list every item a producer on the terminal's data line can make, from the patterns of the line and the recipes producers report. A craftable item with nothing requestable SHALL show "Craft" instead of a count; a craftable item that is requestable SHALL show its count and a craftable marker. A view button in the side column SHALL cycle the grid between All, Stock and Craftable.

#### Scenario: Sticks craftable
- **WHEN** no sticks are in stock and a sticks pattern and a Crafting Station are on the terminal's data line
- **THEN** the grid shows sticks with "Craft"

#### Scenario: Filter
- **WHEN** the player sets the view to Craftable
- **THEN** only craftable items are shown, stocked or not

### Requirement: Screen layout
The Storage Terminal screen SHALL have a column of view buttons left of the panel, the title with a Craft button and the search box, the item grid nine wide with its scrollbar, a message line, the Input row, the Output row, and the player inventory with the hotbar. Each SHALL have its own area that no other element overlaps; the order grid and the dialogs lie over the item grid, the message line and the Input and Output rows only, and leave the player inventory free. A small arrow beside the Input row SHALL point up, into the warehouse, and one beside the Output row SHALL point down, to the player. The grid SHALL show as many rows as the height setting asks for, small, medium, tall or auto (as many as fit the screen), but never more than fit the screen, and the panel SHALL fit a 1080p screen at GUI scales 2, 3 and 4.

#### Scenario: Nothing overlaps
- **WHEN** the screen is open with requests in the grid and a message shown
- **THEN** no label, button, grid cell or slot overlaps another area

#### Scenario: Grid has its own area
- **WHEN** the screen is open
- **THEN** the input row lies above the output row, both below the item grid, and the order grid leaves the player inventory uncovered

#### Scenario: Tall on a small screen
- **WHEN** the height is tall at GUI scale 4 on a 1080p screen
- **THEN** the grid shows only as many rows as fit and the panel stays on the screen

#### Scenario: Row arrows
- **WHEN** the screen is open
- **THEN** the arrow beside the Input row points up and the arrow beside the Output row points down

## ADDED Requirements

### Requirement: Order grid
A Craft button on the title row SHALL open the order grid and SHALL be shown only while a formed Crafting CPU and a Crafting Station are on the terminal's data line. While the order grid is closed its cells SHALL NOT take clicks or show; the ghost items stay until the screen closes.

#### Scenario: Open the order grid
- **WHEN** a formed CPU and a Crafting Station are on the line and the player presses Craft
- **THEN** the order grid opens with an empty grid and the player inventory stays usable

#### Scenario: No Craft button without a CPU
- **WHEN** the terminal's data line has a Crafting Station but no formed Crafting CPU
- **THEN** the title row shows no Craft button

### Requirement: Order grid amount and preview
The order grid SHALL show an amount of result items with the same -stack, -10, -1, +1, +10 and +stack buttons as the amount prompt, the server's preview for that amount (from stock, crafted, missing), Start and Close. Start SHALL be disabled, with the reason shown, while the cells match no recipe, raw materials are missing or no Crafting CPU or Crafting Station is on the line.

#### Scenario: Preview
- **WHEN** the grid holds a plank above a plank, the amount is 8 and the warehouse holds 16 oak planks
- **THEN** the preview shows 4 oak planks from stock and 8 sticks crafted, and Start is enabled

#### Scenario: Nothing matches
- **WHEN** the grid holds a single stick
- **THEN** no result shows and Start is disabled with the reason that the grid matches no recipe

### Requirement: Order grid cells
The order grid SHALL have a 3x3 grid of ghost cells and the result of the vanilla crafting recipe the cells match. Clicking a cell with an item on the cursor SHALL set a single ghost copy of it and clicking with an empty cursor SHALL clear the cell; the cursor and the player inventory keep their items. No real items SHALL lie in the order grid.

#### Scenario: Sticks by ghost
- **WHEN** the player clicks the top middle and the centre cell with oak planks on the cursor
- **THEN** both cells show a plank, the result shows four sticks and the player still holds every plank

#### Scenario: Clear a cell
- **WHEN** a cell shows a plank and the player clicks it with an empty cursor
- **THEN** the cell is empty

### Requirement: JEI fills the order grid
With JEI installed, the transfer button on a vanilla crafting recipe while the Storage Terminal is open SHALL open the order grid and set its ghost cells to the recipe's ingredients, choosing for each cell the accepted item the warehouse has the most of, else the first one. It SHALL take nothing from the player inventory or the warehouse. While no formed Crafting CPU and Crafting Station are on the terminal's data line the transfer button SHALL be refused with that reason.

#### Scenario: Transfer sticks
- **WHEN** the player presses JEI's transfer button on the stick recipe while the warehouse holds birch planks only
- **THEN** the grid shows a birch plank above a birch plank, the result shows four sticks and the inventory is unchanged

#### Scenario: No CPU
- **WHEN** the terminal's data line has no formed Crafting CPU and the player looks at the stick recipe in JEI
- **THEN** the transfer button is refused with a tooltip saying a Crafting CPU and a Crafting Station are needed

### Requirement: Request beyond stock is crafted
When the amount asked is above the requestable count and a Crafting CPU on the line can craft the rest with nothing missing, the prompt SHALL say how many come from stock and how many are crafted, with the server's craft preview, and confirming SHALL ship the requestable part and order the rest on the same request. While the rest cannot be crafted the prompt SHALL say why and allow no more than the requestable count.

#### Scenario: Ten from stock, the rest crafted
- **WHEN** 10 sticks are requestable, a sticks pattern, a Crafting Station and a CPU are on the line with 40 planks in stock, and the player asks for 64 sticks
- **THEN** the prompt says 10 from stock and 54 crafted, and confirming lists one request for 64 that ships 10 and is producing the rest

#### Scenario: Rest cannot be crafted
- **WHEN** 10 sticks are requestable and no planks are in stock, and the player types 64
- **THEN** the prompt lists planks as missing, Request is disabled until the amount is 10 or less

### Requirement: Order grid start
Start SHALL make a request of the terminal for the amount of the result, with a work order on a Crafting CPU of the line that crafts exactly the recipe and items of the grid, also when no pattern of the line has them. The ingredients SHALL come from the warehouse, the results SHALL land in the output slots like a craft request, and the request SHALL be listed and cancellable like any request. The order SHALL survive a reload with its recipe.

#### Scenario: One-off crafting table
- **WHEN** the grid holds four oak planks in a square, no pattern of the line makes a crafting table, the warehouse holds 16 oak planks and the player starts 1
- **THEN** a request for a crafting table is listed as producing, a Crafting Station crafts it from 4 planks of the warehouse and the crafting table lands in the output slots

#### Scenario: Restart during a one-off order
- **WHEN** a one-off crafting table order waits for its planks and the server restarts
- **THEN** the order goes on after the restart and the crafting table lands in the output slots

### Requirement: Save the order grid as a pattern
When no pattern of the line has the grid's recipe with the same items, Start SHALL ask whether to save it, offering "Save as pattern" and "Only this once"; both SHALL start the order. "Save as pattern" SHALL also store the grid as a pattern like the Production Terminal's editor. A refused save SHALL still start the order once and the message line SHALL say why the pattern was not saved. When a matching pattern exists Start SHALL order without asking.

#### Scenario: Save as pattern
- **WHEN** the grid holds four oak planks in a square, no crafting table pattern exists, a Pattern Storage with room is on the line and the player presses Start and then "Save as pattern"
- **THEN** the order starts and the Pattern Storage holds a crafting table pattern

#### Scenario: Only this once
- **WHEN** the player presses Start and then "Only this once" for the same grid
- **THEN** the order starts and no pattern is stored

#### Scenario: Known pattern
- **WHEN** the grid holds a plank above a plank and the line has the sticks pattern
- **THEN** Start orders right away without asking

#### Scenario: No room for the pattern
- **WHEN** every Pattern Storage of the line is full and the player chooses "Save as pattern"
- **THEN** the order starts once and the message line says the Pattern Storage is full

## REMOVED Requirements

### Requirement: Clear the grid
**Reason**: The order grid holds ghost items only; there is nothing to move back into the warehouse.
**Migration**: Clear a ghost cell by clicking it with an empty cursor.

### Requirement: Grid empties on close
**Reason**: No real items lie in the order grid, so nothing needs storing when the screen closes.
**Migration**: None; ghost items simply vanish with the screen.

### Requirement: Repeated crafting
**Reason**: The terminal no longer crafts by hand; the order grid orders the amount from a Crafting CPU instead.
**Migration**: Set the amount in the order grid, or craft by hand at a Crafting Table.

### Requirement: Crafting grid
**Reason**: The terminal no longer crafts by hand; the order grid with ghost cells replaces the crafting grid.
**Migration**: Use the order grid ("Order grid cells") or a Crafting Table.

### Requirement: JEI fills the grid
**Reason**: JEI's transfer now sets ghost cells of the order grid instead of moving items from the inventory.
**Migration**: See "JEI fills the order grid".

### Requirement: Crafting popup
**Reason**: The Craft button opens the order grid, which orders from a Crafting CPU instead of crafting by hand.
**Migration**: See "Order grid".
