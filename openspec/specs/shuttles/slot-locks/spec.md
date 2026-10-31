# shuttles/slot-locks Specification

## Purpose
Slot locks make every shuttle job a promise: the slots a job will fill or empty are reserved for it from the moment it is planned until it is done, so no other job, player, pipe or mod can take its room or its items and nothing is left over.

## Requirements

### Requirement: Which slots take locks
A slot lock SHALL reserve one exact slot of a block for one shuttle job and SHALL name its direction, IN for a slot the job fills and OUT for a slot the job empties, the item, the amount and the job. Slots that take locks SHALL be the slots of Heavy Rack bays, the Inbound Station buffer, the Outbound Station buffer, both buffers of a Packing Station (To storage and To network) and the three parts of a Field Head buffer (seeds, fertilizer, anti-corrosion items). Other storage, foreign inventories and Fluid Gantry tanks SHALL take no locks.

A Field Head buffer part is a count, not a slot: its locks SHALL reserve instead of closing it. An IN lock SHALL reserve room for its amount and an OUT lock SHALL reserve its amount of the items, one part MAY hold IN and OUT locks of different jobs at once, and every other job and field shuttle SHALL see the part's room and items less what other jobs reserved.

The slots of a Packing Station's To network buffer SHALL reserve in the same way: a slot MAY hold IN locks of several picks of one item, the station SHALL be able to send the items a slot holds while picks still have room reserved there, and every other insert SHALL see the slot's room less what the picks reserved.

A slot that reserves instead of closing SHALL take no other item than the one its IN locks reserve room for, while any of them is open.

#### Scenario: Chest beside a station
- **WHEN** a chest feeds an Inbound Station and a put-away is planned from the station
- **THEN** the station slot is locked and the chest has no lock

#### Scenario: Seeds on the way while the harvester takes some
- **WHEN** a head's seed part holds 20 wheat seeds of a level of 64 and a supply run has locked room for 44 more, and the harvester wants 10 seeds
- **THEN** the harvester locks and takes 10 seeds from the part, and the supply run still fills the part with its 44

#### Scenario: Reserved room is not filled by others
- **WHEN** a head's seed part holds 20 of 64 and a supply run has locked room for 44, and a harvester comes back with spare seeds
- **THEN** none of the spare seeds go into the part and they are put away instead

#### Scenario: Sending past a pick on its way
- **WHEN** a To network slot holds 10 coal and a pick on its way has room for 5 more coal reserved there, and the shipment of the coal is granted room at its target
- **THEN** the station sends the 10 coal, and the 5 coal of the pick still land in that slot

#### Scenario: Reserved slot takes no other item
- **WHEN** an empty To network slot has room reserved for 8 coal by a pick on its way and a wireless arrival brings iron ingots
- **THEN** the iron ingots go into another slot, never into the reserved one

### Requirement: A job locks the slots it fills or empties
Locks SHALL be placed when a job is planned. A put-away SHALL lock the station slots it takes from (OUT) and the bay slots it fills (IN). A pick SHALL lock the items it takes in the bay (OUT) and room in its destination buffer (IN): empty slots only at a terminal's Output row, an Outbound Station or a bay; at a Packing Station's To network buffer room on top of a stack of its item that has room first, counting what other picks on their way already bring there, then empty slots. A pick whose destination has no room for it then SHALL lock it as soon as there is room and SHALL NOT be sent before.

#### Scenario: Put-away locks both ends
- **WHEN** 40 cobblestone in an Inbound Station slot are planned into a bay
- **THEN** that station slot holds an OUT lock for 40 cobblestone and the bay holds IN locks for 40 cobblestone, both naming the same job

#### Scenario: Pick locks both ends
- **WHEN** a Packing Station with an empty To network buffer ships 64 iron ingots picked from a bay
- **THEN** the bay slots holding those ingots are OUT locked for 64 and one empty To network slot is IN locked for 64 iron ingots

#### Scenario: Pick joins its item
- **WHEN** a To network slot holds 3 coal and a pick of 2 coal for the Packing Station is sent
- **THEN** that slot is IN locked for the 2 coal and no empty slot is locked

#### Scenario: Pick joins a pick on its way
- **WHEN** an empty To network slot is IN locked for 3 coal of a pick on its way and a second pick of 2 coal is sent
- **THEN** the same slot gets an IN lock for the 2 coal as well, and once both arrived it holds 5 coal

#### Scenario: Stack nearly full
- **WHEN** a To network slot holds 60 coal and a pick of 10 coal is sent
- **THEN** that slot is IN locked for 4 coal and an empty slot for 6 coal

#### Scenario: Destination full at planning
- **WHEN** no To network slot of a Packing Station has room for iron ingots and a pick of iron ingots for it is planned
- **THEN** the pick waits in the queue with its bay slots locked, and is sent once room for it is free and locked for it

### Requirement: Picks may share a slot
A slot that closes when locked SHALL hold either one IN lock or the OUT locks of one or more jobs that together take no more than it holds. A slot that reserves instead of closing SHALL hold any locks whose IN amounts together fit its room and whose OUT amounts together take no more than it holds.

#### Scenario: Two picks from one slot
- **WHEN** a bay slot holds 64 cobblestone and two picks of 40 and 24 are planned
- **THEN** the slot holds an OUT lock for each and a third pick finds nothing left

#### Scenario: Two picks into one To network slot
- **WHEN** a To network slot holds 50 coal and picks of 8 and 6 coal are sent to the Packing Station
- **THEN** the slot holds an IN lock for each, and a third pick of 4 coal locks an empty slot

### Requirement: More of an item joins an open put-away
More of an item arriving at a station SHALL join a put-away of that item from the station that no shuttle has loaded yet, up to a stack per job and as far as its bay has room, instead of becoming a job of its own. At an Inbound Station the more SHALL arrive in the job's own slot when it has room, and the job's OUT lock in that slot SHALL grow by what joined.

#### Scenario: Buffer filling item by item
- **WHEN** 16 planks arrive at a Packing Station one by one while the first shuttle is on its way
- **THEN** they join one put-away of 16 planks instead of sixteen put-aways of one

#### Scenario: Inbound slot grows
- **WHEN** an Inbound Station slot holds 8 cobblestone OUT locked for a put-away no shuttle has loaded and 8 more cobblestone arrive
- **THEN** the slot holds 16 cobblestone, its OUT lock is for 16 and the bay's IN lock for that job grows by 8

#### Scenario: Job full
- **WHEN** the job already takes 64 cobblestone and more cobblestone arrives
- **THEN** the new cobblestone goes into another slot and becomes a put-away of its own

### Requirement: A locked slot is closed to everyone but its job
A locked slot SHALL refuse every insert and extract except those of its own job: clicks, drags, shift clicks and number keys in any GUI, hoppers, item pipes, the stations' own Pull and Push sides, wireless arrivals, packet inlets and the item handler capability as other mods use it. One exception: an Inbound Station slot that holds only OUT locks of put-aways no shuttle has loaded yet SHALL take inserts of its own item up to the stack size, from every inserter including the station GUI, and still refuse every extract. Slots that reserve instead of closing (Field Head buffer parts, the To network buffer) SHALL follow "Which slots take locks" instead. Unlocked slots of the same block SHALL work as before.

#### Scenario: Hand blocked
- **WHEN** a player opens the rack shelf GUI and clicks a slot OUT locked for a pick
- **THEN** nothing lands on the cursor and the slot keeps its items

#### Scenario: Shift click passes locked slots
- **WHEN** a player shift clicks cobblestone into a rack shelf whose only slot with cobblestone is IN locked
- **THEN** the cobblestone goes into an unlocked slot or stays in the inventory, never into the locked slot

#### Scenario: Pull side tops up a planned slot
- **WHEN** an Inbound Station slot with 20 bread is OUT locked for a put-away no shuttle has loaded yet and its Pull side offers more bread
- **THEN** the locked slot fills up to 64 bread and only the bread beyond that goes into a free unlocked slot

#### Scenario: Pull side cannot top up
- **WHEN** an Inbound Station slot with 20 bread is IN locked for a pipe grant and its Pull side offers more bread
- **THEN** the locked slot keeps 20 bread and the new bread goes into a free unlocked slot

#### Scenario: Other items stay out
- **WHEN** that slot holds 20 bread OUT locked and a hopper offers carrots
- **THEN** the carrots go into another slot

#### Scenario: Rest after loading
- **WHEN** the shuttle on the put-away has loaded at the station and the slot still holds bread it did not take
- **THEN** that bread is a new stack of its own, planned as a new put-away

#### Scenario: Pipe blocked
- **WHEN** an item pipe tries to pull from an Outbound Station whose only filled slot is locked
- **THEN** the pipe takes nothing from that slot

### Requirement: Locks end with their job
An OUT lock SHALL be released when its job loads the items at that block, an IN lock when its job unloads there. When a job ends without that (refused, cancelled, its request dropped, its cargo dropped by a recall) its locks SHALL be released. A job that goes back to the queue keeps its locks.

#### Scenario: Unload releases
- **WHEN** a shuttle unloads 40 cobblestone into the bay slots locked for its job
- **THEN** those slots hold the cobblestone and are unlocked

### Requirement: A broken block ends its locks
Breaking a block SHALL end the locks it holds. Put-aways that were to fill it SHALL be routed again when they get there; picks that were to empty it SHALL be planned again from other bays or count as missing. A zone change on a bay SHALL NOT end its IN locks.

#### Scenario: Bay broken under a pick
- **WHEN** the bay a queued pick takes from is broken and another bay holds the item
- **THEN** the pick is planned again from the other bay and the shipment still gets its items

#### Scenario: Zone change keeps the lock
- **WHEN** a bay with an IN lock for raw copper is stamped with a zone that does not take copper
- **THEN** the shuttle still brings the copper into that bay

### Requirement: Locks survive a reload
Locks SHALL be saved with the block that holds them and SHALL come back when it loads. While the warehouse is online and every chunk of its site is loaded, a lock whose job is neither queued nor on a shuttle at two checks in a row SHALL be released. A block saved without locks, such as one from Shuttles 0.3.0, SHALL load with every slot unlocked.

#### Scenario: Reload mid job
- **WHEN** a pick's bay slots are locked and the server restarts before the shuttle arrives
- **THEN** after the restart the slots are still locked for that pick and the shuttle takes the items

#### Scenario: Orphan lock
- **WHEN** a bay loads with a lock whose job no longer exists
- **THEN** after a few seconds the lock is released and the slot can be used again

### Requirement: The GUI shows locks
Locked slots SHALL show a colour wash, one colour for IN and another for OUT, in the rack shelf GUI and the Inbound, Outbound and Packing Station GUIs. An empty IN locked slot SHALL show the incoming item faded. Hovering a locked slot SHALL show a tooltip naming the job: the label of the shuttle on the job and what it brings or takes ("Reserved: Shuttle #12 brings 64 Cobblestone", "Reserved: Bertha (#7) takes 40 Bread"), or that it waits for a shuttle when none is on the job yet. In the Field Head GUI the buffer cell of a part with a lock on the Stock tab SHALL show the wash, and its tooltip SHALL add a line per lock in the same words.

#### Scenario: Incoming stack
- **WHEN** a player opens a rack shelf with an empty slot IN locked for 64 cobblestone of a queued put-away
- **THEN** the slot shows faded cobblestone under the IN wash and the tooltip says it waits for a shuttle to bring 64 Cobblestone

#### Scenario: Shuttle on its way
- **WHEN** Shuttle #12 drives to take 40 bread from an OUT locked station slot
- **THEN** the slot shows the OUT wash over the bread and the tooltip reads "Reserved: Shuttle #12 takes 40 Bread"

#### Scenario: Supply on its way to the head
- **WHEN** Shuttle #3 is on a supply run that locked room for 44 wheat seeds in a head's buffer and a player opens the head's Stock tab
- **THEN** the seed buffer cell shows the IN wash and its tooltip reads "Reserved: Shuttle #3 brings 44 Wheat Seeds"
