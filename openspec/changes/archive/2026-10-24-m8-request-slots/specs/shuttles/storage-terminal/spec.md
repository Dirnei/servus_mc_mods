# Spec Delta

## MODIFIED Requirements

### Requirement: Request items
Clicking a stocked entry SHALL request one item at once; when this site already has an open request for that item with no work order, the click SHALL add one to that request instead of making a new one. Shift clicking an entry SHALL request one stack at once, or all that is requestable when that is less than one stack. Right clicking an entry SHALL open an amount prompt with buttons for -stack, -10, -1, +1, +10 and +stack and a text field, and confirming SHALL create a request for that amount. +stack SHALL raise the amount to the next full stack above it and -stack SHALL lower it to the full stack below it; the amount SHALL stay at least 1 and at most 99999. The prompt SHALL start at one stack, or at the requestable count when that is smaller, and SHALL NOT go above the requestable count unless the rest can be crafted. A request SHALL be shipped through the Packing Station with the shortest shuttle trip that shares the terminal's data line: shuttles pick the items from the racks into that Packing Station, and they travel on as packets into the request's output slot. A request SHALL be refused with the reason when no Packing Station shares the line, when no shuttle can reach the racks holding the item (needs a shuttle), when the Packing Station touches no rail, when nothing of the item is requestable and it cannot be crafted (out of stock), or when no output slot is free for it (output row full).

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
- **WHEN** the player right clicks iron ingots, of which 200 are in a rack beside a rail with a shuttle available, presses +10 three times and confirms
- **THEN** a request for 94 is listed, a shuttle fetches two loads to the Packing Station, and 94 ingots land in the request's output slot over time

#### Scenario: Stack steps
- **WHEN** the amount is 1 and the player presses +stack for an item that stacks to 64 and is requestable 200 times
- **THEN** the amount is 64, a second +stack makes it 128, and -stack from 70 makes it 64

#### Scenario: Never below one
- **WHEN** the amount is 64 and the player presses -stack, or the amount is 5 and the player presses -10
- **THEN** the amount is 1

#### Scenario: Prompt starts at what is there
- **WHEN** the player right clicks bread, of which 10 are requestable and none can be crafted
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
- **THEN** the request is listed as producing and the sticks arrive in its output slot once crafted

### Requirement: Output slots
The terminal SHALL have `terminal_output_slots` output slots (example 9, from `warehouse.json`, at most 9 shown) in one Output row below the Input row. Each open request SHALL own one output slot from the moment it is made until it ends, and deliveries for a request SHALL land only in its slot as packets. The player SHALL take them out by hand or shift click at any time. The output slots SHALL NOT be indexed as warehouse stock. Hoppers, item pipes and other automation SHALL neither insert into nor extract from them. Breaking the terminal SHALL drop their contents.

#### Scenario: Collect
- **WHEN** packets have delivered 64 cobblestone into a request's output slot
- **THEN** the player shift clicks the slot and the cobblestone is in the inventory

#### Scenario: Not stock
- **WHEN** 64 cobblestone sit in the output slots
- **THEN** the stock view does not count them

#### Scenario: No extraction
- **WHEN** a hopper sits below a terminal holding cobblestone in its output slots
- **THEN** the hopper takes nothing

#### Scenario: More than a stack refills the slot
- **WHEN** a request for 94 iron ingots has filled its slot with 64 and the player has not taken them
- **THEN** the other 30 wait at the Packing Station, the slot shows waiting for room, and they land in the same slot once the player takes the 64

#### Scenario: Slot taken until emptied
- **WHEN** a request for 16 cobblestone is done and the 16 still lie in its slot
- **THEN** a new request for bread gets another slot, and a new request for cobblestone may use that slot

### Requirement: Open requests
The terminal's open requests SHALL be shown on their output slots, not in the item grid. A request slot SHALL show a ghost of the item while it holds nothing, the delivered and requested amounts, and a mark for the state: picking while items are on their way to the Packing Station, sending while packets are on the line, waiting for room in the slot, producing, or missing stock. A request SHALL show missing stock only when a shipment already under way brought fewer items than planned and the rest is not in stock yet. Hovering a request slot SHALL show a tooltip naming the item, the amounts, the state in words and that middle click or Ctrl click cancels it. Middle clicking or Ctrl clicking a request slot SHALL cancel the request: picks not yet started are dropped and their locks released, items already picked or at the Packing Station go back into storage, packets already on the line still land in the slot, and a work order of the request is cancelled. Items already in the slot SHALL stay there for the player.

#### Scenario: Progress
- **WHEN** a request for 128 cobblestone has 64 delivered and the rest on its way to the Packing Station
- **THEN** its output slot shows cobblestone with 64 of 128 and the state picking

#### Scenario: Sending
- **WHEN** the items of a request are travelling as packets
- **THEN** its output slot shows the state sending

#### Scenario: Ghost while empty
- **WHEN** a request for 8 bread waits for a shuttle and nothing has arrived
- **THEN** its output slot shows a ghost bread with 0 of 8 and cannot be taken from

#### Scenario: Cancel
- **WHEN** the player middle clicks a request slot that is waiting for a shuttle
- **THEN** the request ends, the slot is free and no shuttle is sent for it

#### Scenario: Cancel keeps what arrived
- **WHEN** the player Ctrl clicks the slot of a request for 128 cobblestone with 64 delivered
- **THEN** the 64 stay in the slot, the rest is not sent and the slot stays taken until the player takes the 64

#### Scenario: Taking is not cancelling
- **WHEN** the player left clicks a request slot holding 20 of 64 cobblestone
- **THEN** the 20 are on the cursor and the request goes on

#### Scenario: Producing
- **WHEN** a request's shortfall is on a work order
- **THEN** its output slot shows the state producing

#### Scenario: Requests come first
- **WHEN** the grid is sorted by name and a request for zinc is open while apples are in stock
- **THEN** the zinc request shows on its output slot, and the grid starts with the apples and shows no zinc request entry

#### Scenario: Shipment short
- **WHEN** a shipment for 64 cobblestone arrives with only 60 because the Packing Station was broken and rebuilt, and no cobblestone is left in stock
- **THEN** the request shows 4 missing until cobblestone arrives in a rack or it is cancelled

### Requirement: Craft dialog
Clicking a craftable item that is not in stock with any button, or Ctrl clicking or middle clicking a craftable item that is in stock, SHALL open a craft dialog with an amount field, the same -stack, -10, -1, +1, +10 and +stack buttons as the amount prompt, Start and Cancel. While the dialog is open the terminal SHALL show a preview the server computes with the order planner for that amount: what is taken from stock, what is crafted and what is missing. Start SHALL be disabled, with the reason shown, while raw materials are missing or no Crafting CPU is on the line.

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

#### Scenario: Craft entry on a plain click
- **WHEN** the grid shows sticks with "Craft" and the player left clicks them
- **THEN** the craft dialog opens and no request is made

### Requirement: Craft request
Start SHALL create a work order for the full amount on a Crafting CPU of the line, independent of the stock of the item itself, linked to a new request of the terminal that takes a free output slot and shows producing. The crafted items SHALL land in that slot like any request. Cancelling the request SHALL cancel the order. Start SHALL be refused with the reason when no output slot is free.

#### Scenario: Craft sticks
- **WHEN** the player starts crafting 8 sticks
- **THEN** a request for 8 sticks shows producing on an output slot, and 8 sticks land in that slot once crafted

#### Scenario: No free slot for a craft
- **WHEN** every output slot is taken and the player presses Start in the craft dialog
- **THEN** no order is made and the dialog says the output row is full

### Requirement: Screen layout
The Storage Terminal screen SHALL have a column of view buttons left of the panel, the title with a Craft button and the search box, the item grid nine wide with its scrollbar, a message line, the Input row, the Output row, and the player inventory with the hotbar. Each SHALL have its own area that no other element overlaps; the order grid and the dialogs lie over the item grid, the message line and the Input and Output rows only, and leave the player inventory free. A small arrow beside the Input row SHALL point up, into the warehouse, and one beside the Output row SHALL point down, to the player. The grid SHALL show as many rows as the height setting asks for, small, medium, tall or auto (as many as fit the screen), but never more than fit the screen, and the panel SHALL fit a 1080p screen at GUI scales 2, 3 and 4.

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

### Requirement: Transfers from another site
Requesting an item while another site is shown SHALL make a transfer to this site: the other site ships the items from one of its Packing Stations to a Packing Station of the warehouse this terminal reaches first that reaches the terminal by data line, and that station passes them on as packets into the transfer's output slot like a request from this site's storage; they SHALL NOT be put away at this site. The request SHALL be refused with the reason when no output slot is free, when this site's controller is not wireless capable, when this site has no Packing Station, when the other site's controller is not wireless capable, when the other site has no Packing Station or sleeps without enough Anchor Upgrades to wake, or when the other site, online, refuses the shipment. A sleeping site that can wake SHALL be woken and held awake until the transfer ends or is cancelled. The transfer SHALL take an output slot like a request and show its delivered and requested amounts and a state (waking, picking, sending, waiting for power, missing), and SHALL end when the items arrived in its slot; middle or Ctrl clicking its slot SHALL cancel it like a request. Clicks on an item SHALL NOT add to an open transfer; each makes a new one. Transfers SHALL survive a reload like requests.

#### Scenario: Transfer from an online outpost
- **WHEN** the player requests 16 iron ingots of the online outpost at the base terminal, both controllers are wireless capable and both sites have a Packing Station
- **THEN** a transfer for 16 takes an output slot, the iron arrives in that slot, the base's storage does not change and the transfer ends

#### Scenario: Output full
- **WHEN** a transfer from the outpost arrives while its output slot is full
- **THEN** the items wait in the receiving Packing Station like packets of a local request and move on once the player empties the slot

#### Scenario: Wake for a transfer
- **WHEN** the outpost sleeps, has enough Anchor Upgrades and a wireless capable controller, and the player requests iron from it
- **THEN** the outpost wakes, the transfer shows waking and then picking, and the outpost sleeps again after the transfer and its grace time

#### Scenario: Cannot be ordered
- **WHEN** the outpost sleeps without a Anchor Upgrade
- **THEN** a request is refused with the reason that the site sleeps and cannot wake

#### Scenario: Outpost without power
- **WHEN** the outpost's controller holds a Wireless Upgrade but no FE and the player requests iron from it
- **THEN** the request is refused with "That site has no power for wireless"

#### Scenario: No Packing Station here
- **WHEN** the base has no Packing Station on its rails and the player requests iron from the outpost
- **THEN** the request is refused with "This site has no Packing Station"

### Requirement: Packets land in the terminal
Packets delivered to the terminal over the data line SHALL enter through its packet inlet: items an open request of the terminal still waits for SHALL land in that request's output slot, every other item in the Input row, which stores it. Items that do not fit SHALL wait at their source as the data line rules say. The terminal SHALL register no item capability, so item pipes show no item arm toward it.

#### Scenario: Delivery lands in the output
- **WHEN** a request waits for sticks and packets deliver sticks
- **THEN** the sticks land in that request's output slot

#### Scenario: Unrequested packets are stored
- **WHEN** packets deliver cobblestone that no request of the terminal waits for
- **THEN** the cobblestone lands in the Input row and goes on into the warehouse

#### Scenario: Not into another request's slot
- **WHEN** a request for sticks has a full slot and another slot holds bread of a second request
- **THEN** the next sticks wait at their source and the bread slot gets no sticks

#### Scenario: Item pipe beside the terminal
- **WHEN** a duct with the Item Pipe is placed beside a terminal
- **THEN** the duct shows no item arm toward the terminal

## ADDED Requirements

### Requirement: Output row full
A new request, transfer, craft request or order grid start SHALL get an output slot that is free: no open request owns it and it holds nothing, or it holds the same item with room left. A slot that still holds items of a request that ended SHALL stay taken until it is emptied. When no slot is free the terminal SHALL refuse with "Output row full, take your items"; adding to an open request does not need a new slot.

#### Scenario: Ten requests
- **WHEN** nine requests for different items are open and the player clicks a tenth item
- **THEN** no request is made and the message line says "Output row full, take your items"

#### Scenario: Room after taking
- **WHEN** every slot is taken, one holds the 16 cobblestone of a finished request and the player takes them out
- **THEN** the next click on an item makes a request in that slot

#### Scenario: Adding needs no slot
- **WHEN** every slot is taken, one by an open request for cobblestone, and the player clicks cobblestone
- **THEN** that request grows by one and nothing is refused

### Requirement: Request slots survive a reload
The output slot a request owns SHALL be saved with the request and SHALL be the same after the world is reloaded or the terminal's chunk loads again.

#### Scenario: Restart with two requests
- **WHEN** a request for bread owns slot 1 and a request for iron owns slot 4 and the server restarts
- **THEN** after the restart bread still shows on slot 1 and iron on slot 4, and their deliveries land there

### Requirement: Slot choice is pure
Choosing the output slot for a new request (same item with room first, then the leftmost empty slot that no request owns, else none) SHALL be plain Java without Minecraft classes and covered by unit tests.

#### Scenario: Same item first
- **WHEN** slot 0 is empty and free and slot 3 holds 16 cobblestone of an ended request
- **THEN** a new cobblestone request gets slot 3

#### Scenario: Owned slots are skipped
- **WHEN** slot 0 is owned by an open request and holds nothing, and slot 1 is empty and free
- **THEN** a new request gets slot 1
