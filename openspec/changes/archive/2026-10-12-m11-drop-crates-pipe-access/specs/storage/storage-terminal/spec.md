## MODIFIED Requirements

### Requirement: Push into the warehouse
Clicking the grid with a carried stack SHALL move it into the Input row, and shift clicking an inventory slot SHALL move that slot into the Input row; the Input row stores it. What does not fit into the Input row SHALL stay on the cursor or in the inventory slot. Stored items reach the warehouse's racks through a Packing Station and a shuttle put-away like any intake.

#### Scenario: Shift click to store
- **WHEN** the player shift clicks 20 cobblestone in the inventory while a Packing Station of a warehouse with a shuttle is on the line
- **THEN** the slot is empty and the warehouse holds 20 more cobblestone once a shuttle has put them away

#### Scenario: Only racks have room
- **WHEN** the player pushes bread and a rack has room
- **THEN** a shuttle puts the bread into the rack

### Requirement: Live stock while open
The terminal SHALL send its full content (stock, craftable items, requests, rules) when the game opens the screen, without waiting for the periodic update. The client SHALL keep the last content of each terminal for the session and SHALL show it at once when that terminal is opened again, replacing it when the fresh content arrives; the screen SHALL show a loading hint only while it has no content for that terminal yet. While the GUI is open the stock shown SHALL refresh within half a second after the index changes, without resending when nothing changed.

#### Scenario: Machine output arrives
- **WHEN** a machine's output reaches an Inbound Station and a shuttle puts 10 bread from it into a rack while the GUI is open
- **THEN** the bread count rises within a second

#### Scenario: Instant content
- **WHEN** the player opens the terminal
- **THEN** the stock is sent in the same tick the screen opens

#### Scenario: Opened again
- **WHEN** the player closes the terminal and opens it again in the same session
- **THEN** the last stock shows at once without a loading hint and is replaced by the fresh content

### Requirement: Request items
Clicking an entry SHALL open an amount prompt defaulting to one stack, with buttons for -stack, -10, -1, +1, +10 and +stack and a text field, and confirming SHALL create a request for that amount. +stack SHALL raise the amount to the next full stack above it and -stack SHALL lower it to the full stack below it; the amount SHALL stay between 1 and 99999. Shift clicking an entry SHALL request one stack at once, or the whole stock of that item when it holds less than one stack. A request SHALL be shipped through the Packing Station with the shortest shuttle trip that shares the terminal's data line: shuttles pick the items from the racks into that Packing Station, and they travel on as packets into the output slots. A request SHALL be refused with the reason when no Packing Station shares the line, when no shuttle can reach the racks holding the item (needs a shuttle), or when the Packing Station touches no rail. When stock falls short and a producer can make the item, the shortfall SHALL become a work order linked to the request.

#### Scenario: Request a stack
- **WHEN** the player shift clicks cobblestone, the warehouse holds 50 in a rack on the rails and has a garage with a shuttle
- **THEN** a request for 50 is made, a shuttle brings them to the Packing Station, 50 cobblestone arrive in the output slots as packets and nothing is left missing

#### Scenario: Request an amount
- **WHEN** the player clicks iron ingots, presses +10 three times and confirms, and the ingots sit in a rack beside a rail with a shuttle available
- **THEN** a request for 94 is listed, a shuttle fetches two loads to the Packing Station, and 94 ingots land in the output slots over time

#### Scenario: Stack steps
- **WHEN** the amount is 1 and the player presses +stack for an item that stacks to 64
- **THEN** the amount is 64, a second +stack makes it 128, and -stack from 70 makes it 64

#### Scenario: Never below one
- **WHEN** the amount is 64 and the player presses -stack, or the amount is 5 and the player presses -10
- **THEN** the amount is 1

#### Scenario: Rack needs a shuttle
- **WHEN** the player requests bread that sits in a rack and the warehouse has no garage
- **THEN** the request is refused and the GUI says the item needs a shuttle

#### Scenario: No Packing Station
- **WHEN** the terminal reaches a warehouse only through its controller and the warehouse has no Packing Station on that data line
- **THEN** the request is refused and the GUI says there is no Packing Station

#### Scenario: Partly in stock
- **WHEN** the player requests 100 bread and the warehouse holds 70
- **THEN** 70 are delivered and the request shows 30 missing until more arrives or it is cancelled

#### Scenario: Shortage ordered
- **WHEN** the player requests 8 sticks, none are in stock, and a Crafting Station on the line with a formed CPU can make sticks from a pattern
- **THEN** the request is listed as producing and the sticks arrive in the output slots once crafted

### Requirement: Output slots
The terminal SHALL have `terminal_output_slots` output slots (example 9, from `warehouse.json`, at most 9 shown) in one Output row below the Input row. Deliveries SHALL land there as packets and the player SHALL take them out by hand or shift click. The output slots SHALL NOT be indexed as warehouse stock. Hoppers, item pipes and other automation SHALL neither insert into nor extract from them. Breaking the terminal SHALL drop their contents.

#### Scenario: Collect
- **WHEN** packets have delivered 64 cobblestone into the output slots
- **THEN** the player shift clicks the slot and the cobblestone is in the inventory

#### Scenario: Not stock
- **WHEN** 64 cobblestone sit in the output slots
- **THEN** the stock view does not count them

#### Scenario: No extraction
- **WHEN** a hopper sits below a terminal holding cobblestone in its output slots
- **THEN** the hopper takes nothing

### Requirement: Terminal sits on the data line
The Storage Terminal SHALL be a member of a data line with the link role, and SHALL NOT join a warehouse network by touching rails, racks or controllers. A warehouse SHALL be reachable from the terminal when its controller or one of its Packing Stations is a member of the terminal's data network and the warehouse is online. The terminal's lamp SHALL be lit while at least one warehouse is reachable.

#### Scenario: Reached over the data line
- **WHEN** a terminal and a Packing Station of an online warehouse are linked by data ducts
- **THEN** the terminal lists the warehouse's stock and its lamp is lit

#### Scenario: Beside a controller only
- **WHEN** a terminal touches a controller but no data duct
- **THEN** the terminal reaches no warehouse and its lamp is dark

#### Scenario: Rail beside the terminal
- **WHEN** a rail is placed beside a terminal
- **THEN** the rail shows no dock or arm towards the terminal

### Requirement: JEI fills the grid
With JEI installed, the transfer button on a vanilla crafting recipe while the Storage Terminal is open SHALL open the crafting popup, move the grid contents into the Input row, which stores them, and fill the grid with the recipe's ingredients taken from the player inventory. Ingredients the player does not carry SHALL leave their grid slot empty. With max transfer the grid SHALL be filled for as many crafts as fit in a stack.

#### Scenario: Transfer sticks
- **WHEN** the player carries planks and presses JEI's transfer button on the stick recipe
- **THEN** the grid holds a plank above a plank taken from the player inventory and the result shows four sticks

#### Scenario: Warehouse stock is not used
- **WHEN** the player carries no planks, a rack holds planks and the player presses JEI's transfer button on the stick recipe
- **THEN** the grid stays empty and the rack keeps its planks

### Requirement: Input row
The Storage Terminal SHALL have an Input row of 9 slots below the item grid, filled by the player and by packets that no request of the terminal waits for. Stacks in the Input row SHALL be moved over the data line to the Packing Station on the terminal's data line nearest to the terminal, which puts them away like any intake (racks by shuttle, zones). Items SHALL leave the Input row only as fast as the Packing Station takes them: at most its conversion rate into storage and only while its To storage buffer has room, so the row shrinks over time and keeps what the station cannot take yet. Without a Packing Station on the line the stacks SHALL stay and the message line SHALL say that no Packing Station is on the data line. The input slots SHALL NOT be indexed as warehouse stock, also when the terminal touches a storage node or the controller, SHALL never be a put-away target or a source for picks, and SHALL be saved with the terminal and dropped when it breaks.

#### Scenario: Hand in cobblestone
- **WHEN** the player puts 20 cobblestone into an input slot while a Packing Station of the warehouse is on the line
- **THEN** the slot empties over a few seconds and the cobblestone ends in the warehouse after the put-away

#### Scenario: More than the station can hold
- **WHEN** the player fills all 9 input slots with cobblestone while the station's To storage buffer has room for one stack and no shuttle is free
- **THEN** one stack leaves the Input row and the other eight stay until the buffer has room

#### Scenario: No room
- **WHEN** the player puts bread into an input slot and no Packing Station is on the data line
- **THEN** the bread stays in the input slot and the message line says "No Packing Station on this data line"

#### Scenario: Racks only
- **WHEN** the warehouse has only Heavy Duty Racks with room and the player puts planks into an input slot
- **THEN** the planks end in a rack

#### Scenario: Terminal touching the controller
- **WHEN** the terminal stands directly beside the Warehouse Controller, its Output row holds 5 diamonds and the player puts 64 logs into the Input row of a warehouse whose only room is a rack
- **THEN** the stock shows no diamonds, all 64 logs end in the rack and the stock counts them once

#### Scenario: Nowhere to put it
- **WHEN** the player puts logs into the Input row and no storage of the warehouse has room for them
- **THEN** the Packing Station shows the blocked lamp and its status line says why, instead of the logs waiting silently

#### Scenario: Hopper above the terminal
- **WHEN** a hopper above the terminal holds cobblestone
- **THEN** the cobblestone stays in the hopper and the Input row is unchanged

## ADDED Requirements

### Requirement: Repeated crafting
Shift clicking the result of the crafting grid SHALL craft again and again while the grid still matches and the player inventory has room. The grid SHALL NOT refill from the warehouse: each craft uses up what lies in the grid.

#### Scenario: Shift craft
- **WHEN** the grid holds 3 planks above 3 planks and the player shift clicks the result
- **THEN** the player gets 12 sticks and the grid is empty

#### Scenario: No refill
- **WHEN** the grid holds one plank above one plank, the warehouse holds 100 planks and the player takes the result
- **THEN** the player gets four sticks, the grid is empty and the warehouse still holds 100 planks

### Requirement: Packets land in the terminal
Packets delivered to the terminal over the data line SHALL enter through its packet inlet: items an open request of the terminal still waits for SHALL land in the output slots, every other item in the Input row, which stores it. Items that do not fit SHALL wait at their source as the data line rules say. The terminal SHALL register no item capability, so item pipes show no item arm toward it.

#### Scenario: Delivery lands in the output
- **WHEN** a request waits for sticks and packets deliver sticks
- **THEN** the sticks land in the output slots

#### Scenario: Unrequested packets are stored
- **WHEN** packets deliver cobblestone that no request of the terminal waits for
- **THEN** the cobblestone lands in the Input row and goes on into the warehouse

#### Scenario: Item pipe beside the terminal
- **WHEN** a duct with the Item Pipe is placed beside a terminal
- **THEN** the duct shows no item arm toward the terminal

## REMOVED Requirements

### Requirement: Grid refills from the warehouse
**Reason**: The refill took ingredients from instant warehouse stock (crates and foreign inventories). With racks served only by shuttles there is no instant stock.
**Migration**: See "Repeated crafting": shift click still crafts while the grid matches; put more ingredients into the grid by hand.

### Requirement: Pull a stack to the cursor
**Reason**: Alt click took a stack from instant warehouse stock (crates and foreign inventories). With racks served only by shuttles there is no instant stock.
**Migration**: Request the item; it arrives in the output slots by shuttle and packets. Alt click on an entry does nothing; plain, shift, Ctrl and middle clicks keep their request and craft meanings.

### Requirement: Automation inserts
**Reason**: The terminal registers no item capability any more, so hoppers and ducts cannot reach it. Packets use the packet inlet instead.
**Migration**: Packets follow "Packets land in the terminal". Items from pipe lines go into an Inbound Station.
