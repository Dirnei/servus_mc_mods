## MODIFIED Requirements

### Requirement: Which slots take locks
A slot lock SHALL reserve one exact slot of a block for one shuttle job and SHALL name its direction, IN for a slot the job fills and OUT for a slot the job empties, the item, the amount and the job. Slots that take locks SHALL be the slots of Heavy Rack bays, the Inbound Station buffer, the Outbound Station buffer, both buffers of a Packing Station (To storage and To network) and the three parts of a Field Head buffer (seeds, fertilizer, anti-corrosion items). Other storage, foreign inventories and Fluid Gantry tanks SHALL take no locks.

A Field Head buffer part is a count, not a slot: its locks SHALL reserve instead of closing it. An IN lock SHALL reserve room for its amount and an OUT lock SHALL reserve its amount of the items, one part MAY hold IN and OUT locks of different jobs at once, and every other job and field shuttle SHALL see the part's room and items less what other jobs reserved.

#### Scenario: Chest beside a station
- **WHEN** a chest feeds an Inbound Station and a put-away is planned from the station
- **THEN** the station slot is locked and the chest has no lock

#### Scenario: Seeds on the way while the harvester takes some
- **WHEN** a head's seed part holds 20 wheat seeds of a level of 64 and a supply run has locked room for 44 more, and the harvester wants 10 seeds
- **THEN** the harvester locks and takes 10 seeds from the part, and the supply run still fills the part with its 44

#### Scenario: Reserved room is not filled by others
- **WHEN** a head's seed part holds 20 of 64 and a supply run has locked room for 44, and a harvester comes back with spare seeds
- **THEN** none of the spare seeds go into the part and they are put away instead

### Requirement: The GUI shows locks
Locked slots SHALL show a colour wash, one colour for IN and another for OUT, in the rack section GUI and the Inbound, Outbound and Packing Station GUIs. An empty IN locked slot SHALL show the incoming item faded. Hovering a locked slot SHALL show a tooltip naming the job: the label of the shuttle on the job and what it brings or takes ("Reserved: Shuttle #12 brings 64 Cobblestone", "Reserved: Bertha (#7) takes 40 Bread"), or that it waits for a shuttle when none is on the job yet. In the Field Head GUI the buffer cell of a part with a lock on the Stock tab SHALL show the wash, and its tooltip SHALL add a line per lock in the same words.

#### Scenario: Incoming stack
- **WHEN** a player opens a rack section with an empty slot IN locked for 64 cobblestone of a queued put-away
- **THEN** the slot shows faded cobblestone under the IN wash and the tooltip says it waits for a shuttle to bring 64 Cobblestone

#### Scenario: Shuttle on its way
- **WHEN** Shuttle #12 drives to take 40 bread from an OUT locked station slot
- **THEN** the slot shows the OUT wash over the bread and the tooltip reads "Reserved: Shuttle #12 takes 40 Bread"

#### Scenario: Supply on its way to the head
- **WHEN** Shuttle #3 is on a supply run that locked room for 44 wheat seeds in a head's buffer and a player opens the head's Stock tab
- **THEN** the seed buffer cell shows the IN wash and its tooltip reads "Reserved: Shuttle #3 brings 44 Wheat Seeds"
