# shuttles/inbound-station Specification

## Purpose

The Inbound Station is the warehouse's inbound dock: items arrive in its buffer and the warehouse puts them away by its zones, carried by shuttles over the rails when the warehouse has them.

## Requirements

### Requirement: Station block and buffer
The Inbound Station SHALL be a block with a horizontal facing and an intake buffer of `inbound_buffer_slots` slots (example 9, from `data/<ns>/shuttles/warehouse.json`). Every relative side but the front SHALL be in one of three modes of the Lib side configuration, Input, Pull or Off, all Input by default; the front is fixed to Off. On Input and Pull sides the buffer SHALL be exposed as an item handler that accepts inserts and refuses extraction, so hoppers, item pipes and machines feed it but never take from it, and item pipes show an item arm toward those sides. On Pull sides the station SHALL additionally take stacks out of the neighbour's item handler every `push_interval` ticks while the buffer has room, taking only what its open slots accept and never losing an item between the neighbour and the buffer. The station's Pull sides SHALL fill slots that already hold the item before they use an empty slot. Off sides SHALL expose nothing. A slot reserved by a pipe grant SHALL take items only from that grant, and a slot whose put-away a shuttle has not loaded yet SHALL take more of its own item and nothing else. The Configuration Carrot used on a face SHALL cycle that face's mode and the GUI SHALL show the Lib side panel. Breaking the station SHALL drop the buffer contents.

#### Scenario: Hopper feeds the station
- **WHEN** a hopper above the station holds 20 cobblestone
- **THEN** the cobblestone moves into the station's buffer

#### Scenario: Item pipe feeds the station
- **WHEN** a duct with the Item Pipe beside the station carries cobblestone toward it
- **THEN** the duct shows an item arm toward the station and the cobblestone moves into the buffer

#### Scenario: Hopper cannot drain it
- **WHEN** a hopper under the station points into a chest and the buffer holds bread
- **THEN** the bread stays in the buffer

#### Scenario: Side switched off
- **WHEN** the player uses the carrot on the top face twice and a hopper above offers cobblestone
- **THEN** the cobblestone stays in the hopper until the top is set to Input again, and a duct on that face shows no item arm

#### Scenario: Pull from a chest
- **WHEN** the player sets the back side to Pull and a chest behind the station holds 30 bread
- **THEN** the bread moves into the buffer and on into the warehouse without a hopper

#### Scenario: Pull into a nearly full buffer
- **WHEN** a Pull side offers 64 cobblestone while the open slots take only 10
- **THEN** 10 cobblestone move into the buffer and 54 stay in the neighbour

#### Scenario: Pull fills the partial slot first
- **WHEN** slot 1 is empty, slot 4 holds 20 bread whose put-away no shuttle has loaded yet, and a Pull side offers 30 bread
- **THEN** slot 4 takes 30 bread, holds 50 and slot 1 stays empty

#### Scenario: Hopper trickle fills one slot
- **WHEN** a hopper above the station hands in 64 cobblestone one by one while no shuttle has come yet
- **THEN** the 64 cobblestone are in one slot

#### Scenario: Front closed
- **WHEN** a machine pushes against the station's front
- **THEN** nothing goes in and the carrot on the front only reports Off

#### Scenario: Broken with contents
- **WHEN** the station is broken while the buffer holds 10 bread
- **THEN** 10 bread drop at its position

### Requirement: Put-away by shuttle
A buffer stack SHALL be put away only by shuttle. When the warehouse is online, has shuttles and a home rail, and both the station and the chosen rack touch rails the home rail reaches, a buffer stack SHALL become a put-away job: its slot is OUT locked and the room for it in the target rack is IN locked (see `shuttles/slot-locks`). A free shuttle SHALL drive to the station's rail, take the locked amount, drive to the rack's rail, unload into the locked slots, and return home. The stack SHALL stay in the buffer until the shuttle loads it; nobody SHALL take it out meanwhile and it SHALL NOT be offered to a second shuttle. More of the same item arriving in that slot before the shuttle loads it SHALL join the job, up to a stack per job, and what is left in the slot after loading SHALL become a new put-away. Cargo SHALL never return to the station and never drop: what a rack does not take on arrival SHALL be routed again by the put-away routing, and with no room anywhere the shuttle SHALL park it in its garage (see `shuttles/shuttle-garage`). Without a shuttle that can do this, the stack SHALL wait and the station SHALL report that it needs a shuttle. The buffer SHALL stay untouched while the warehouse is offline.

#### Scenario: Shuttle put-away
- **WHEN** a station beside a rail receives 40 cobblestone and the only rack with room stands beside a rail eight blocks further
- **THEN** the cobblestone leaves the buffer only when a shuttle arrives, is carried along the rail, and lands in the rack when the shuttle gets there

#### Scenario: Buffer shows the wait
- **WHEN** three stacks arrive and one shuttle exists
- **THEN** the stacks leave the buffer one at a time as the shuttle comes back for each

#### Scenario: Taken out before loading
- **WHEN** the player tries to take the stack out of the buffer GUI before the shuttle arrives
- **THEN** the stack stays in its slot and the shuttle takes it

#### Scenario: Topped up before loading
- **WHEN** 8 cobblestone wait in a slot for the shuttle on its way and a hopper adds 24 more cobblestone before it arrives
- **THEN** the 32 cobblestone are in that one slot and the shuttle takes all 32 in one trip

#### Scenario: Arriving after loading
- **WHEN** the shuttle has taken 32 cobblestone from the slot and a hopper adds 8 more
- **THEN** the 8 cobblestone become a new put-away

#### Scenario: Target filled meanwhile
- **WHEN** a player tries to fill the target rack by hand while the shuttle is on its way
- **THEN** the slots locked for the shuttle stay free and the cobblestone lands there

#### Scenario: Station pulling fast
- **WHEN** a chest and a backpack on Pull sides keep the buffer full while shuttles put away into a nearly full rack
- **THEN** every item ends in a rack or waits in the buffer, and nothing drops at the station or the rack

#### Scenario: No garage
- **WHEN** a station receives bread, a rack has room and the warehouse has no garage
- **THEN** the bread waits in the buffer, the lamp turns red and the GUI says the bread needs a shuttle

#### Scenario: Offline
- **WHEN** the station belongs to a network with no controller and receives cobblestone
- **THEN** the cobblestone stays in the buffer

#### Scenario: Routed to its zone
- **WHEN** a rack is zoned "Ores" with `c:ores` and raw iron arrives at the station
- **THEN** a shuttle puts the raw iron into that rack

### Requirement: Backed up station
A stack that the routing cannot place anywhere SHALL wait in the buffer. The station SHALL carry a second lamp beside the connection lamp that shows what it is doing: green while the buffer is empty, orange while stacks are being put away or wait for a shuttle on its way, red while any stack has no room, and dark while the warehouse is offline. The station GUI SHALL name the first waiting stack and the reason while the lamp is red: no room in its zone and in general storage, no storage at all, or no shuttle.

#### Scenario: Zone and general storage full
- **WHEN** raw copper waits because every "Aisle 3" rack and every unzoned rack is full
- **THEN** the activity lamp is red and the GUI reads that raw copper has no room in "Aisle 3" or in general storage

#### Scenario: Room appears
- **WHEN** a new rack is stamped "Aisle 3" while raw copper waits
- **THEN** a shuttle puts the copper away and the activity lamp goes back to green

#### Scenario: Shuttle on its way
- **WHEN** a stack waits in the buffer for the shuttle that was sent for it
- **THEN** the activity lamp is orange and turns green once the buffer is empty

### Requirement: Station GUI
Right clicking the station SHALL open a GUI with the buffer slots and the player inventory. Idle and working are shown by the lamps on the block, not in the GUI; the GUI SHALL show a line only while the warehouse is offline, naming the reason, or while a stack has no room, naming the stack and the reason. Shift clicking an inventory stack SHALL move it into the buffer. Zones are defined on Zone Labels and seen on the blocks; the station does not list them.

#### Scenario: Shift click into the buffer
- **WHEN** the player shift clicks 20 cobblestone in the inventory
- **THEN** the cobblestone is in the buffer and is put away as if the station had pulled it

#### Scenario: Offline
- **WHEN** the station touches a controller but no rail
- **THEN** the GUI says the warehouse has no controller

#### Scenario: Waiting stack
- **WHEN** raw copper waits because its zone and general storage are full
- **THEN** the GUI names raw copper and the reason

#### Scenario: Nothing to say
- **WHEN** the station is online and nothing waits
- **THEN** the GUI shows only the buffer and the inventory

### Requirement: Station is obtainable and dismantled
The station SHALL be craftable from vanilla materials including planks, recording the wood of the planks, SHALL drop itself with its wood when broken, and SHALL be dismantled by sneaking with the Configuration Carrot, dropping its buffer contents and keeping its side modes and wood in the dropped item. The facing is set at placement; the carrot without sneaking configures sides and does not turn the station.

#### Scenario: Crafting
- **WHEN** the player places iron ingots, a chest, a hopper and planks in the station pattern (for example a chest framed by ingots over a hopper between planks)
- **THEN** a Inbound Station carrying the wood of the planks is produced

#### Scenario: Dismantle
- **WHEN** the player sneaks and uses the Configuration Carrot on a station holding bread
- **THEN** the station and the bread drop as items

### Requirement: Inbound Station joins by rail and holds no stock
The station SHALL be a member of the warehouse network only through rails: rails SHALL dock onto it on any side and link it, and it SHALL NOT link to touching controllers, storage blocks, racks, garages, other stations or elevator segments, so it never passes the warehouse connection on to a neighbour. Its buffer SHALL NOT be indexed and inventories touching it SHALL NOT become warehouse sources. The station SHALL show the connection lamp like storage blocks, lit only while a rail links it to an online warehouse.

#### Scenario: Chest feeding the station is not stock
- **WHEN** a chest with 30 bread touches only the station of an online warehouse on an Off side
- **THEN** the terminal does not list the bread

#### Scenario: Buffer is not stock
- **WHEN** the buffer holds 10 bread that has not been put away yet
- **THEN** the terminal does not list those 10 bread

#### Scenario: Station links an aisle
- **WHEN** a rail runs from a controller to a station and on to a rack front
- **THEN** the rack, the station and the controller form one online warehouse

#### Scenario: Touching the controller is not enough
- **WHEN** a station touches the controller and no rail touches the station
- **THEN** the station is not part of that warehouse, its lamp is dark and items in its buffer stay there

#### Scenario: Rail placed later
- **WHEN** a rail is then placed between the station and the controller
- **THEN** the station joins the warehouse and its lamp lights; breaking that rail takes it out again

#### Scenario: Rack touching only the station
- **WHEN** a rack touches only a station that is linked by rail and no rail stands in front of the rack
- **THEN** the rack is not part of the warehouse

### Requirement: Intake stops at a rule's cap
An online Inbound Station SHALL take an item through its Input and Pull sides only while the warehouse holds less than the cap of that item's stock rule. Held counts the indexed stock, every Inbound buffer, put-aways on shuttles and what open work orders still make for storage. An insert or pull SHALL take at most the difference to the cap; the rest stays with the sender. Items without a cap, and items put in through the station GUI, SHALL be taken as before.

#### Scenario: Hopper at the cap
- **WHEN** cobblestone has a cap of 4096, the warehouse holds 4090 and a hopper above the station offers 64 cobblestone
- **THEN** 6 cobblestone move into the buffer and the rest stays in the hopper

#### Scenario: Duct falls back
- **WHEN** cobblestone is at its cap and a duct carries cobblestone with the station at priority 1 and a Sink at priority 0 on the same channel
- **THEN** the station takes none and the duct delivers the cobblestone to the Sink

#### Scenario: Pull side at the cap
- **WHEN** the back side is Pull, a chest behind it holds 30 bread, bread has a cap of 100 and the warehouse holds 90
- **THEN** 10 bread move into the buffer and 20 stay in the chest

#### Scenario: Waiting stacks count
- **WHEN** cobblestone has a cap of 128, the warehouse holds 64 and two buffers hold 64 cobblestone waiting for a shuttle
- **THEN** the station takes no more cobblestone

#### Scenario: Open orders count
- **WHEN** sticks have a cap of 10, the warehouse holds none, an open work order still makes 8 sticks for storage and a duct pulls sticks from a chest into the station
- **THEN** 2 sticks move into the buffer and the rest stays in the chest

#### Scenario: Hand insert ignores the cap
- **WHEN** cobblestone is at its cap and the player shift clicks 20 cobblestone into the station GUI
- **THEN** the cobblestone is in the buffer and is put away

#### Scenario: Other items pass
- **WHEN** cobblestone is at its cap and a hopper offers raw iron, which has no rule
- **THEN** the raw iron moves into the buffer

#### Scenario: Offline station
- **WHEN** the station is offline and a hopper offers cobblestone that has a cap in another warehouse
- **THEN** the station takes the cobblestone as before and it waits in the buffer

### Requirement: Pipe offers are granted one slot
An online Inbound Station SHALL answer an item pipe's offer on its Input and Pull sides with a grant that reserves one empty slot for at most a stack of the item, no more than the offer and than the item's cap leaves room for. It SHALL refuse while offline, when no storage of the warehouse takes the item, when no slot is free or when the cap leaves no room. Off sides and the front SHALL answer no offer.

#### Scenario: A chest of cobblestone
- **WHEN** a pipe pulling from a chest with 200 cobblestone offers them to the station
- **THEN** the station grants 64 and reserves one empty slot for them

#### Scenario: Less than a stack
- **WHEN** the chest holds 20 cobblestone
- **THEN** the grant is 20 and one slot is reserved

#### Scenario: Buffer full
- **WHEN** every slot holds items or is reserved
- **THEN** the station refuses and the pipe sends as without a grant, into what room is left

#### Scenario: Offline
- **WHEN** the station has no controller
- **THEN** it refuses every offer and takes items from the pipe as before

### Requirement: Put-away waits for a pipe grant
A slot reserved by a pipe grant SHALL NOT be put away while the grant runs. Once the grant is filled or has ended, the slot SHALL be put away like any buffer stack, so one shuttle trip takes what the grant brought.

#### Scenario: One stack, one slot, one trip
- **WHEN** a pipe pulls 64 cobblestone from a chest into the station at its usual rate and a shuttle is free
- **THEN** the cobblestone fills one buffer slot, no shuttle is sent while it arrives, and once all 64 are in the slot one shuttle takes them in one trip

#### Scenario: Three stacks
- **WHEN** the chest holds 192 cobblestone
- **THEN** the cobblestone uses three buffer slots one after the other and makes three put-away trips of 64, not 24 slots of 8

#### Scenario: Chest runs dry
- **WHEN** a grant for 64 logs is open and the chest is emptied by a player after 40 arrived
- **THEN** the grant ends and the 40 logs are put away in one trip

#### Scenario: Pipe broken
- **WHEN** the duct is broken while its grant has brought 30 of 64 cobblestone
- **THEN** the grant ends and the 30 cobblestone are put away

### Requirement: Granted amounts count against the cap
For the cap of a stock rule, what open grants of the warehouse's Inbound Stations still expect SHALL count as held, like the stacks in their buffers. A grant SHALL never reserve more than the cap leaves room for.

#### Scenario: Grant at the cap
- **WHEN** cobblestone has a cap of 4096, the warehouse holds 4070 and a pipe offers 64 cobblestone
- **THEN** the grant is 26 cobblestone

#### Scenario: Open grant blocks a hopper
- **WHEN** cobblestone has a cap of 128, the warehouse holds 64 and a pipe grant still expects 64 cobblestone
- **THEN** a hopper above the station hands in no cobblestone

#### Scenario: Grant ends short
- **WHEN** that grant ends after 10 cobblestone arrived
- **THEN** the other 54 no longer count and the hopper hands in up to 54 cobblestone

### Requirement: The GUI shows what a pipe brings
A slot reserved by a pipe grant SHALL show the IN wash and the faded item, and its tooltip SHALL say that a pipe brings the rest of the grant, for example "Reserved: pipe brings 56 more Cobblestone".

#### Scenario: Half filled grant slot
- **WHEN** a grant for 64 cobblestone has brought 8 and a player hovers the slot
- **THEN** the slot shows 8 cobblestone under the IN wash and the tooltip reads "Reserved: pipe brings 56 more Cobblestone"

### Requirement: Pipe grants are not kept over a reload
An Inbound Station SHALL save no grants. After its block entity loads again it SHALL hold no grant reservations, the items already in a grant's slot SHALL be put away, and a pipe still pulling SHALL offer again.

#### Scenario: Restart while a pipe fills a slot
- **WHEN** 30 of 64 granted cobblestone are in the slot when the server stops
- **THEN** after the restart the 30 cobblestone are put away and the pipe gets a new grant for the cobblestone still in the chest
