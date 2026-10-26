# Spec Delta

## ADDED Requirements

### Requirement: Item popup with tabs
The Storage Terminal SHALL show the request amount, the craft amount panel and the stock rule of one item in one popup in the look of the craft popup, with three tabs above it: Request, Craft and Rule. The tab that opens SHALL follow the click that opened it; picking another tab SHALL keep the item and show that tab's page. Escape and Cancel SHALL close the popup.

#### Scenario: Right click opens Request
- **WHEN** the player right clicks iron ingots in the grid
- **THEN** the popup opens for iron ingots on its Request tab

#### Scenario: Switch tabs
- **WHEN** the popup shows iron ingots on its Request tab and the player clicks the Rule tab
- **THEN** the popup shows the Rule tab for iron ingots

#### Scenario: Craftable entry opens Craft
- **WHEN** the grid shows sticks with "Craft" and the player left clicks them
- **THEN** the popup opens for sticks on its Craft tab

### Requirement: Tabs that cannot act are greyed out
A popup tab that can do nothing for its item SHALL be greyed out, take no clicks and say why in its tooltip: Request while nothing of the item is requestable and it cannot be crafted (out of stock), Craft while no producer of the line makes the item (nothing makes it), Rule while no warehouse is reachable (no warehouse).

#### Scenario: Not craftable
- **WHEN** the popup opens for cobblestone, which no producer of the line makes
- **THEN** the Craft tab is greyed out and its tooltip says nothing on the line makes it

#### Scenario: Not in stock
- **WHEN** the popup opens from the Rules tab for bread, of which nothing is in stock and nothing makes it
- **THEN** the Request tab and the Craft tab are greyed out and the Rule tab is shown

### Requirement: Stock and Rules tabs
The Storage Terminal SHALL have two tabs above its panel, Stock and Rules, and SHALL open on Stock. Stock SHALL show the item grid as before. Rules SHALL show, in place of the grid, one row per stock rule of the reached warehouse with the item, its minimum and maximum ("8 to 16") and its state, sorted by item name, filtered by the search box and scrolled like the grid. Clicking a row SHALL open the item popup on its Rule tab.

#### Scenario: Rules listed
- **WHEN** the warehouse has rules for sticks (8 to 16, ok) and torches (32 to 64, ordering) and the player clicks the Rules tab
- **THEN** two rows show, torches after sticks, with "8 to 16" ok and "32 to 64" ordering

#### Scenario: Search filters rules
- **WHEN** the Rules tab is shown and the player types "torch"
- **THEN** only the torches row shows

#### Scenario: Row opens the Rule tab
- **WHEN** the player clicks the sticks row
- **THEN** the popup opens for sticks on its Rule tab with 8 and 16

### Requirement: Grid buttons only on Stock
While the Rules tab is shown the sort, direction and view buttons SHALL be hidden; the search mode and height buttons SHALL stay.

#### Scenario: Back to the grid
- **WHEN** the Rules tab is shown and the player clicks the Stock tab
- **THEN** the item grid shows with its sort, direction and view buttons

### Requirement: Add a rule for any item
The Rules tab SHALL have an add slot as its first row, which does not scroll. Clicking it with a carried stack, or dropping a JEI ingredient on it with JEI installed, SHALL open the item popup for that item on its Rule tab; the carried stack SHALL stay on the cursor. An item that already has a rule SHALL open with that rule.

#### Scenario: Rule for an item not in stock
- **WHEN** the warehouse holds no bread, the player carries one bread and clicks the add slot
- **THEN** the Rule tab opens for bread and the bread is still on the cursor

#### Scenario: From JEI
- **WHEN** the player drags glass from the JEI list onto the add slot
- **THEN** the Rule tab opens for glass

## MODIFIED Requirements

### Requirement: Request items
Clicking a stocked entry SHALL request one item at once; when this site already has an open request for that item with no work order, the click SHALL add one to that request instead of making a new one. Shift clicking an entry SHALL request one stack at once, or all that is requestable when that is less than one stack. Right clicking an entry SHALL open the item popup on its Request tab with the Foundation number stepper (+1, +10 and +stack above the amount, -1, -10 and -stack below it) and the item beside the amount, and Request SHALL create a request for that amount. +stack SHALL raise the amount to the next full stack above it and -stack SHALL lower it to the full stack below it; the amount SHALL stay at least 1 and at most 99999. The Request tab SHALL start at one stack, or at the requestable count when that is smaller, and SHALL NOT go above the requestable count unless the rest can be crafted. A request SHALL be shipped through the Packing Station with the shortest shuttle trip that shares the terminal's data line: shuttles pick the items from the racks into that Packing Station, and they travel on as packets into the request's output slot. A request SHALL be refused with the reason when no Packing Station shares the line, when no shuttle can reach the racks holding the item (needs a shuttle), when the Packing Station touches no rail, when nothing of the item is requestable and it cannot be crafted (out of stock), or when no output slot is free for it (output row full).

#### Scenario: Request one
- **WHEN** the player clicks cobblestone, the warehouse holds 50 in a rack on the rails and has a garage with a shuttle
- **THEN** a request for 1 is made and one cobblestone arrives in its output slot

#### Scenario: Clicks add up
- **WHEN** the player clicks cobblestone three times while the first request is still on its way
- **THEN** one request for 3 cobblestone is open in one output slot and 3 cobblestone arrive in that slot

#### Scenario: Request a stack
- **WHEN** the player shift clicks cobblestone, the warehouse holds 50 in a rack on the rails and has a garage with a shuttle
- **THEN** a request for 50 is made, a shuttle brings them to the Packing Station, 50 cobblestone arrive in the request's output slot as packets and nothing is left missing

#### Scenario: Request an amount
- **WHEN** the player right clicks iron ingots, of which 200 are in a rack beside a rail with a shuttle available, presses +10 three times on the Request tab and presses Request
- **THEN** a request for 94 is listed, a shuttle fetches two loads to the Packing Station, and 94 ingots land in the request's output slot over time

#### Scenario: Stack steps
- **WHEN** the amount is 1 and the player presses +stack for an item that stacks to 64 and is requestable 200 times
- **THEN** the amount is 64, a second +stack makes it 128, and -stack from 70 makes it 64

#### Scenario: Never below one
- **WHEN** the amount is 64 and the player presses -stack, or the amount is 5 and the player presses -10
- **THEN** the amount is 1

#### Scenario: Prompt starts at what is there
- **WHEN** the player right clicks bread, of which 10 are requestable and none can be crafted
- **THEN** the Request tab shows 10, and typing 64 or pressing +stack leaves it at 10

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
- **THEN** the request is listed as producing and the sticks arrive in its output slot once crafted

### Requirement: Request beyond stock is crafted
When the amount asked is above the requestable count and a Crafting CPU on the line can craft the rest with nothing missing, the Request tab SHALL say how many come from stock and how many are crafted, with the server's craft preview, and Request SHALL ship the requestable part and order the rest on the same request. While the rest cannot be crafted the Request tab SHALL say why and allow no more than the requestable count.

#### Scenario: Ten from stock, the rest crafted
- **WHEN** 10 sticks are requestable, a sticks pattern, a Crafting Station and a CPU are on the line with 40 planks in stock, and the player asks for 64 sticks
- **THEN** the Request tab says 10 from stock and 54 crafted, and Request lists one request for 64 that ships 10 and is producing the rest

#### Scenario: Rest cannot be crafted
- **WHEN** 10 sticks are requestable and no planks are in stock, and the player types 64
- **THEN** the Request tab lists planks as missing, Request is disabled until the amount is 10 or less

### Requirement: Craft dialog
Clicking a craftable item that is not in stock with any button, or Ctrl clicking or middle clicking a craftable item that is in stock, SHALL open the item popup on its Craft tab, which shows the amount panel for that item. The amount panel SHALL show the title "Craft" with the item's name, the Foundation number stepper with +1, +10 and +stack above the amount and -1, -10 and -stack below it, starting at 1, and beside the amount the item to make with the count that gets crafted. Below SHALL be three scrollable item lists of the server's preview for that amount, computed with the order planner: From stock, To craft and Missing; Missing SHALL always show, empty when nothing is missing, and all three SHALL use the panel's dark text colour. Cancel SHALL be on the left and Start on the right. Start SHALL always show; it SHALL be greyed out, with the reason as its tooltip, while the preview is on its way, raw materials are missing or no Crafting CPU is on the line, and SHALL have no tooltip when it can start. The panel SHALL show no status line.

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
- **THEN** the popup opens on its Craft tab for sticks with the amount 1 and no request is made

#### Scenario: Crafted count beside the amount
- **WHEN** the amount panel for sticks shows the amount 5 and one craft makes 4 sticks
- **THEN** the stick beside the amount shows 8

#### Scenario: Ingredients crafted too
- **WHEN** the warehouse holds oak logs but no planks, a pattern for oak planks is on the line and the player opens the craft dialog for 8 sticks
- **THEN** From stock lists oak logs and To craft lists 8 sticks and oak planks

#### Scenario: Long preview scrolls
- **WHEN** the preview has more ingredients than fit in From stock
- **THEN** the list shows a scrollbar and the mouse wheel over it shows the rest

### Requirement: Stock rules from the terminal
The item popup SHALL have a Rule tab that sets or deletes the stock rule of its item, and the Rules tab SHALL list every rule. Items with a rule SHALL show a marker in the stock grid and the rule with its state in their tooltip.

#### Scenario: Save a rule
- **WHEN** the player right clicks sticks, picks the Rule tab, sets 8 and 16 and saves
- **THEN** the sticks entry shows a marker and the tooltip reads "keep 8 to 16"

#### Scenario: Remove
- **WHEN** the player opens the Rule tab for sticks and presses Remove
- **THEN** the marker and tooltip line are gone

### Requirement: Screen layout
The Storage Terminal screen SHALL have the Stock and Rules tabs above the panel, a column of view buttons left of the panel, the Craft button right of the panel, the title and the search box, the item grid nine wide with its scrollbar, a message line, the Input row, the Output row, and the player inventory with the hotbar. Each SHALL have its own area that no other element overlaps; the item popup with its tabs, the order grid and the amount panel lie over the item grid, the message line and the Input and Output rows only, may reach past the panel's sides, and leave the player inventory free. A small arrow beside the Input row SHALL point up, into the warehouse, and one beside the Output row SHALL point down, to the player. The grid SHALL show as many rows as the height setting asks for, small, medium, tall or auto (as many as fit the screen), but never more than fit the screen with the tabs, and the panel with its tabs SHALL fit a 1080p screen at GUI scales 2, 3 and 4. A stack the player carries SHALL be drawn above the grid, the popups, the order grid and the amount panel.

#### Scenario: Nothing overlaps
- **WHEN** the screen is open with requests on the Output row and a message shown
- **THEN** no label, tab, button, grid cell, slot or request mark overlaps another area

#### Scenario: Grid has its own area
- **WHEN** the screen is open
- **THEN** the input row lies above the output row, both below the item grid, and the order grid leaves the player inventory uncovered

#### Scenario: Tall on a small screen
- **WHEN** the height is tall at GUI scale 4 on a 1080p screen
- **THEN** the grid shows only as many rows as fit, and the panel and its tabs stay on the screen

#### Scenario: Row arrows
- **WHEN** the screen is open
- **THEN** the arrow beside the Input row points up and the arrow beside the Output row points down

#### Scenario: Carried stack over a dialog
- **WHEN** the item popup is open and the player carries a stack from the inventory over it
- **THEN** the stack is drawn above the popup
