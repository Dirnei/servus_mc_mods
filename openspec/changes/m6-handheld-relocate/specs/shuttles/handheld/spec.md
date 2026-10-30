# Spec Delta

## ADDED Requirements

### Requirement: FIND basket
The FIND screen SHALL hold a basket of items with amounts, at most a fixed number of entries (example 9), each entry one item. Clicking an item in the stock list SHALL add it with an amount the player sets, shift clicking SHALL add one stack, and an item already in the basket SHALL raise that entry's amount. The player SHALL be able to change an entry's amount, remove an entry and clear the basket. Adding a new item to a full basket SHALL be refused with a note.

#### Scenario: Build a basket
- **WHEN** a player on the FIND screen adds 128 cobblestone and then shift clicks bread
- **THEN** the basket lists 128 cobblestone and 64 bread

#### Scenario: Same item again
- **WHEN** the basket holds 64 bread and the player adds 32 bread
- **THEN** the basket holds one bread entry of 96

#### Scenario: Basket full
- **WHEN** the basket has 9 entries and the player adds a tenth item
- **THEN** the item is not added and the screen says the basket is full

### Requirement: Basket kept on the handheld
The basket SHALL be kept on the handheld itself, each handheld with its own basket. Closing the GUI, switching modes, relocating and logging out SHALL NOT empty it; only the player clears it.

#### Scenario: Basket survives
- **WHEN** a player builds a basket, closes the GUI, switches to ZONE and back to FIND and opens the GUI again
- **THEN** the basket shows the same entries

#### Scenario: Kept after relocating
- **WHEN** a relocation of the basket has started
- **THEN** the basket still holds the same entries

### Requirement: Relocate starts pick target mode
The FIND screen SHALL offer a Relocate action while the basket holds at least one entry and the handheld is bound to a warehouse. Relocate SHALL close the GUI and put the handheld into pick target mode. In pick target mode the panel SHALL say to scan the target shelf and show how many entries the basket holds. Switching the handheld's mode by sneak scrolling or a softkey SHALL leave pick target mode and keep the basket.

#### Scenario: Relocate
- **WHEN** a bound player with 2 basket entries presses Relocate
- **THEN** the GUI closes and the panel shows pick target mode with 2 entries

#### Scenario: Empty basket
- **WHEN** the basket is empty
- **THEN** the Relocate action cannot be pressed

#### Scenario: Mode switch leaves pick target mode
- **WHEN** a player in pick target mode sneak scrolls to INFO
- **THEN** the handheld is in INFO, no longer in pick target mode, and the basket is unchanged

### Requirement: Relocating onto a target shelf
In pick target mode, right clicking any bay of a Heavy Duty Rack shelf of the bound warehouse, within the handheld's scan range, SHALL relocate the basket onto that whole shelf. Shuttles SHALL carry each entry from the warehouse's other shelves, never from the target shelf, one stack per trip like a terminal request. On success the shelf SHALL flash green, a beep SHALL play, the action bar SHALL name the shelf's code, and pick target mode SHALL end.

#### Scenario: Move to a shelf
- **WHEN** the basket holds 128 cobblestone, the warehouse holds 300 cobblestone on other shelves and the player scans an empty unzoned shelf in pick target mode
- **THEN** the shelf flashes green and shuttles carry 128 cobblestone onto that shelf in stacks

#### Scenario: Not a target
- **WHEN** a player in pick target mode scans a Fluid Rack or a rack of another warehouse
- **THEN** nothing moves, a buzz plays and the handheld stays in pick target mode

### Requirement: Zone filter in a relocation
Basket entries the target shelf's zone filter does not take SHALL be refused, named in the action bar, and not moved; the other entries SHALL still move.

#### Scenario: Zone filter refuses an item
- **WHEN** the basket holds raw copper and bread and the target shelf is zoned "Ores" with `c:ores`
- **THEN** the raw copper moves, the bread stays where it is and the action bar names bread as refused

### Requirement: Room for a relocation
Room on the target shelf SHALL be checked for everything that would move before anything starts. When the shelf cannot take it all, nothing SHALL move, the shelf SHALL flash red, a buzz SHALL play, the action bar SHALL say the shelf has no room, and the handheld SHALL stay in pick target mode.

#### Scenario: No room
- **WHEN** the basket holds 640 cobblestone and the target shelf has room for 192
- **THEN** nothing moves, the shelf flashes red, a buzz plays, and the handheld stays in pick target mode

### Requirement: Shortfall in a relocation
A basket amount SHALL be an upper bound: what the warehouse holds outside the target shelf SHALL move, and the action bar SHALL report how many of each entry are short.

#### Scenario: Less stock than asked
- **WHEN** the basket asks for 128 bread and the warehouse holds 40 bread outside the target shelf
- **THEN** 40 bread move and the action bar reports 88 short

#### Scenario: Already on the target
- **WHEN** all the warehouse's stone already lies on the target shelf and the basket asks for stone
- **THEN** no stone moves and the action bar reports it short

### Requirement: Basket fit is pure logic
Whether a basket fits on a shelf SHALL be decided by pure logic without Minecraft, from the shelf's slots (item, count, stack limit, locked) and the stacks that would move: partial stacks of the same item fill first, then empty unlocked slots, and slots another job locked do not count.

#### Scenario: Shared empty slots
- **WHEN** a shelf has 2 empty slots and the basket would move 64 cobblestone and 64 bread
- **THEN** it fits, and adding 1 more bread makes it not fit

## MODIFIED Requirements

### Requirement: FIND mode
The FIND screen SHALL list the items stored in the bound warehouse with their counts, largest first, with a search line. Right clicking an item in the list SHALL colour every loaded shelf of the bound warehouse holding that item, a tint and an outline over all its bays, seen through blocks, for some seconds (example 8), while the handheld is in hand; a left click adds the item to the basket. The panel SHALL show the item, its total and each holding shelf's code once. The screen SHALL say which click adds and which finds.

#### Scenario: Pick from the list
- **WHEN** the bound warehouse holds bread in two bays of one shelf and in one bay of another shelf and the player right clicks bread on the FIND screen
- **THEN** both shelves are tinted and outlined as a whole, seen through the walls, and the panel shows bread, its total and the two shelf codes

#### Scenario: Glow only in hand
- **WHEN** shelves glow after a FIND and the player switches to another hotbar slot
- **THEN** no shelf is tinted until the handheld is in hand again

#### Scenario: Glow ends
- **WHEN** the glow time has passed
- **THEN** no shelf is tinted or outlined any more
