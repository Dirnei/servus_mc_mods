# Spec Delta

## ADDED Requirements

### Requirement: FIND basket
The FIND screen SHALL hold a basket of items with amounts, at most a fixed number of entries (example 9), each entry one item. Clicking an item in the list SHALL add it with an amount the player sets, shift clicking SHALL add one stack, and an item already in the basket SHALL raise that entry's amount. The player SHALL be able to change an entry's amount, remove an entry and clear the basket. Adding a new item to a full basket SHALL be refused with a note.

#### Scenario: Build a basket
- **WHEN** a player on the FIND screen adds 128 cobblestone and then shift clicks bread
- **THEN** the basket lists 128 cobblestone and 64 bread

#### Scenario: Same item again
- **WHEN** the basket holds 64 bread and the player adds 32 bread
- **THEN** the basket holds one bread entry of 96

#### Scenario: Basket full
- **WHEN** the basket has 9 entries and the player adds a tenth item
- **THEN** the item is not added and the screen says the basket is full

### Requirement: Warehouse basket and shelf basket
A handheld SHALL have two baskets: the warehouse basket, shown while FIND lists the whole warehouse and kept for later requests, and the shelf basket, shown while FIND lists a scanned shelf. Closing the GUI and logging out SHALL keep both. Only the player SHALL empty the warehouse basket. The shelf basket SHALL be emptied when the list leaves its shelf: by ALL, by scanning another shelf, or by switching the handheld's mode.

#### Scenario: Warehouse basket survives
- **WHEN** a player builds a warehouse basket, closes the GUI, switches to ZONE and back to FIND and opens the GUI again
- **THEN** the warehouse basket shows the same entries

#### Scenario: Shelf basket kept on close
- **WHEN** a player builds a shelf basket, closes the GUI and opens it again on the same shelf
- **THEN** the shelf basket shows the same entries

#### Scenario: Leaving the shelf empties it
- **WHEN** a player builds a shelf basket and presses ALL
- **THEN** the list shows the whole warehouse with the warehouse basket, and the shelf basket is empty

### Requirement: Back to the whole warehouse
While the FIND screen lists a scanned shelf, an ALL button SHALL switch the list back to the bound warehouse's stock; without a scanned shelf or a binding it SHALL be inactive.

#### Scenario: From a shelf to the warehouse
- **WHEN** a bound player scanned a shelf in FIND, opened the GUI on its contents and presses ALL
- **THEN** the FIND list shows the whole warehouse's stock and the bar shows the warehouse's name

### Requirement: Basket amounts limited to the stock
The amount the player sets for a basket entry SHALL NOT go above the count the FIND list shows for that item, less what the basket already holds of it when adding; shift clicking SHALL add at most that much. An item the basket already holds as often as the list shows SHALL NOT be added again.

#### Scenario: Counter stops at the stock
- **WHEN** the FIND list shows 40 bread and the player clicks bread and steps the amount up
- **THEN** the amount stops at 40

#### Scenario: Shift click with little stock
- **WHEN** the FIND list shows 20 bread, the basket holds 10 bread and the player shift clicks bread
- **THEN** the basket holds 20 bread

### Requirement: Relocate starts pick target mode
Relocate SHALL be offered only while FIND lists a scanned shelf, its shelf basket holds an entry and the handheld is bound; the warehouse view SHALL have no Relocate. Relocate SHALL keep the shelf basket and its shelf as the relocation, empty the shelf basket, close the GUI and start pick target mode, whose panel says to scan the target and shows the entry count. A mode switch SHALL end pick target mode and drop the relocation.

#### Scenario: Relocate
- **WHEN** a bound player with 2 shelf basket entries presses Relocate
- **THEN** the GUI closes, the shelf basket is empty and the panel shows pick target mode with 2 entries

#### Scenario: Empty basket
- **WHEN** the shelf basket is empty
- **THEN** the Relocate action cannot be pressed

#### Scenario: No Relocate for the warehouse
- **WHEN** FIND lists the whole warehouse and the warehouse basket holds entries
- **THEN** no Relocate action is shown

#### Scenario: Mode switch leaves pick target mode
- **WHEN** a player in pick target mode sneak scrolls to INFO
- **THEN** the handheld is in INFO, no longer in pick target mode, the relocation is dropped and the warehouse basket is unchanged

### Requirement: Relocating onto a target shelf
In pick target mode, right clicking a bay of another Heavy Duty Rack shelf of the bound warehouse in scan range SHALL relocate onto that whole shelf. Shuttles SHALL carry each entry only from the relocation's source shelf, one stack per trip like a terminal request. On success the shelf SHALL flash green, a beep SHALL play, the action bar SHALL name the shelf's code and pick target mode SHALL end. The source shelf SHALL be refused as target.

#### Scenario: Move to a shelf
- **WHEN** the relocation takes 128 cobblestone from shelf A, which holds 300, and the player scans an empty unzoned shelf B in pick target mode
- **THEN** shelf B flashes green and shuttles carry 128 cobblestone from shelf A onto shelf B in stacks

#### Scenario: Not a target
- **WHEN** a player in pick target mode scans a Fluid Rack, a rack of another warehouse or the source shelf
- **THEN** nothing moves, a buzz plays and the handheld stays in pick target mode

### Requirement: Zone filter in a relocation
Relocation entries the target shelf's zone filter does not take SHALL be refused, named in the action bar, and not moved; the other entries SHALL still move.

#### Scenario: Zone filter refuses an item
- **WHEN** the relocation holds raw copper and bread and the target shelf is zoned "Ores" with `c:ores`
- **THEN** the raw copper moves, the bread stays where it is and the action bar names bread as refused

### Requirement: Room for a relocation
Room on the target shelf SHALL be checked for everything that would move before anything starts. When the shelf cannot take it all, nothing SHALL move, the shelf SHALL flash red, a buzz SHALL play, the action bar SHALL say the shelf has no room, and the handheld SHALL stay in pick target mode.

#### Scenario: No room
- **WHEN** the relocation holds 640 cobblestone and the target shelf has room for 192
- **THEN** nothing moves, the shelf flashes red, a buzz plays, and the handheld stays in pick target mode

### Requirement: Shortfall in a relocation
A relocation amount SHALL be an upper bound: what the source shelf still holds SHALL move, and the action bar SHALL report how many of each entry are short.

#### Scenario: Source emptied meanwhile
- **WHEN** the relocation asks for 128 bread from a shelf that now holds 40 bread
- **THEN** 40 bread move and the action bar reports 88 short

#### Scenario: Gone from the source
- **WHEN** the source shelf no longer holds any stone and the relocation asks for stone
- **THEN** no stone moves and the action bar reports it short

### Requirement: Basket fit is pure logic
Whether a basket fits on a shelf SHALL be decided by pure logic without Minecraft, from the shelf's slots (item, count, stack limit, locked) and the stacks that would move: partial stacks of the same item fill first, then empty unlocked slots, and slots another job locked do not count.

#### Scenario: Shared empty slots
- **WHEN** a shelf has 2 empty slots and the basket would move 64 cobblestone and 64 bread
- **THEN** it fits, and adding 1 more bread makes it not fit

## MODIFIED Requirements

### Requirement: FIND mode
The FIND screen SHALL list the items stored in the bound warehouse with their counts, largest first, with a search line. Right clicking an item in the list SHALL colour every loaded shelf of the bound warehouse holding that item, a tint and an outline over all its bays, seen through blocks, for some seconds (example 8), while the handheld is in hand; a left click adds the item to the basket of the list shown. The panel SHALL show the item, its total and each holding shelf's code once. The screen SHALL say which click adds and which finds.

#### Scenario: Pick from the list
- **WHEN** the bound warehouse holds bread in two bays of one shelf and in one bay of another shelf and the player right clicks bread on the FIND screen
- **THEN** both shelves are tinted and outlined as a whole, seen through the walls, and the panel shows bread, its total and the two shelf codes

#### Scenario: Glow only in hand
- **WHEN** shelves glow after a FIND and the player switches to another hotbar slot
- **THEN** no shelf is tinted until the handheld is in hand again

#### Scenario: Glow ends
- **WHEN** the glow time has passed
- **THEN** no shelf is tinted or outlined any more
