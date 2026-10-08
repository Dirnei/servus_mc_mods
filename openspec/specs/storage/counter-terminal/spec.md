# storage/counter-terminal Specification

## Purpose

The counter is where the player meets the warehouse: see everything, find it, take it, hand things in.

## Requirements

### Requirement: Stock view with search
The Counter Terminal GUI SHALL list every indexed item of the warehouses it reaches over its data line with its total, largest first, in a scrollable grid. A search box SHALL filter the list by item name, case insensitive, as the player types.

#### Scenario: Search
- **WHEN** the warehouse holds cobblestone, iron ingots and bread and the player types "iron"
- **THEN** only iron ingots are shown

### Requirement: Push into the warehouse
Clicking the grid with a carried stack SHALL store it in the warehouse; shift clicking an inventory slot SHALL store that slot. Pushes SHALL follow the zone routing but SHALL only go into crates and foreign inventories; racks are filled through a Inbound Station. Items that fit nowhere stay where they were.

#### Scenario: Shift click to store
- **WHEN** the player shift clicks 20 cobblestone in the inventory while an empty crate is on the network
- **THEN** the crate holds 20 cobblestone and the slot is empty

#### Scenario: Only racks have room
- **WHEN** the player pushes bread while every crate is full and a rack has room
- **THEN** the bread stays with the player

### Requirement: Live stock while open
The terminal SHALL send its full content (stock, craftable items, requests, rules) as soon as the screen opens, without waiting for the periodic update, and the screen SHALL show a loading hint until it arrives. While the GUI is open the stock shown SHALL refresh within half a second after the index changes, without resending when nothing changed.

#### Scenario: Machine output arrives
- **WHEN** a hopper puts 10 bread into a crate while the GUI is open
- **THEN** the bread count rises within a second

#### Scenario: Instant content
- **WHEN** the player opens the terminal
- **THEN** the stock is sent in the same tick the screen opens

### Requirement: Offline shown
When no warehouse is reachable the GUI SHALL show the reason instead of the stock and SHALL refuse requests and pushes. Output slots SHALL stay usable so deliveries already made can be collected.

#### Scenario: No controller
- **WHEN** the terminal sits on a data line with no controller and no Access Point of an online warehouse
- **THEN** the GUI says no warehouse is reachable

#### Scenario: Collect while offline
- **WHEN** the controller is broken after a delivery landed in the output slots
- **THEN** the player can still take the delivery out

### Requirement: Crafting
The Counter Terminal SHALL be crafted from glass, iron ingots, a chest and planks.

#### Scenario: Recipe
- **WHEN** the player crafts with the pattern
- **THEN** a Counter Terminal is produced

### Requirement: Request items
Clicking an entry SHALL open an amount prompt defaulting to one stack, with buttons for +1, +10 and +stack and a text field, and confirming SHALL create a request for that amount. Shift clicking an entry SHALL request one stack at once. A request SHALL be shipped through the Access Point with the shortest shuttle trip that shares the terminal's data line: picks land in that Access Point (instantly for stock no shuttle can reach in crates and foreign inventories, by shuttle for stock a shuttle can reach, never instantly from a rack) and travel on as packets into the output slots. A request SHALL be refused with the reason when no Access Point shares the line, when its stock sits only in racks no shuttle can reach, or when it needs a shuttle while the Access Point touches no rail. When stock falls short and a producer can make the item, the shortfall SHALL become a work order linked to the request.

#### Scenario: Request a stack
- **WHEN** the player shift clicks cobblestone, the warehouse holds 50 in a crate touching the controller and has no garage
- **THEN** 50 cobblestone arrive in the output slots as packets shortly after

#### Scenario: Request an amount
- **WHEN** the player clicks iron ingots, presses +10 three times and confirms, and the ingots sit in a crate beside a rail with a shuttle available
- **THEN** a request for 94 is listed, a shuttle fetches two loads to the Access Point, and 94 ingots land in the output slots over time

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

### Requirement: Output slots
The terminal SHALL have `terminal_output_slots` output slots (example 9, from `warehouse.json`) shown in its GUI. Deliveries SHALL land there and the player SHALL take them out by hand or shift click. The output slots SHALL NOT be indexed as warehouse stock. The terminal SHALL expose them to automation for insertion only, so packets can land; nothing SHALL be extracted from them by hoppers or ducts. Breaking the terminal SHALL drop their contents.

#### Scenario: Collect
- **WHEN** packets have delivered 64 cobblestone into the output slots
- **THEN** the player shift clicks the slot and the cobblestone is in the inventory

#### Scenario: Not stock
- **WHEN** 64 cobblestone sit in the output slots
- **THEN** the stock view does not count them

#### Scenario: No extraction
- **WHEN** a hopper sits below a terminal holding cobblestone in its output slots
- **THEN** the hopper takes nothing

### Requirement: Open requests
The GUI SHALL list the terminal's open requests with the item, the delivered and requested amounts and the state: picking while items are on their way to the Access Point, sending while packets are on the line, waiting for room in the output slots, producing, or missing stock. Clicking a request SHALL cancel it: picks not yet started are dropped, items already picked or at the Access Point go back into storage, packets already on the line still land in the output slots, and a work order of the request is cancelled. Requests are cleared when the terminal reloads.

#### Scenario: Progress
- **WHEN** a request for 128 cobblestone has 64 delivered and the rest on its way to the Access Point
- **THEN** the list shows 64 of 128 and the state picking

#### Scenario: Sending
- **WHEN** the items of a request are travelling as packets
- **THEN** the list shows the state sending

#### Scenario: Cancel
- **WHEN** the player clicks a request that is waiting for a shuttle
- **THEN** it leaves the list and no shuttle is sent for it

#### Scenario: Producing
- **WHEN** a request's shortfall is on a work order
- **THEN** the list shows the state producing

### Requirement: Stock rules from the terminal
The amount prompt SHALL have a Rule mode with minimum and maximum fields, Save and Remove, that sets or deletes the stock rule of the clicked item. Items with a rule SHALL show a marker in the stock grid and the rule with its state in their tooltip.

#### Scenario: Save a rule
- **WHEN** the player opens the prompt for sticks, switches to Rule, enters 8 and 16 and saves
- **THEN** the sticks entry shows a marker and the tooltip reads "keep 8 to 16"

#### Scenario: Remove
- **WHEN** the player opens the prompt for sticks in Rule mode and presses Remove
- **THEN** the marker and tooltip line are gone

### Requirement: Terminal sits on the data line
The Counter Terminal SHALL be a member of a data line with the link role, and SHALL NOT join a warehouse network by touching rails, crates, racks or controllers. A warehouse SHALL be reachable from the terminal when its controller or one of its Access Points is a member of the terminal's data network and the warehouse is online. The terminal's lamp SHALL be lit while at least one warehouse is reachable.

#### Scenario: Reached over the data line
- **WHEN** a terminal and an Access Point of an online warehouse are linked by data ducts
- **THEN** the terminal lists the warehouse's stock and its lamp is lit

#### Scenario: Beside a controller only
- **WHEN** a terminal touches a controller but no data duct
- **THEN** the terminal reaches no warehouse and its lamp is dark

#### Scenario: Rail beside the terminal
- **WHEN** a rail is placed beside a terminal
- **THEN** the rail shows no dock or arm towards the terminal

### Requirement: Several warehouses on one line
When more than one warehouse is reachable, the stock view SHALL show the sum of their stock. A request SHALL be served first by the warehouse holding the most of the item, then by the next, until the amount is covered or the stock runs out. Work orders for a shortfall SHALL go to a Crafting CPU on the terminal's data line; stock rules set in the terminal and pushes SHALL use the first reachable warehouse, the one whose controller has the lowest position.

#### Scenario: Merged stock
- **WHEN** one reachable warehouse holds 30 cobblestone and another 20
- **THEN** the terminal lists 50 cobblestone

#### Scenario: Split request
- **WHEN** the player requests 40 of that cobblestone
- **THEN** 30 come from the first warehouse and 10 from the second, and 40 land in the output slots

### Requirement: Craftable entries
The stock grid SHALL also list every item a producer on the terminal's data line can make, from the patterns of the line and the recipes producers report. A craftable item that is not in stock SHALL show "Craft" instead of a count; a craftable item that is in stock SHALL show its count and a craftable marker. A filter button next to the search box SHALL cycle the grid between All, Stock and Craftable.

#### Scenario: Sticks craftable
- **WHEN** no sticks are in stock and a sticks pattern and a Crafting Station are on the terminal's data line
- **THEN** the grid shows sticks with "Craft"

#### Scenario: Filter
- **WHEN** the player sets the filter to Craftable
- **THEN** only craftable items are shown, stocked or not

### Requirement: Craft dialog
Clicking a craftable item that is not in stock, or Ctrl clicking or middle clicking a craftable item that is in stock, SHALL open a craft dialog with an amount field, +1, +10 and +stack buttons, Start and Cancel. While the dialog is open the terminal SHALL show a preview the server computes with the order planner for that amount: what is taken from stock, what is crafted and what is missing. Start SHALL be disabled, with the reason shown, while raw materials are missing or no Crafting CPU is on the line.

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

### Requirement: Craft request
Start SHALL create a work order for the full amount on a Crafting CPU of the line, independent of the stock of the item itself, linked to a new request of the terminal that shows producing. The crafted items SHALL be put away and delivered into the output slots like any request. Cancelling the request SHALL cancel the order.

#### Scenario: Craft sticks
- **WHEN** the player starts crafting 8 sticks
- **THEN** a request for 8 sticks is listed as producing, and 8 sticks land in the output slots once crafted

### Requirement: Screen layout
The Counter Terminal screen SHALL give the search box and filter, the stock grid with its scrollbar, the request list, the output slots and the player inventory each their own area that no other element overlaps, and the panel SHALL fit a 1080p screen at the automatic GUI scale.

#### Scenario: Nothing overlaps
- **WHEN** the screen is open with requests listed and a message shown
- **THEN** no label, list row or slot overlaps another area

### Requirement: Requests survive a reload
The open requests of a Counter Terminal SHALL be saved with the terminal and SHALL be there again after the world is reloaded or the terminal's chunk loads again: item, requested, delivered and missing amounts, their shipments and the CPU holding their work order. Request ids SHALL stay unique across terminals and reloads. After a reload a request SHALL go on as before: shipments that still run are booked when they end, a missing part is shipped again, and a request whose work order no longer exists is retried as missing stock.

#### Scenario: Restart with a producing request
- **WHEN** a request for 8 sticks is producing on a CPU and the server restarts
- **THEN** the request is listed again after the restart and the sticks land in the terminal once the order is done

#### Scenario: Ids stay unique
- **WHEN** a terminal with request 5 is reloaded and a new request is made
- **THEN** the new request gets an id above every id used before
