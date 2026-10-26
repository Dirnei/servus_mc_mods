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

### Requirement: Output slots
The terminal SHALL have `terminal_output_slots` output slots (example 9, from `warehouse.json`, at most 9 shown) in one Output row below the Input row. Each open request SHALL own as many output slots as its amount needs at the item's stack size, as far as they are free and at least one, from the moment it is made until it ends; deliveries for a request SHALL land only in its slots as packets, and the terminal SHALL grant a delivery only the room left in them. The player SHALL take them out by hand or shift click at any time. The output slots SHALL NOT be indexed as warehouse stock. Hoppers, item pipes and other automation SHALL neither insert into nor extract from them. Breaking the terminal SHALL drop their contents.

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
- **WHEN** a request for 94 iron ingots owns one slot because only one was free, and that slot holds 64 the player has not taken
- **THEN** the other 30 wait in the warehouse, the slot shows waiting for room, and they land in the same slot once the player takes the 64

#### Scenario: Two stacks in two slots
- **WHEN** the player requests 128 copper and the Output row has free slots
- **THEN** the request owns two slots and all 128 copper arrive without the player taking anything out

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

### Requirement: Stock rules from the terminal
The item popup SHALL have a Rule tab that sets or deletes the stock rule of its item, and the Rules tab SHALL list every rule. Items with a rule SHALL show a marker in the stock grid and the rule with its state in their tooltip.

#### Scenario: Save a rule
- **WHEN** the player right clicks sticks, picks the Rule tab, sets 8 and 16 and saves
- **THEN** the sticks entry shows a marker and the tooltip reads "keep 8 to 16"

#### Scenario: Remove
- **WHEN** the player opens the Rule tab for sticks and presses Remove
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

### Requirement: Craftable entries
The stock grid SHALL also list every item a producer on the terminal's data line can make, from the patterns of the line and the recipes producers report. A craftable item with nothing requestable SHALL show "Craft" instead of a count; a craftable item that is requestable SHALL show its count and a craftable marker. A view button in the side column SHALL cycle the grid between All, Stock and Craftable.

#### Scenario: Sticks craftable
- **WHEN** no sticks are in stock and a sticks pattern and a Crafting Station are on the terminal's data line
- **THEN** the grid shows sticks with "Craft"

#### Scenario: Filter
- **WHEN** the player sets the view to Craftable
- **THEN** only craftable items are shown, stocked or not

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

### Requirement: Craft request
Start SHALL create a work order for the full amount on a Crafting CPU of the line, independent of the stock of the item itself, linked to a new request of the terminal that takes a free output slot and shows producing. The crafted items SHALL land in that slot like any request. Cancelling the request SHALL cancel the order. Start SHALL be refused with the reason when no output slot is free.

#### Scenario: Craft sticks
- **WHEN** the player starts crafting 8 sticks
- **THEN** a request for 8 sticks shows producing on an output slot, and 8 sticks land in that slot once crafted

#### Scenario: No free slot for a craft
- **WHEN** every output slot is taken and the player presses Start in the craft dialog
- **THEN** no order is made and the dialog says the output row is full

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
The Storage Terminal SHALL have an Input row of 9 slots below the item grid, filled by the player and by packets that no request of the terminal waits for. Stacks in the Input row SHALL be moved over the data line to the Packing Station the warehouse chooses for them, which puts them away like any intake (racks by shuttle, zones). Each input slot SHALL send only what a station has granted it, at most the station's conversion rate into storage, so the row shrinks over time and keeps what no station can take yet; the message line SHALL say why a slot waits. An input slot whose stack is on its way SHALL take no more items until its move ends, so a stack handed in meanwhile, by click, shift click or packet, goes into another slot and gets a grant of its own. Without a Packing Station on the line the stacks SHALL stay and the message line SHALL say that no Packing Station is on the data line. The input slots SHALL NOT be indexed as warehouse stock, also when the terminal touches a storage node or the controller, SHALL never be a put-away target or a source for picks, and SHALL be saved with the terminal and dropped when it breaks.

#### Scenario: Hand in cobblestone
- **WHEN** the player puts 20 cobblestone into an input slot while a Packing Station of the warehouse is on the line
- **THEN** the slot empties over a few seconds and the cobblestone ends in the warehouse after the put-away

#### Scenario: More than the station can hold
- **WHEN** the player fills all 9 input slots with cobblestone while the station's To storage buffer has one free slot and no shuttle is free
- **THEN** one stack leaves the Input row and the other eight stay until the buffer has a free slot again

#### Scenario: Different items do not share slots
- **WHEN** the player puts a stack of cobblestone, logs and sand into the Input row while the station has three free To storage slots
- **THEN** each stack gets a slot of its own and each arrives whole in its slot

#### Scenario: Second stack while the first leaves
- **WHEN** the player shift clicks a stack of apples into the Input row, and a second stack of apples while the first is still leaving
- **THEN** the first slot is not topped up, the second stack takes a slot of its own, and the apples arrive as two whole stacks in two To storage slots

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
- **THEN** the logs stay in the Input row and the message line says that no storage takes them, instead of the logs waiting silently

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

### Requirement: Terminal shows warehouse power
While a warehouse the terminal reaches is in the no power state, the terminal SHALL show a line in the alert colour saying the warehouse has no power and requests wait. The stock SHALL stay shown and requests SHALL still be accepted; since every item leaves the warehouse by shuttle, an open request that waits for a shuttle or a Crafting Station SHALL show the state waiting for power until the warehouse has power again. A transfer from another site whose wake ended for lack of power SHALL show waiting for power and stay cancellable.

#### Scenario: Request without power
- **WHEN** the warehouse is in the no power state and the player requests iron that sits only in a rack
- **THEN** the request is listed as waiting for power and the iron arrives after FE comes back

#### Scenario: Stock still shown
- **WHEN** the warehouse is in the no power state and the player opens the terminal
- **THEN** the grid shows the stock and the no power line is shown

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
The order grid SHALL have a 3x3 grid of ghost cells and, after an arrow, the result of the vanilla crafting recipe the cells match, drawn as a bare item icon without a slot frame, with the count one craft makes and the item's normal tooltip; the grid, arrow, result and Save button SHALL be centred in the panel. The result SHALL be what the recipe makes from the cells, with the components the recipe adds (for example the wood of the planks), the same stack the amount panel's preview and a saved pattern have. Clicking a cell with an item on the cursor SHALL set a single ghost copy of it, dragging the cursor's item over cells SHALL set a copy in each cell it passes, and clicking with an empty cursor SHALL clear the cell; the cursor and the player inventory keep their items. No real items SHALL lie in the order grid.

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

#### Scenario: Rack with its wood
- **WHEN** the grid holds iron in the four corners and three spruce planks across the middle row, no rack pattern exists and a Pattern Storage with room is on the line
- **THEN** the result shows three Heavy Duty Racks whose tooltip names spruce wood, and the Save button shows

#### Scenario: Rack can be started
- **WHEN** that grid is in the order grid, the warehouse holds iron and spruce planks, and the player presses Next and sets the amount to 3
- **THEN** From stock lists 4 iron ingots and 3 spruce planks, To craft lists 3 spruce Heavy Duty Racks and Start is enabled

### Requirement: JEI fills the order grid
With JEI installed, the transfer button on a vanilla crafting recipe while the Storage Terminal is open SHALL open the order grid and set its ghost cells to the recipe's ingredients. For each cell it SHALL choose, among the stacks the warehouse can give out that the cell's ingredient accepts, the one the warehouse has the most of, with that stack's components (for example a Rail with its wood); only when the warehouse has none of them SHALL the cell take the ingredient's first item. It SHALL take nothing from the player inventory or the warehouse. While no formed Crafting CPU and Crafting Station are on the terminal's data line the transfer button SHALL be refused with that reason. JEI's recipe view in front of the terminal SHALL NOT make the terminal forget what its line has.

#### Scenario: Transfer sticks
- **WHEN** the player presses JEI's transfer button on the stick recipe while the warehouse holds birch planks only
- **THEN** the grid shows a birch plank above a birch plank, the result shows four sticks and the inventory is unchanged

#### Scenario: Stocked item with its wood
- **WHEN** the warehouse holds 40 oak Rails and 10 spruce Rails and the player presses the transfer button on the Elevator recipe
- **THEN** the centre cell shows an oak Rail with its wood, the result shows oak Elevators and the amount panel takes the Rail from stock

#### Scenario: Nothing stocked
- **WHEN** the warehouse holds no planks and the player presses the transfer button on the stick recipe
- **THEN** each plank cell shows the first plank the recipe accepts

#### Scenario: No CPU
- **WHEN** the terminal's data line has no formed Crafting CPU and the player looks at the stick recipe in JEI
- **THEN** the transfer button is refused with a tooltip saying a Crafting CPU and a Crafting Station are needed

#### Scenario: Recipe view opened from the terminal
- **WHEN** a formed CPU and a Crafting Station are on the line, the player opens JEI's recipe view for sticks from the terminal and presses the transfer button
- **THEN** the button is not refused and the order grid opens with the recipe

### Requirement: Request beyond stock is crafted
When the amount asked is above the requestable count and a Crafting CPU on the line can craft the rest with nothing missing, the Request tab SHALL say how many come from stock and how many are crafted, with the server's craft preview, and Request SHALL ship the requestable part and order the rest on the same request. While the rest cannot be crafted the Request tab SHALL say why and allow no more than the requestable count.

#### Scenario: Ten from stock, the rest crafted
- **WHEN** 10 sticks are requestable, a sticks pattern, a Crafting Station and a CPU are on the line with 40 planks in stock, and the player asks for 64 sticks
- **THEN** the Request tab says 10 from stock and 54 crafted, and Request lists one request for 64 that ships 10 and is producing the rest

#### Scenario: Rest cannot be crafted
- **WHEN** 10 sticks are requestable and no planks are in stock, and the player types 64
- **THEN** the Request tab lists planks as missing, Request is disabled until the amount is 10 or less

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

### Requirement: Output row full
A new request, transfer, craft request or order grid start SHALL get output slots that are free: no open request owns them and they hold nothing, or they hold the same item with room left. It SHALL get as many as its amount needs, as far as they are free, and at least one. A slot that still holds items of a request that ended SHALL stay taken until it is emptied. When no slot is free the terminal SHALL refuse with "Output row full, take your items"; adding to an open request does not need a new slot.

#### Scenario: Ten requests
- **WHEN** nine requests for different items are open and the player clicks a tenth item
- **THEN** no request is made and the message line says "Output row full, take your items"

#### Scenario: Room after taking
- **WHEN** every slot is taken, one holds the 16 cobblestone of a finished request and the player takes them out
- **THEN** the next click on an item makes a request in that slot

#### Scenario: Adding needs no slot
- **WHEN** every slot is taken, one by an open request for cobblestone, and the player clicks cobblestone
- **THEN** that request grows by one and nothing is refused

#### Scenario: Fewer free slots than stacks
- **WHEN** two slots are free and the player requests 256 cobblestone
- **THEN** the request owns those two slots and the rest arrives as the player empties them

### Requirement: Request slots survive a reload
The output slots a request owns SHALL be saved with the request and SHALL be the same after the world is reloaded or the terminal's chunk loads again.

#### Scenario: Restart with two requests
- **WHEN** a request for bread owns slot 1 and a request for iron owns slots 4 and 5 and the server restarts
- **THEN** after the restart bread still shows on slot 1 and iron on slots 4 and 5, and their deliveries land there

### Requirement: Slot choice is pure
Choosing the output slots for a new request (same item with room first, then the leftmost empty slots that no request owns, as many as the amount needs at the stack size, at least one, else none) SHALL be plain Java without Minecraft classes and covered by unit tests.

#### Scenario: Same item first
- **WHEN** slot 0 is empty and free and slot 3 holds 16 cobblestone of an ended request
- **THEN** a new cobblestone request gets slot 3

#### Scenario: Owned slots are skipped
- **WHEN** slot 0 is owned by an open request and holds nothing, and slot 1 is empty and free
- **THEN** a new request gets slot 1

#### Scenario: Several slots
- **WHEN** slots 0 to 8 are empty and free and a request is for 130 cobblestone
- **THEN** it gets slots 0, 1 and 2

### Requirement: Dragging the grid scrollbar
Pressing the left mouse button on the item grid's scrollbar while no dialog is open SHALL scroll the grid so the thumb centres on the mouse, as far as the grid can scroll. Moving the mouse with the button held SHALL keep the thumb under the mouse, also when the mouse leaves the bar, until the button is released. The wheel SHALL keep scrolling as before.

#### Scenario: Drag to the end
- **WHEN** the grid lists more items than fit and the player presses on the thumb and drags it to the bottom of the bar
- **THEN** the grid shows its last rows and the thumb sits at the bottom of the bar

#### Scenario: Jump on press
- **WHEN** the player presses the left button halfway down the scrollbar of a grid with 20 rows to scroll
- **THEN** the grid jumps to about row 10

#### Scenario: Nothing to scroll
- **WHEN** all items fit in the grid and the player drags on the scrollbar
- **THEN** the grid stays where it is

### Requirement: Clicking away from the search box
The search box SHALL have focus when the Storage Terminal opens. A press of any mouse button anywhere in the screen that is not on the search box SHALL take the focus from it, keep its text, and still do what the press does there. Without focus the box SHALL take no keys, so the inventory key closes the terminal and the number keys move items to the hotbar. A click on the box SHALL give it focus again.

#### Scenario: Click on the grid
- **WHEN** the player has typed "plank" in the search box and clicks an item in the grid
- **THEN** the item is requested, the search box loses focus and still holds "plank"

#### Scenario: Close with the inventory key
- **WHEN** the player clicks on an empty part of the panel after typing in the search box and then presses the inventory key
- **THEN** the terminal closes

#### Scenario: Hotbar key on a slot
- **WHEN** the player clicks a slot of the player inventory while the search box has focus, then hovers a slot and presses 1
- **THEN** that slot's stack swaps with the first hotbar slot and no "1" is typed in the box

#### Scenario: Back into the box
- **WHEN** the search box has no focus and the player clicks it
- **THEN** it has focus and takes typed text again

### Requirement: Recipe viewer keys on grid items
With JEI installed, the item under the mouse in the Storage Terminal's grid SHALL count as hovered for JEI, so its show recipes and show uses keys open that item's recipes and uses while the search box has no focus.

#### Scenario: Recipes of a stocked item
- **WHEN** the player hovers Oak Planks in the grid with the search box unfocused and presses the show recipes key (R by default)
- **THEN** JEI shows the recipes that make Oak Planks

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
