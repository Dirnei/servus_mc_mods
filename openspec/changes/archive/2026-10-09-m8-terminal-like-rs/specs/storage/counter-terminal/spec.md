# Spec Delta

## MODIFIED Requirements

### Requirement: Stock view with search
The Counter Terminal GUI SHALL list every indexed item of the warehouses it reaches over its data line with its total in a scrollable grid, sorted by the player's sort setting (name, amount or mod, ascending or descending; amount descending by default). A search box SHALL filter the list by item name, case insensitive, as the player types.

#### Scenario: Search
- **WHEN** the warehouse holds cobblestone, iron ingots and bread and the player types "iron"
- **THEN** only iron ingots are shown

#### Scenario: Sort by name
- **WHEN** the warehouse holds 50 cobblestone, 10 apples and 20 bread and the sort is name ascending
- **THEN** the grid shows apple, bread, cobblestone in that order

#### Scenario: Sort by amount
- **WHEN** the same stock is sorted by amount descending
- **THEN** the grid shows cobblestone, bread, apple

### Requirement: Request items
Clicking an entry SHALL open an amount prompt defaulting to one stack, with buttons for -stack, -10, -1, +1, +10 and +stack and a text field, and confirming SHALL create a request for that amount. +stack SHALL raise the amount to the next full stack above it and -stack SHALL lower it to the full stack below it; the amount SHALL stay between 1 and 99999. Shift clicking an entry SHALL request one stack at once. A request SHALL be shipped through the Access Point with the shortest shuttle trip that shares the terminal's data line: picks land in that Access Point (instantly for stock no shuttle can reach in crates and foreign inventories, by shuttle for stock a shuttle can reach, never instantly from a rack) and travel on as packets into the output slots. A request SHALL be refused with the reason when no Access Point shares the line, when its stock sits only in racks no shuttle can reach, or when it needs a shuttle while the Access Point touches no rail. When stock falls short and a producer can make the item, the shortfall SHALL become a work order linked to the request.

#### Scenario: Request a stack
- **WHEN** the player shift clicks cobblestone, the warehouse holds 50 in a crate touching the controller and has no garage
- **THEN** 50 cobblestone arrive in the output slots as packets shortly after

#### Scenario: Request an amount
- **WHEN** the player clicks iron ingots, presses +10 three times and confirms, and the ingots sit in a crate beside a rail with a shuttle available
- **THEN** a request for 94 is listed, a shuttle fetches two loads to the Access Point, and 94 ingots land in the output slots over time

#### Scenario: Stack steps
- **WHEN** the amount is 1 and the player presses +stack for an item that stacks to 64
- **THEN** the amount is 64, a second +stack makes it 128, and -stack from 70 makes it 64

#### Scenario: Never below one
- **WHEN** the amount is 64 and the player presses -stack, or the amount is 5 and the player presses -10
- **THEN** the amount is 1

#### Scenario: Rack needs a shuttle
- **WHEN** the player requests bread that only sits in a rack and the warehouse has no garage
- **THEN** the request is refused and the GUI says the item needs a shuttle

#### Scenario: No Access Point
- **WHEN** the terminal reaches a warehouse only through its controller and the warehouse has no Access Point on that data line
- **THEN** the request is refused and the GUI says there is no Access Point

#### Scenario: Partly in stock
- **WHEN** the player requests 100 bread and the warehouse holds 70
- **THEN** 70 are delivered and the request shows 30 missing until more arrives or it is cancelled

#### Scenario: Shortage ordered
- **WHEN** the player requests 8 sticks, none are in stock, and an Outbound Station faces a Crafting Station with the sticks recipe
- **THEN** the request is listed as producing and the sticks arrive in the output slots once crafted

### Requirement: Open requests
The terminal's open requests SHALL be the first entries of the item grid, before the stock entries and independent of the sort, drawn on a tinted background with the delivered and requested amounts and a mark for the state: picking while items are on their way to the Access Point, sending while packets are on the line, waiting for room in the output slots, producing, or missing stock. Their tooltip SHALL name the item, the amounts and the state in words. The search filters them like stock; the view setting does not hide them. Clicking a request entry SHALL cancel it: picks not yet started are dropped, items already picked or at the Access Point go back into storage, packets already on the line still land in the output slots, and a work order of the request is cancelled.

#### Scenario: Progress
- **WHEN** a request for 128 cobblestone has 64 delivered and the rest on its way to the Access Point
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

### Requirement: Craftable entries
The stock grid SHALL also list every item a producer on the terminal's data line can make, from the patterns of the line and the recipes producers report. A craftable item that is not in stock SHALL show "Craft" instead of a count; a craftable item that is in stock SHALL show its count and a craftable marker. A view button in the side column SHALL cycle the grid between All, Stock and Craftable.

#### Scenario: Sticks craftable
- **WHEN** no sticks are in stock and a sticks pattern and a Crafting Station are on the terminal's data line
- **THEN** the grid shows sticks with "Craft"

#### Scenario: Filter
- **WHEN** the player sets the view to Craftable
- **THEN** only craftable items are shown, stocked or not

### Requirement: Craft dialog
Clicking a craftable item that is not in stock, or Ctrl clicking or middle clicking a craftable item that is in stock, SHALL open a craft dialog with an amount field, the same -stack, -10, -1, +1, +10 and +stack buttons as the amount prompt, Start and Cancel. While the dialog is open the terminal SHALL show a preview the server computes with the order planner for that amount: what is taken from stock, what is crafted and what is missing. Start SHALL be disabled, with the reason shown, while raw materials are missing or no Crafting CPU is on the line.

#### Scenario: Preview
- **WHEN** the player opens the craft dialog for 8 sticks and the warehouse holds 16 oak planks
- **THEN** the preview shows 4 oak planks from stock and 8 sticks to craft, and Start is enabled

#### Scenario: Missing material
- **WHEN** the warehouse holds no planks and no logs
- **THEN** the preview lists oak planks as missing and Start is disabled

#### Scenario: No CPU
- **WHEN** no formed Crafting CPU is on the line
- **THEN** Start is disabled and the dialog says there is no CPU

#### Scenario: Alternatives in the preview
- **WHEN** the player opens the craft dialog for 10 charcoal with a furnace adapter on the line and the warehouse holds 6 oak logs and 4 birch logs but no acacia
- **THEN** the preview shows 6 oak logs and 4 birch logs from stock, 10 charcoal to craft and nothing missing

### Requirement: Screen layout
The Counter Terminal screen SHALL have a column of view buttons left of the panel, the title with the search box on its right, the item grid nine wide with its scrollbar, a message line, a row with the crafting grid, its arrow, result slot and clear button on the left and the output slots on the right, and the player inventory with the hotbar. Each SHALL have its own area that no other element overlaps. The grid SHALL show as many rows as the height setting asks for, small, medium, tall or auto (as many as fit the screen), but never more than fit the screen, and the panel SHALL fit a 1080p screen at GUI scales 2, 3 and 4.

#### Scenario: Nothing overlaps
- **WHEN** the screen is open with requests in the grid and a message shown
- **THEN** no label, button, grid cell or slot overlaps another area

#### Scenario: Grid has its own area
- **WHEN** the screen is open
- **THEN** the crafting grid, its result slot and the clear button overlap neither the output slots nor the item grid, and the crafting grid sits left of the output slots

#### Scenario: Tall on a small screen
- **WHEN** the height is tall at GUI scale 4 on a 1080p screen
- **THEN** the grid shows only as many rows as fit and the panel stays on the screen

## ADDED Requirements

### Requirement: View settings
The side column SHALL have buttons, each with an icon and a tooltip naming its current value, for the sort (name, amount, mod), the direction (ascending, descending), the view (all, stock, craftable), the search mode (normal, JEI synced, JEI two way) and the height (small, medium, tall, auto). The settings SHALL be saved per player on the client, SHALL be the same for every Counter Terminal and SHALL be kept across restarts.

#### Scenario: Settings kept
- **WHEN** the player sets the sort to mod and the height to tall, closes the game and opens another Counter Terminal later
- **THEN** that terminal sorts by mod and is tall

### Requirement: JEI search sync
With JEI installed, the search mode JEI synced SHALL copy the terminal's search text into JEI's search as the player types, and JEI two way SHALL also copy JEI's search text into the terminal's search box when it changes there. Without JEI the search mode button SHALL only offer normal.

#### Scenario: Synced
- **WHEN** the search mode is JEI synced and the player types "iron" in the terminal
- **THEN** JEI's search shows "iron"

#### Scenario: No JEI
- **WHEN** JEI is not installed
- **THEN** the search mode stays normal and the terminal works unchanged
