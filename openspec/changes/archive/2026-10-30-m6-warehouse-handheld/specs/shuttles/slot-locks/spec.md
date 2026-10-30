# Spec Delta

## MODIFIED Requirements

### Requirement: A locked slot is closed to everyone but its job
A locked slot SHALL refuse every insert and extract except those of its own job: clicks, drags, shift clicks and number keys in any GUI, hoppers, item pipes, the stations' own Pull and Push sides, wireless arrivals, packet inlets and the item handler capability as other mods use it. One exception: an Inbound Station slot that holds only OUT locks of put-aways no shuttle has loaded yet SHALL take inserts of its own item up to the stack size, from every inserter including the station GUI, and still refuse every extract. Unlocked slots of the same block SHALL work as before.

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
