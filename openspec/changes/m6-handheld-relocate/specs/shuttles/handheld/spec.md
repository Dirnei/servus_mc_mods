# Spec Delta

## ADDED Requirements

### Requirement: STOCK mode
The STOCK screen SHALL list the items stored in the bound warehouse with their counts, largest first, with a search line. Right clicking an item in the list SHALL close the GUI and colour every loaded shelf of the bound warehouse holding that item, a tint and an outline over all its bays, seen through blocks, for some seconds (example 8), while the handheld is in hand. The panel SHALL show the item, its total and each holding shelf's code once.

#### Scenario: Find from the stock list
- **WHEN** the bound warehouse holds bread in two bays of one shelf and in one bay of another shelf and the player right clicks bread on the STOCK screen
- **THEN** both shelves are tinted and outlined as a whole, seen through the walls, and the panel shows bread, its total and the two shelf codes

#### Scenario: Glow only in hand
- **WHEN** shelves glow after a find and the player switches to another hotbar slot
- **THEN** no shelf is tinted until the handheld is in hand again

#### Scenario: Glow ends
- **WHEN** the glow time has passed
- **THEN** no shelf is tinted or outlined any more

### Requirement: STOCK scan jumps to INFO
Right clicking a bay in STOCK mode SHALL switch the handheld to INFO and scan that shelf as INFO does.

#### Scenario: Scan from STOCK
- **WHEN** a player in STOCK mode right clicks a bay of the shelf `N8.W3+0`
- **THEN** the handheld is in INFO and shows the scan of `N8.W3+0`

### Requirement: Find from a shelf list
Right clicking an item in the INFO screen's item list SHALL find it like the STOCK list does: close the GUI and colour every loaded shelf of the bound warehouse holding that item.

#### Scenario: Where else is it
- **WHEN** a player scanned a shelf holding bread and right clicks bread on the INFO screen
- **THEN** every shelf of the bound warehouse holding bread is tinted and outlined

### Requirement: Baskets
The INFO and STOCK screens SHALL each hold a basket of items with amounts, at most a fixed number of entries (example 9), each one item. Clicking an item in the list SHALL add it with an amount the player sets, shift clicking SHALL add one stack, and an item already in the basket SHALL raise that entry. The player SHALL be able to change an entry's amount, remove an entry and clear the basket. A new item for a full basket SHALL be refused with a note.

#### Scenario: Build a basket
- **WHEN** a player on the STOCK screen adds 128 cobblestone and then shift clicks bread
- **THEN** the basket lists 128 cobblestone and 64 bread

#### Scenario: Same item again
- **WHEN** the basket holds 64 bread and the player adds 32 bread
- **THEN** the basket holds one bread entry of 96

#### Scenario: Basket full
- **WHEN** the basket has 9 entries and the player adds a tenth item
- **THEN** the item is not added and the screen says the basket is full

### Requirement: Warehouse basket and shelf basket
The STOCK basket (warehouse basket) SHALL be kept for later requests and emptied only by the player. The INFO basket (shelf basket) SHALL belong to the scanned shelf and SHALL be emptied when the handheld leaves that shelf: a scan of another shelf or a switch to another mode. Closing the GUI and logging out SHALL keep both.

#### Scenario: Warehouse basket survives
- **WHEN** a player builds a STOCK basket, closes the GUI, switches to ZONE and back to STOCK and opens the GUI again
- **THEN** the STOCK basket shows the same entries

#### Scenario: Shelf basket kept on close
- **WHEN** a player builds an INFO basket, closes the GUI and opens it again without scanning
- **THEN** the INFO basket shows the same entries

#### Scenario: Leaving the shelf empties it
- **WHEN** a player builds an INFO basket and scans another shelf
- **THEN** the INFO basket is empty and the STOCK basket is unchanged

### Requirement: Basket amounts limited to the stock
The amount the player sets for a basket entry SHALL NOT go above the count the list shows for that item, less what the basket already holds of it when adding; shift clicking SHALL add at most that much. An item the basket already holds as often as the list shows SHALL NOT be added again.

#### Scenario: Counter stops at the stock
- **WHEN** the list shows 40 bread and the player clicks bread and steps the amount up
- **THEN** the amount stops at 40

#### Scenario: Shift click with little stock
- **WHEN** the list shows 20 bread, the basket holds 10 bread and the player shift clicks bread
- **THEN** the basket holds 20 bread

### Requirement: Relocate starts pick target mode
Relocate SHALL be offered only on the INFO screen of a scanned Heavy Duty Rack shelf whose basket holds an entry, with the handheld bound; STOCK SHALL have no Relocate. Relocate SHALL keep the INFO basket and its shelf as the relocation, empty the INFO basket, close the GUI and start pick target mode, whose panel says to scan the target and shows the entry count. A mode switch SHALL end pick target mode and drop the relocation.

#### Scenario: Relocate
- **WHEN** a bound player with 2 INFO basket entries presses Relocate
- **THEN** the GUI closes, the INFO basket is empty and the panel shows pick target mode with 2 entries

#### Scenario: Empty basket
- **WHEN** the INFO basket is empty
- **THEN** the Relocate action cannot be pressed

#### Scenario: No Relocate in STOCK
- **WHEN** the STOCK basket holds entries
- **THEN** the STOCK screen shows no Relocate action

#### Scenario: Mode switch leaves pick target mode
- **WHEN** a player in pick target mode sneak scrolls to STOCK
- **THEN** the handheld is in STOCK, no longer in pick target mode, the relocation is dropped and the STOCK basket is unchanged

### Requirement: Relocating onto a target shelf
In pick target mode, right clicking a bay of another Heavy Duty Rack shelf of the bound warehouse in scan range SHALL relocate onto that whole shelf instead of scanning it. Shuttles SHALL carry each entry only from the relocation's source shelf, one stack per trip like a terminal request. On success the shelf SHALL flash green, a beep SHALL play, the action bar SHALL name the shelf's code and pick target mode SHALL end. The source shelf SHALL be refused as target.

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

### Requirement: Modes
The handheld SHALL be in one of the modes ZONE, INFO or STOCK, kept on the item; a handheld saved in the old FIND mode SHALL load in INFO. A new handheld SHALL be in ZONE. Scrolling the mouse wheel while sneaking with the handheld in the main hand SHALL switch to the next or previous mode, wrap around, play a short click and SHALL NOT change the selected hotbar slot.

#### Scenario: Next mode
- **WHEN** a player holding a handheld in ZONE sneaks and scrolls down once
- **THEN** the handheld is in INFO and the hotbar selection is unchanged

#### Scenario: Wrap around
- **WHEN** the handheld is in STOCK and the player sneaks and scrolls down once
- **THEN** the handheld is in ZONE

#### Scenario: Scroll without sneaking
- **WHEN** a player holding a handheld scrolls without sneaking
- **THEN** the hotbar selection moves as usual and the mode stays

### Requirement: Handheld GUI
Right clicking the air with the handheld SHALL open its GUI on the screen of its current mode: in ZONE the team's zone list, in INFO the last scanned shelf, in STOCK the stock list of the bound warehouse. The ZONE screen SHALL list the zones of the player's team with colour, name, filter summary and number of loaded shelves, offer a "No zone" entry, let the player choose the active zone, and let the player create, edit and delete zones.

#### Scenario: Opens on the mode
- **WHEN** a player whose handheld is in STOCK right clicks the air
- **THEN** the GUI opens showing the bound warehouse's stock list with its search line

#### Scenario: Choose the active zone
- **WHEN** a player opens the GUI in ZONE and clicks the zone "Ores"
- **THEN** "Ores" is the handheld's active zone and the panel in ZONE mode shows it

#### Scenario: No zone entry
- **WHEN** a player chooses "No zone"
- **THEN** scanning a shelf in ZONE mode clears that shelf's zone

### Requirement: Handheld GUI looks like the device
The GUI SHALL be drawn as the handheld itself: a dark steel body inside a yellow rubber bumper, a red scan window and a brand line "HH-1" with a green LED at the top, and a green CRT screen holding the mode's content. The screen SHALL show dark green background, light green text and dim green labels, with scanlines, a dark inner vignette and a faint glow. Item icons on the screen SHALL show in their own colours, and stack counts in item cells SHALL be short (for example 8k) in small green text.

#### Scenario: CRT screen
- **WHEN** a player opens the handheld GUI
- **THEN** the content sits on a green screen with scanlines inside the yellow bumpered device

#### Scenario: Short counts
- **WHEN** the STOCK list shows 8,000 cobblestone
- **THEN** its cell shows the cobblestone icon in its own colours and the count 8k in small green text

### Requirement: Mode softkeys
The device SHALL show one softkey per mode (ZONE, INFO, STOCK) below the screen. Clicking a softkey SHALL switch the handheld to that mode, like sneak scrolling does, and show that mode's screen. The softkey of the current mode SHALL be lit.

#### Scenario: Switch in the GUI
- **WHEN** the GUI shows the zone list and the player clicks the STOCK softkey
- **THEN** the handheld is in STOCK, the STOCK softkey is lit and the stock list shows

### Requirement: Binding to a warehouse
Right clicking a Warehouse Controller with the handheld SHALL bind the handheld to that controller's warehouse, show the warehouse name in the action bar and keep the binding on the item. STOCK SHALL list and find in the bound warehouse. A handheld that is not bound, or whose controller is gone, SHALL say so on its panel in STOCK mode.

#### Scenario: Bind
- **WHEN** a player right clicks the controller of "Main Base" with the handheld
- **THEN** the handheld is bound to "Main Base" and the action bar says so

#### Scenario: Controller gone
- **WHEN** the bound controller has been broken and the player switches to STOCK
- **THEN** the panel says the handheld is not bound

### Requirement: INFO mode
INFO SHALL show a shelf only after it is scanned. Right clicking a bay in INFO mode SHALL scan its shelf with a beep and a flash and open the GUI on it. Until the next scan the panel and the INFO screen SHALL show its code, zone, zone filter, slots in use and its items with counts, or a Fluid Rack shelf's fluid and amount; the panel lists the items held most, the INFO screen all of them as a grid with the basket below. The readout SHALL change nothing.

#### Scenario: GUI shows the whole shelf
- **WHEN** a player scans a shelf holding 12 different items
- **THEN** the INFO screen opens with all 12 items and their counts, while the panel lists only the few held most

#### Scenario: Aim shows nothing
- **WHEN** a player in INFO mode aims at a bay without clicking
- **THEN** the panel shows the shelf's code in its top line and no zone, slots or items

#### Scenario: Scan a shelf
- **WHEN** the player right clicks a bay of a shelf of three bays of 27 slots in "Ores" holding 320 raw iron spread over 5 slots of two bays
- **THEN** a beep plays, the shelf flashes, and the panel shows the shelf's code, "Ores", its filter, 5/81 and raw iron 320

#### Scenario: Readout stays
- **WHEN** the player then closes the GUI, looks away or opens the GUI again
- **THEN** the panel and the INFO screen still show that shelf until another shelf is scanned

## REMOVED Requirements

### Requirement: FIND mode
**Reason**: FIND and INFO showed nearly the same screen; the warehouse stock list moves to the new STOCK mode and finding is a right click on an item in either list.
**Migration**: Use STOCK for the warehouse list and right click an item to find it; a handheld saved in FIND loads in INFO.

### Requirement: FIND scan shows the shelf
**Reason**: Scanning a shelf in INFO now opens the GUI on its contents, which is what the FIND scan did.
**Migration**: Scan the shelf in INFO (or in STOCK, which switches to INFO).
