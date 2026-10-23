# shuttles/storage-terminal Specification

## Purpose

The Storage Terminal is where the player meets the warehouse: see everything, find it, take it, hand things in.

## Requirements

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

### Requirement: Push into the warehouse
Clicking the grid with a carried stack SHALL move it into the Input row, and shift clicking an inventory slot SHALL move that slot into the Input row; the Input row stores it. What does not fit into the Input row SHALL stay on the cursor or in the inventory slot. Stored items reach the warehouse's racks through a Packing Station and a shuttle put-away like any intake.

#### Scenario: Shift click to store
- **WHEN** the player shift clicks 20 cobblestone in the inventory while a Packing Station of a warehouse with a shuttle is on the line
- **THEN** the slot is empty and the warehouse holds 20 more cobblestone once a shuttle has put them away

#### Scenario: Only racks have room
- **WHEN** the player pushes bread and a rack has room
- **THEN** a shuttle puts the bread into the rack

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

### Requirement: Stock rules from the terminal
The amount prompt SHALL have a Rule mode with minimum and maximum fields, Save and Remove, that sets or deletes the stock rule of the clicked item. Items with a rule SHALL show a marker in the stock grid and the rule with its state in their tooltip.

#### Scenario: Save a rule
- **WHEN** the player opens the prompt for sticks, switches to Rule, enters 8 and 16 and saves
- **THEN** the sticks entry shows a marker and the tooltip reads "keep 8 to 16"

#### Scenario: Remove
- **WHEN** the player opens the prompt for sticks in Rule mode and presses Remove
- **THEN** the marker and tooltip line are gone

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

### Requirement: Several warehouses on one line
When more than one warehouse is reachable, the stock view SHALL show the sum of their stock. A request SHALL be served first by the warehouse holding the most of the item, then by the next, until the amount is covered or the stock runs out. Work orders for a shortfall SHALL go to a Crafting CPU on the terminal's data line; stock rules set in the terminal and pushes SHALL use the first reachable warehouse, the one whose controller has the lowest position.

#### Scenario: Merged stock
- **WHEN** one reachable warehouse holds 30 cobblestone and another 20
- **THEN** the terminal lists 50 cobblestone

#### Scenario: Split request
- **WHEN** the player requests 40 of that cobblestone
- **THEN** 30 come from the first warehouse and 10 from the second, and 40 land in the output slots

### Requirement: Craftable entries
The stock grid SHALL also list every item a producer on the terminal's data line can make, from the patterns of the line and the recipes producers report. A craftable item with nothing requestable SHALL show "Craft" instead of a count; a craftable item that is requestable SHALL show its count and a craftable marker. A view button in the side column SHALL cycle the grid between All, Stock and Craftable.

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

### Requirement: Other sites in the terminal
The Storage Terminal's title SHALL be a site selector only while the controller of the first warehouse the terminal reaches is wireless capable. "This site" shows the stock of the warehouses the terminal reaches as before; clicking cycles through the other sites of the same team in the same dimension whose controller holds a Wireless Upgrade, powered or not, by name, and back. While this site's controller is not wireless capable the title SHALL show only this site, clicking it SHALL do nothing, and a terminal that showed another site SHALL go back to "This site". An online site SHALL show its live stock; a sleeping site SHALL show its last known stock with every entry greyed out and a line saying when it can wake or that it cannot be ordered from. A site whose controller has no power SHALL show a line in the alert colour saying it has no power for wireless. While another site is shown, the craft list and stock rules SHALL apply to nothing; the open requests and the Input and Output rows stay those of this terminal. While another site is shown the Input row SHALL be closed: no click, shift click, carried stack or grid clear puts items into it, it is drawn greyed with a grey arrow, and hovering it says that it is closed while another site is shown, because what is handed in would be stored at this site. No text of the terminal SHALL use the word team.

#### Scenario: Pick the outpost
- **WHEN** a base terminal is open, the base controller is wireless capable, the outpost's controller holds a Wireless Upgrade and the player clicks the title
- **THEN** the title reads the outpost's name and the grid shows the outpost's stock

#### Scenario: This site only
- **WHEN** the base controller has no Wireless Upgrade and the player clicks the terminal's title
- **THEN** the title stays on this site and no other site is offered

#### Scenario: Power lost while viewing
- **WHEN** the terminal shows the outpost and the base controller runs out of FE
- **THEN** the terminal goes back to "This site" and the title offers no other site until FE arrives

#### Scenario: Input closed for another site
- **WHEN** the terminal shows the outpost and the player shift clicks logs from the inventory
- **THEN** the logs stay in the inventory and the Input row stays empty; back on "This site" the same shift click hands them in

#### Scenario: Sleeping outpost
- **WHEN** the outpost is asleep with 40 iron ingots last known
- **THEN** the grid shows 40 iron ingots greyed out

### Requirement: Transfers from another site
Requesting an item while another site is shown SHALL make a transfer to this site: the other site ships the items from one of its Packing Stations to a Packing Station of the warehouse this terminal reaches first that reaches the terminal by data line, and that station passes them on as packets into the terminal's output slots like a request from this site's storage; they SHALL NOT be put away at this site. The request SHALL be refused with the reason when this site's controller is not wireless capable, when this site has no Packing Station, when the other site's controller is not wireless capable, when the other site has no Packing Station or sleeps without enough Anchor Upgrades to wake, or when the other site, online, refuses the shipment. A sleeping site that can wake SHALL be woken and held awake until the transfer ends or is cancelled. The transfer SHALL be listed with the open requests with its delivered and requested amounts and a state (waking, picking, sending, waiting for power, missing) and SHALL end when the items arrived in the terminal's output slots; clicking it SHALL cancel it like a request. Transfers SHALL survive a reload like requests.

#### Scenario: Transfer from an online outpost
- **WHEN** the player requests 16 iron ingots of the online outpost at the base terminal, both controllers are wireless capable and both sites have a Packing Station
- **THEN** a transfer for 16 is listed, the iron arrives in the terminal's output slots, the base's storage does not change and the transfer leaves the list

#### Scenario: Output full
- **WHEN** a transfer from the outpost arrives while the terminal's output slots are full
- **THEN** the items wait in the receiving Packing Station like packets of a local request and move on once the player empties the output

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

### Requirement: Terminal shows warehouse power
While a warehouse the terminal reaches is in the no power state, the terminal SHALL show a line in the alert colour saying the warehouse has no power and requests wait. The stock SHALL stay shown and requests SHALL still be accepted; since every item leaves the warehouse by shuttle, an open request that waits for a shuttle or a Crafting Station SHALL show the state waiting for power until the warehouse has power again. A transfer from another site whose wake ended for lack of power SHALL show waiting for power and stay cancellable.

#### Scenario: Request without power
- **WHEN** the warehouse is in the no power state and the player requests iron that sits only in a rack
- **THEN** the request is listed as waiting for power and the iron arrives after FE comes back

#### Scenario: Stock still shown
- **WHEN** the warehouse is in the no power state and the player opens the terminal
- **THEN** the grid shows the stock and the no power line is shown

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
