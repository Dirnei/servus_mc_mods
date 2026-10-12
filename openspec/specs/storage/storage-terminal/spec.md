# storage/storage-terminal Specification

## Purpose

The Storage Terminal is where the player meets the warehouse: see everything, find it, take it, hand things in.

## Requirements

### Requirement: Stock view with search
The Storage Terminal GUI SHALL list every indexed item of the warehouses it reaches over its data line with its total in a scrollable grid, sorted by the player's sort setting (name, amount or mod, ascending or descending; amount descending by default). A search box SHALL filter the list by item name, case insensitive, as the player types.

#### Scenario: Search
- **WHEN** the warehouse holds cobblestone, iron ingots and bread and the player types "iron"
- **THEN** only iron ingots are shown

#### Scenario: Sort by name
- **WHEN** the warehouse holds 50 cobblestone, 10 apples and 20 bread and the sort is name ascending
- **THEN** the grid shows apple, bread, cobblestone in that order

#### Scenario: Sort by amount
- **WHEN** the same stock is sorted by amount descending
- **THEN** the grid shows cobblestone, bread, apple

### Requirement: Push into the warehouse
Clicking the grid with a carried stack SHALL move it into the Input row, and shift clicking an inventory slot SHALL move that slot into the Input row; the Input row stores it. What does not fit into the Input row SHALL stay on the cursor or in the inventory slot. Stored items reach racks, crates and foreign inventories through a Packing Station like any intake.

#### Scenario: Shift click to store
- **WHEN** the player shift clicks 20 cobblestone in the inventory while a Packing Station of the warehouse is on the line
- **THEN** the slot is empty and the warehouse holds 20 more cobblestone once they are put away

#### Scenario: Only racks have room
- **WHEN** the player pushes bread while every crate is full and a rack has room
- **THEN** a shuttle puts the bread into the rack

### Requirement: Live stock while open
The terminal SHALL send its full content (stock, craftable items, requests, rules) when the game opens the screen, without waiting for the periodic update. The client SHALL keep the last content of each terminal for the session and SHALL show it at once when that terminal is opened again, replacing it when the fresh content arrives; the screen SHALL show a loading hint only while it has no content for that terminal yet. While the GUI is open the stock shown SHALL refresh within half a second after the index changes, without resending when nothing changed.

#### Scenario: Machine output arrives
- **WHEN** a hopper puts 10 bread into a crate while the GUI is open
- **THEN** the bread count rises within a second

#### Scenario: Instant content
- **WHEN** the player opens the terminal
- **THEN** the stock is sent in the same tick the screen opens

#### Scenario: Opened again
- **WHEN** the player closes the terminal and opens it again in the same session
- **THEN** the last stock shows at once without a loading hint and is replaced by the fresh content

### Requirement: Offline shown
When no warehouse is reachable the GUI SHALL show the reason instead of the stock and SHALL refuse requests and pushes. Output slots SHALL stay usable so deliveries already made can be collected.

#### Scenario: No controller
- **WHEN** the terminal sits on a data line with no controller and no Packing Station of an online warehouse
- **THEN** the GUI says no warehouse is reachable

#### Scenario: Collect while offline
- **WHEN** the controller is broken after a delivery landed in the output slots
- **THEN** the player can still take the delivery out

### Requirement: Crafting
The Storage Terminal SHALL be crafted from glass, iron ingots, a chest and planks.

#### Scenario: Recipe
- **WHEN** the player crafts with the pattern
- **THEN** a Storage Terminal is produced

### Requirement: Request items
Clicking an entry SHALL open an amount prompt defaulting to one stack, with buttons for -stack, -10, -1, +1, +10 and +stack and a text field, and confirming SHALL create a request for that amount. +stack SHALL raise the amount to the next full stack above it and -stack SHALL lower it to the full stack below it; the amount SHALL stay between 1 and 99999. Shift clicking an entry SHALL request one stack at once, or the whole stock of that item when it holds less than one stack. A request SHALL be shipped through the Packing Station with the shortest shuttle trip that shares the terminal's data line: picks land in that Packing Station (instantly for stock no shuttle can reach in crates and foreign inventories, by shuttle for stock a shuttle can reach, never instantly from a rack) and travel on as packets into the output slots. A request SHALL be refused with the reason when no Packing Station shares the line, when its stock sits only in racks no shuttle can reach, or when it needs a shuttle while the Packing Station touches no rail. When stock falls short and a producer can make the item, the shortfall SHALL become a work order linked to the request.

#### Scenario: Request a stack
- **WHEN** the player shift clicks cobblestone, the warehouse holds 50 in a crate touching the controller and has no garage
- **THEN** a request for 50 is made, 50 cobblestone arrive in the output slots as packets shortly after and nothing is left missing

#### Scenario: Request an amount
- **WHEN** the player clicks iron ingots, presses +10 three times and confirms, and the ingots sit in a crate beside a rail with a shuttle available
- **THEN** a request for 94 is listed, a shuttle fetches two loads to the Packing Station, and 94 ingots land in the output slots over time

#### Scenario: Stack steps
- **WHEN** the amount is 1 and the player presses +stack for an item that stacks to 64
- **THEN** the amount is 64, a second +stack makes it 128, and -stack from 70 makes it 64

#### Scenario: Never below one
- **WHEN** the amount is 64 and the player presses -stack, or the amount is 5 and the player presses -10
- **THEN** the amount is 1

#### Scenario: Rack needs a shuttle
- **WHEN** the player requests bread that only sits in a rack and the warehouse has no garage
- **THEN** the request is refused and the GUI says the item needs a shuttle

#### Scenario: No Packing Station
- **WHEN** the terminal reaches a warehouse only through its controller and the warehouse has no Packing Station on that data line
- **THEN** the request is refused and the GUI says there is no Packing Station

#### Scenario: Partly in stock
- **WHEN** the player requests 100 bread and the warehouse holds 70
- **THEN** 70 are delivered and the request shows 30 missing until more arrives or it is cancelled

#### Scenario: Shortage ordered
- **WHEN** the player requests 8 sticks, none are in stock, and an Outbound Station faces a Crafting Station with the sticks recipe
- **THEN** the request is listed as producing and the sticks arrive in the output slots once crafted

### Requirement: Output slots
The terminal SHALL have `terminal_output_slots` output slots (example 9, from `warehouse.json`, at most 9 shown) in one Output row below the Input row. Deliveries SHALL land there and the player SHALL take them out by hand or shift click. The output slots SHALL NOT be indexed as warehouse stock. Automation SHALL only insert into them as the automation inserts requirement says; nothing SHALL be extracted from them by hoppers or ducts. Breaking the terminal SHALL drop their contents.

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
The terminal's open requests SHALL be the first entries of the item grid, before the stock entries and independent of the sort, drawn on a tinted background with the delivered and requested amounts and a mark for the state: picking while items are on their way to the Packing Station, sending while packets are on the line, waiting for room in the output slots, producing, or missing stock. Their tooltip SHALL name the item, the amounts and the state in words. The search filters them like stock; the view setting does not hide them. Clicking a request entry SHALL cancel it: picks not yet started are dropped, items already picked or at the Packing Station go back into storage, packets already on the line still land in the output slots, and a work order of the request is cancelled.

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

### Requirement: Stock rules from the terminal
The amount prompt SHALL have a Rule mode with minimum and maximum fields, Save and Remove, that sets or deletes the stock rule of the clicked item. Items with a rule SHALL show a marker in the stock grid and the rule with its state in their tooltip.

#### Scenario: Save a rule
- **WHEN** the player opens the prompt for sticks, switches to Rule, enters 8 and 16 and saves
- **THEN** the sticks entry shows a marker and the tooltip reads "keep 8 to 16"

#### Scenario: Remove
- **WHEN** the player opens the prompt for sticks in Rule mode and presses Remove
- **THEN** the marker and tooltip line are gone

### Requirement: Terminal sits on the data line
The Storage Terminal SHALL be a member of a data line with the link role, and SHALL NOT join a warehouse network by touching rails, crates, racks or controllers. A warehouse SHALL be reachable from the terminal when its controller or one of its Packing Stations is a member of the terminal's data network and the warehouse is online. The terminal's lamp SHALL be lit while at least one warehouse is reachable.

#### Scenario: Reached over the data line
- **WHEN** a terminal and a Packing Station of an online warehouse are linked by data ducts
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

### Requirement: Craft request
Start SHALL create a work order for the full amount on a Crafting CPU of the line, independent of the stock of the item itself, linked to a new request of the terminal that shows producing. The crafted items SHALL be put away and delivered into the output slots like any request. Cancelling the request SHALL cancel the order.

#### Scenario: Craft sticks
- **WHEN** the player starts crafting 8 sticks
- **THEN** a request for 8 sticks is listed as producing, and 8 sticks land in the output slots once crafted

### Requirement: Screen layout
The Storage Terminal screen SHALL have a column of view buttons left of the panel, the title with a Craft button and the search box, the item grid nine wide with its scrollbar, a message line, the Input row, the Output row, and the player inventory with the hotbar. Each SHALL have its own area that no other element overlaps; the crafting popup lies over the item grid only. A small arrow beside the Input row SHALL point up, into the warehouse, and one beside the Output row SHALL point down, to the player. The grid SHALL show as many rows as the height setting asks for, small, medium, tall or auto (as many as fit the screen), but never more than fit the screen, and the panel SHALL fit a 1080p screen at GUI scales 2, 3 and 4.

#### Scenario: Nothing overlaps
- **WHEN** the screen is open with requests in the grid and a message shown
- **THEN** no label, button, grid cell or slot overlaps another area

#### Scenario: Grid has its own area
- **WHEN** the screen is open
- **THEN** the input row lies above the output row, both below the item grid, and the crafting popup covers only the item grid

#### Scenario: Tall on a small screen
- **WHEN** the height is tall at GUI scale 4 on a 1080p screen
- **THEN** the grid shows only as many rows as fit and the panel stays on the screen

#### Scenario: Row arrows
- **WHEN** the screen is open
- **THEN** the arrow beside the Input row points up and the arrow beside the Output row points down

### Requirement: Requests survive a reload
The open requests of a Storage Terminal SHALL be saved with the terminal and SHALL be there again after the world is reloaded or the terminal's chunk loads again: item, requested, delivered and missing amounts, their shipments and the CPU holding their work order. Request ids SHALL stay unique across terminals and reloads. After a reload a request SHALL go on as before: shipments that still run are booked when they end, a missing part is shipped again, and a request whose work order no longer exists is retried as missing stock.

#### Scenario: Restart with a producing request
- **WHEN** a request for 8 sticks is producing on a CPU and the server restarts
- **THEN** the request is listed again after the restart and the sticks land in the terminal once the order is done

#### Scenario: Ids stay unique
- **WHEN** a terminal with request 5 is reloaded and a new request is made
- **THEN** the new request gets an id above every id used before

### Requirement: Content shows when the screen returns
The Storage Terminal screen SHALL show its terminal's last known content whenever it is shown, including when the same screen comes back after another screen was in front of it, and SHALL NOT fall back to "Loading..." while content for that terminal exists in the session.

#### Scenario: Back from the recipe viewer
- **WHEN** the player looks up a recipe in the recipe viewer from the Storage Terminal and returns
- **THEN** the stock and requests are shown at once

### Requirement: Right click clears the search
Right clicking the search box SHALL clear it and show the unfiltered list.

#### Scenario: Clear
- **WHEN** the search box holds "plank" and the player right clicks it
- **THEN** the box is empty and every entry is listed again

### Requirement: Crafted items arrive directly
While a request's work order is open, the terminal SHALL NOT ship the request's item from stock; the crafted items arrive in the output slots from the producer and count as delivered, and the missing amount SHALL shrink as they land. After the order ends, whatever is still missing SHALL be shipped from stock as before. Output slots that are full SHALL make the packets wait, shown as waiting for room.

#### Scenario: No second shipment
- **WHEN** an order for a request delivers all 8 sticks straight to the terminal
- **THEN** the request is done and no sticks are shipped from storage for it

#### Scenario: Shortfall after the order
- **WHEN** an order ends having delivered 6 of 8 sticks and 2 sticks are in stock
- **THEN** the terminal ships the 2 sticks from stock

### Requirement: Crafting grid
The crafting popup SHALL have a 3x3 crafting grid with a result slot. The result SHALL show the vanilla crafting recipe the grid matches, and taking it SHALL craft like a crafting table, using up one of each ingredient and leaving remainders such as empty buckets in the grid or the player inventory as vanilla does.

#### Scenario: Sticks by hand
- **WHEN** the player puts a plank above a plank into the grid and takes the result
- **THEN** the player gets four sticks and the planks are used up

### Requirement: Grid refills from the warehouse
After every craft, each grid slot that lost an ingredient SHALL be topped up with one of the same item from the instant stock of the warehouses the terminal reaches, crates and foreign inventories but not Heavy Duty Racks. Shift clicking the result SHALL craft again and again while the grid still matches and the player inventory has room.

#### Scenario: Shift craft from a crate
- **WHEN** the grid holds a plank above a plank, a crate on the line holds 6 planks and the player shift clicks the result
- **THEN** the player gets 16 sticks, the crate holds no planks and the grid is empty

#### Scenario: Rack stock is not used
- **WHEN** the only planks are in a Heavy Duty Rack and the player crafts the last sticks the grid can make
- **THEN** the grid stays empty and the rack keeps its planks

### Requirement: Pull a stack to the cursor
Alt clicking a stock entry with an empty cursor SHALL put up to one stack of that item from instant stock onto the cursor. With a carried stack or without instant stock nothing SHALL happen. Plain, shift, Ctrl and middle clicks on entries SHALL keep their request and craft meanings.

#### Scenario: Pull planks
- **WHEN** a crate holds 80 planks and the player Alt clicks the planks entry
- **THEN** the cursor holds 64 planks and the crate 16

### Requirement: Clear the grid
A clear button in the crafting popup SHALL move every grid stack into the Input row, which stores it; what does not fit SHALL go to the player inventory, and what still does not fit SHALL drop at the player.

#### Scenario: Clear
- **WHEN** the grid holds two planks and the player presses clear
- **THEN** the grid is empty and the two planks are stored through the Input row

### Requirement: Grid empties on close
When the terminal screen closes, the grid contents SHALL go into the Input row, which stores them, and what does not fit into the player inventory.

#### Scenario: Close
- **WHEN** the grid holds a plank and the player closes the screen
- **THEN** the plank is stored through the Input row

### Requirement: JEI fills the grid
With JEI installed, the transfer button on a vanilla crafting recipe while the Storage Terminal is open SHALL open the crafting popup, move the grid contents back to the warehouse and fill the grid with the recipe's ingredients, taking each from instant warehouse stock first and from the player inventory second. Ingredients found in neither SHALL leave their grid slot empty. With max transfer the grid SHALL be filled for as many crafts as fit in a stack.

#### Scenario: Transfer sticks
- **WHEN** a crate holds planks and the player presses JEI's transfer button on the stick recipe
- **THEN** the grid holds a plank above a plank taken from the crate and the result shows four sticks

### Requirement: View settings
The side column SHALL have buttons, each with an icon and a tooltip naming its current value, for the sort (name, amount, mod), the direction (ascending, descending), the view (all, stock, craftable), the search mode (normal, JEI synced, JEI two way) and the height (small, medium, tall, auto). The settings SHALL be saved per player on the client, SHALL be the same for every Storage Terminal and SHALL be kept across restarts.

#### Scenario: Settings kept
- **WHEN** the player sets the sort to mod and the height to tall, closes the game and opens another Storage Terminal later
- **THEN** that terminal sorts by mod and is tall

### Requirement: JEI search sync
With JEI installed, the search mode JEI synced SHALL copy the terminal's search text into JEI's search as the player types, and JEI two way SHALL also copy JEI's search text into the terminal's search box when it changes there. Without JEI the search mode button SHALL only offer normal.

#### Scenario: Synced
- **WHEN** the search mode is JEI synced and the player types "iron" in the terminal
- **THEN** JEI's search shows "iron"

#### Scenario: No JEI
- **WHEN** JEI is not installed
- **THEN** the search mode stays normal and the terminal works unchanged

### Requirement: Input row
The Storage Terminal SHALL have an Input row of 9 slots below the item grid. Stacks in the Input row SHALL be moved over the data line to the Packing Station on the terminal's data line nearest to the terminal, which puts them away like any intake (racks by shuttle, zones, crates and foreign inventories). Items SHALL leave the Input row only as fast as the Packing Station takes them: at most its conversion rate into storage and only while its To storage buffer has room, so the row shrinks over time and keeps what the station cannot take yet. Without a Packing Station on the line the stacks SHALL stay and the message line SHALL say that no Packing Station is on the data line. The input slots SHALL NOT be indexed as warehouse stock and SHALL be saved with the terminal and dropped when it breaks.

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

### Requirement: Automation inserts
A hopper, duct or packet inserting into the terminal SHALL put items an open request of the terminal still waits for into the output slots, and every other item into the input slots, which store it. Nothing SHALL be extracted from the terminal by automation.

#### Scenario: Hopper hands in
- **WHEN** a hopper above the terminal holds cobblestone and no request waits for cobblestone
- **THEN** the cobblestone goes through the input slots into the warehouse

#### Scenario: Delivery lands in the output
- **WHEN** a request waits for sticks and packets deliver sticks
- **THEN** the sticks land in the output slots

### Requirement: Crafting popup
A Craft button on the title row SHALL open a popup over the item grid with the 3x3 crafting grid, its arrow, result slot and clear button. While the popup is closed its slots SHALL NOT take clicks or show. Closing the popup SHALL store the grid contents like the clear button. JEI's transfer button SHALL open the popup before filling the grid.

#### Scenario: Open and craft
- **WHEN** the player presses Craft, puts a plank above a plank into the popup grid and takes the result
- **THEN** the player gets four sticks

#### Scenario: Close stores
- **WHEN** the popup grid holds two planks and the player closes the popup
- **THEN** the grid is empty and the warehouse holds two more planks
