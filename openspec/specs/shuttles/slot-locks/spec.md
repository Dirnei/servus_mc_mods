# shuttles/slot-locks Specification

## Purpose
Slot locks make every shuttle job a promise: the slots a job will fill or empty are reserved for it from the moment it is planned until it is done, so no other job, player, pipe or mod can take its room or its items and nothing is left over.

## Requirements

### Requirement: Which slots take locks
A slot lock SHALL reserve one exact slot of a block for one shuttle job and SHALL name its direction, IN for a slot the job fills and OUT for a slot the job empties, the item, the amount and the job. Slots that take locks SHALL be the slots of Heavy Rack bays, the Inbound Station buffer, the Outbound Station buffer and both buffers of a Packing Station (To storage and To network). Other storage, foreign inventories and Fluid Gantry tanks SHALL take no locks.

#### Scenario: Chest beside a station
- **WHEN** a chest feeds an Inbound Station and a put-away is planned from the station
- **THEN** the station slot is locked and the chest has no lock

### Requirement: A job locks the slots it fills or empties
Locks SHALL be placed when a job is planned. A put-away SHALL lock the station slots it takes from (OUT) and the bay slots it fills (IN). A pick SHALL lock the items it takes in the bay (OUT) and one empty slot of its destination buffer (IN). A pick whose destination has no free slot then SHALL lock one as soon as one is free and SHALL NOT be sent before.

#### Scenario: Put-away locks both ends
- **WHEN** 40 cobblestone in an Inbound Station slot are planned into a bay
- **THEN** that station slot holds an OUT lock for 40 cobblestone and the bay holds IN locks for 40 cobblestone, both naming the same job

#### Scenario: Pick locks both ends
- **WHEN** a Packing Station ships 64 iron ingots picked from a bay
- **THEN** the bay slots holding those ingots are OUT locked for 64 and one empty To network slot is IN locked for 64 iron ingots

#### Scenario: Destination full at planning
- **WHEN** every To network slot of a Packing Station is in use and a pick for it is planned
- **THEN** the pick waits in the queue with its bay slots locked, and is sent once a To network slot is free and locked for it

### Requirement: Picks may share a slot
A slot SHALL hold either one IN lock or the OUT locks of one or more jobs that together take no more than it holds.

#### Scenario: Two picks from one slot
- **WHEN** a bay slot holds 64 cobblestone and two picks of 40 and 24 are planned
- **THEN** the slot holds an OUT lock for each and a third pick finds nothing left

### Requirement: More of an item joins an open put-away
More of an item arriving at a station SHALL join a put-away of that item from the station that no shuttle has loaded yet, up to a stack per job and as far as its bay has room, instead of becoming a job of its own.

#### Scenario: Buffer filling item by item
- **WHEN** 16 planks arrive at a Packing Station one by one while the first shuttle is on its way
- **THEN** they join one put-away of 16 planks instead of sixteen put-aways of one

### Requirement: A locked slot is closed to everyone but its job
A locked slot SHALL refuse every insert and extract except those of its own job: clicks, drags, shift clicks and number keys in any GUI, hoppers, item pipes, the stations' own Pull and Push sides, wireless arrivals, packet inlets and the item handler capability as other mods use it. Unlocked slots of the same block SHALL work as before.

#### Scenario: Hand blocked
- **WHEN** a player opens the rack section GUI and clicks a slot OUT locked for a pick
- **THEN** nothing lands on the cursor and the slot keeps its items

#### Scenario: Shift click passes locked slots
- **WHEN** a player shift clicks cobblestone into a rack section whose only slot with cobblestone is IN locked
- **THEN** the cobblestone goes into an unlocked slot or stays in the inventory, never into the locked slot

#### Scenario: Pull side cannot top up
- **WHEN** an Inbound Station slot with 20 bread is OUT locked and its Pull side offers more bread
- **THEN** the locked slot keeps 20 bread and the new bread goes into a free unlocked slot

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
Locked slots SHALL show a colour wash, one colour for IN and another for OUT, in the rack section GUI and the Inbound, Outbound and Packing Station GUIs. An empty IN locked slot SHALL show the incoming item faded. Hovering a locked slot SHALL show a tooltip naming the job: the label of the shuttle on the job and what it brings or takes ("Reserved: Shuttle #12 brings 64 Cobblestone", "Reserved: Bertha (#7) takes 40 Bread"), or that it waits for a shuttle when none is on the job yet.

#### Scenario: Incoming stack
- **WHEN** a player opens a rack section with an empty slot IN locked for 64 cobblestone of a queued put-away
- **THEN** the slot shows faded cobblestone under the IN wash and the tooltip says it waits for a shuttle to bring 64 Cobblestone

#### Scenario: Shuttle on its way
- **WHEN** Shuttle #12 drives to take 40 bread from an OUT locked station slot
- **THEN** the slot shows the OUT wash over the bread and the tooltip reads "Reserved: Shuttle #12 takes 40 Bread"
