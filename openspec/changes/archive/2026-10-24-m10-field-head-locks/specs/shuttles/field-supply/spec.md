## MODIFIED Requirements

### Requirement: Supply run
A supply run SHALL plan, for each part of the head's buffer, what it is short of up to its level: seeds of the crop, the fertilizer only above the head's reserve, and the head's anti-corrosion item only above its anti-corrosion reserve, as far as its cargo has room, counting only stock no other job reserved. When it is planned it SHALL lock those items in the racks holding them, cheapest first by travel cost from the head's rail (OUT), and the room it fills in the head's buffer (IN), for what it found. It SHALL drive to those racks, riding the lift where needed, take exactly what it locked there, then drive to the farm rail beside the head and fill the buffer as its job. A rack it cannot reach any more SHALL lose that run's locks. Items in the buffer that are no longer the field's crop, fertilizer or anti-corrosion item SHALL be taken along and put away. What it carries after the head SHALL be put away like the harvest (see `shuttles/harvester`), then it SHALL go home and its remaining locks SHALL be released.

#### Scenario: Stock behind a lift
- **WHEN** the only bone meal lies in a rack on another floor reached only by an elevator shaft, and a field with bone meal fertilizing has an empty buffer
- **THEN** a shuttle without a tool rides the lift, brings bone meal into the head's buffer, and a Spreader then doses the field

#### Scenario: Reserve kept
- **WHEN** a head's reserve is 10, its fertilizer buffer is empty, `fertilizer_buffer` is 64 and the warehouse holds 30 bone meal
- **THEN** the supply run brings 20 bone meal and 10 stay in the rack

#### Scenario: Crop changed
- **WHEN** a head's buffer holds wheat seeds and the player sets carrots
- **THEN** the next supply run brings carrots and takes the wheat seeds back to storage

#### Scenario: Locked when planned
- **WHEN** a supply run is planned for 44 wheat seeds from a rack holding 64
- **THEN** the rack slot holds an OUT lock for 44 wheat seeds and the head's seed part an IN lock for 44, both naming the run, before the shuttle leaves its garage

#### Scenario: Reserved stock is not taken twice
- **WHEN** the only rack holds 30 bone meal, 30 of which are OUT locked for a pick, and a head's fertilizer part is empty
- **THEN** no supply run takes bone meal and the pick still gets its 30

#### Scenario: Nothing lost when the cargo is full
- **WHEN** a supply run takes items from a rack that its cargo cannot hold and the rack has no room to take them back
- **THEN** the items ride along and are put away, and none vanish or drop
