# Spec Delta

## ADDED Requirements

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
When more than one warehouse is reachable, the stock view SHALL show the sum of their stock. A request SHALL be served first by the warehouse holding the most of the item, then by the next, until the amount is covered or the stock runs out. Work orders for a shortfall, stock rules set in the terminal and pushes SHALL use the first reachable warehouse, the one whose controller has the lowest position.

#### Scenario: Merged stock
- **WHEN** one reachable warehouse holds 30 cobblestone and another 20
- **THEN** the terminal lists 50 cobblestone

#### Scenario: Split request
- **WHEN** the player requests 40 of that cobblestone
- **THEN** 30 come from the first warehouse and 10 from the second, and 40 land in the output slots

## MODIFIED Requirements

### Requirement: Stock view with search
The Counter Terminal GUI SHALL list every indexed item of the warehouses it reaches over its data line with its total, largest first, in a scrollable grid. A search box SHALL filter the list by item name, case insensitive, as the player types.

#### Scenario: Search
- **WHEN** the warehouse holds cobblestone, iron ingots and bread and the player types "iron"
- **THEN** only iron ingots are shown

### Requirement: Offline shown
When no warehouse is reachable the GUI SHALL show the reason instead of the stock and SHALL refuse requests and pushes. Output slots SHALL stay usable so deliveries already made can be collected.

#### Scenario: No controller
- **WHEN** the terminal sits on a data line with no controller and no Access Point of an online warehouse
- **THEN** the GUI says no warehouse is reachable

#### Scenario: Collect while offline
- **WHEN** the controller is broken after a delivery landed in the output slots
- **THEN** the player can still take the delivery out

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
