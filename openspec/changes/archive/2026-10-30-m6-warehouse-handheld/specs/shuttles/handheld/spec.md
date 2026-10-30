# Spec Delta

## Purpose

The Warehouse Handheld is the player's WMS tool: one scanner item that assigns zones to rack shelves, finds stock in a warehouse and shows what a shelf holds, with modes that later changes extend.

## ADDED Requirements

### Requirement: Handheld item
The Handheld HH-1 SHALL be a Shuttles item that stacks to 1, crafted from vanilla materials and redstone, and shown in the Shuttles creative tab. Its look SHALL be a dark steel scanner with a yellow rubber bumper, a red scan window and a green screen.

#### Scenario: Crafting
- **WHEN** a player crafts the handheld recipe (for example iron, yellow dye, glass, redstone and a red dye)
- **THEN** the result is one Handheld HH-1

#### Scenario: Does not stack
- **WHEN** a player picks up a second handheld
- **THEN** it takes its own slot

### Requirement: Shelves are the unit
The handheld SHALL work on shelves: a shelf is the bays of a Heavy Duty Rack or Fluid Rack joined side by side on one level, a single bay standing alone being a shelf of one. Every scan, readout and glow SHALL cover the whole shelf of the bay the player aims at or clicks, never one bay of it.

#### Scenario: Any bay stands for its shelf
- **WHEN** a player scans the left bay and then the right bay of a shelf of three in INFO mode
- **THEN** both scans show the same code, zone and contents

### Requirement: Scan range
Every right click scan of a shelf SHALL reach farther than a normal block click, up to a range from data (example 16 blocks), and SHALL see through warehouse rails and elevator shafts to the shelf behind them; the panel and the scan line SHALL aim the same way.

#### Scenario: Scan from afar
- **WHEN** a player in ZONE mode with "Ores" active right clicks a bay 10 blocks away
- **THEN** that bay's shelf is in "Ores"

#### Scenario: Through a rail
- **WHEN** a rail runs between the player and an upper shelf of a high rack and the player scans that shelf
- **THEN** the shelf behind the rail is scanned, not the rail

### Requirement: Modes
The handheld SHALL be in one of the modes ZONE, FIND or INFO, kept on the item. A new handheld SHALL be in ZONE. Scrolling the mouse wheel while sneaking with the handheld in the main hand SHALL switch to the next or previous mode, wrap around, play a short click and SHALL NOT change the selected hotbar slot.

#### Scenario: Next mode
- **WHEN** a player holding a handheld in ZONE sneaks and scrolls down once
- **THEN** the handheld is in FIND and the hotbar selection is unchanged

#### Scenario: Wrap around
- **WHEN** the handheld is in INFO and the player sneaks and scrolls down once
- **THEN** the handheld is in ZONE

#### Scenario: Scroll without sneaking
- **WHEN** a player holding a handheld scrolls without sneaking
- **THEN** the hotbar selection moves as usual and the mode stays

### Requirement: Handheld panel
While a player holds the handheld in either hand, a green screen panel SHALL be drawn in a corner of the HUD. Its top line SHALL show the mode and the location code of the shelf the player aims at, or dashes when the player aims at no rack. Below it the panel SHALL show what the current mode describes.

#### Scenario: Panel while held
- **WHEN** a player holds the handheld in the off hand and aims at any bay of the shelf with code `N8.W3+0`
- **THEN** the panel shows the mode and `N8.W3+0` in its top line

#### Scenario: Panel gone
- **WHEN** the player switches to another hotbar slot
- **THEN** the panel is no longer drawn

### Requirement: Handheld GUI
Right clicking the air with the handheld SHALL open its GUI on the screen of its current mode: in ZONE the team's zone list, in FIND the stock list of the bound warehouse, in INFO the readout of the last scanned shelf. The ZONE screen SHALL list the zones of the player's team with colour, name, filter summary and number of loaded shelves, offer a "No zone" entry, let the player choose the active zone, and let the player create, edit and delete zones.

#### Scenario: Opens on the mode
- **WHEN** a player whose handheld is in FIND right clicks the air
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
- **WHEN** the FIND list shows 8,000 cobblestone
- **THEN** its cell shows the cobblestone icon in its own colours and the count 8k in small green text

### Requirement: Mode softkeys
The device SHALL show one softkey per mode (ZONE, FIND, INFO) below the screen. Clicking a softkey SHALL switch the handheld to that mode, like sneak scrolling does, and show that mode's screen. The softkey of the current mode SHALL be lit.

#### Scenario: Switch in the GUI
- **WHEN** the GUI shows the zone list and the player clicks the FIND softkey
- **THEN** the handheld is in FIND, the FIND softkey is lit and the stock list shows

### Requirement: Scanning a shelf in ZONE mode
Right clicking any bay of a Heavy Duty Rack or Fluid Rack with the handheld in ZONE mode SHALL put that bay's whole shelf into the active zone, or clear the shelf's zone when "No zone" is active, and SHALL NOT open the rack's GUI. The action bar SHALL show the shelf's location code, its zone before and its zone after. The whole shelf SHALL flash green and the handheld SHALL beep.

#### Scenario: Assign
- **WHEN** a player with "Ores" active right clicks the right bay of the shelf `N8.W3+0` of three bays that is in "Food"
- **THEN** all three bays are in "Ores", the action bar reads `N8.W3+0  Food -> Ores`, the shelf flashes green and a beep plays

#### Scenario: Already in the zone
- **WHEN** a player with "Ores" active right clicks a bay of a shelf already in "Ores"
- **THEN** nothing changes, the shelf flashes red and a low buzz plays

#### Scenario: No active zone chosen
- **WHEN** a player who never chose a zone right clicks a bay in ZONE mode
- **THEN** the action bar says to choose a zone in the handheld's GUI and the shelf is unchanged

#### Scenario: Sneaking changes nothing
- **WHEN** a player with "Ores" active sneaks and right clicks a bay of a shelf of three
- **THEN** the shelf of three is in "Ores", the same as without sneaking

### Requirement: Sweeping
Holding right click with the handheld in ZONE mode SHALL keep scanning: every further shelf the crosshair moves onto while the button is held SHALL be assigned like a single scan, with a beep whose pitch rises with each shelf of the sweep. Moving across further bays of a shelf already scanned in the sweep SHALL do nothing more, and a shelf already in the active zone SHALL be passed over silently.

#### Scenario: Sweep a row of shelves
- **WHEN** a player with "Bulk" active holds right click on a bay of the shelf `N8.W8+0` and moves the crosshair across the shelves `N8.W5+0` and `N8.W2+0`
- **THEN** all three shelves are in "Bulk" and three rising beeps play

#### Scenario: Several bays of one shelf
- **WHEN** the sweep crosses all three bays of one shelf
- **THEN** that shelf is assigned once with one beep

#### Scenario: Sweep over a zoned shelf
- **WHEN** the sweep crosses a shelf already in "Bulk"
- **THEN** that shelf is unchanged and makes no sound

### Requirement: Binding to a warehouse
Right clicking a Warehouse Controller with the handheld SHALL bind the handheld to that controller's warehouse, show the warehouse name in the action bar and keep the binding on the item. FIND SHALL search the bound warehouse. A handheld that is not bound, or whose controller is gone, SHALL say so on its panel in FIND mode.

#### Scenario: Bind
- **WHEN** a player right clicks the controller of "Main Base" with the handheld
- **THEN** the handheld is bound to "Main Base" and the action bar says so

#### Scenario: Controller gone
- **WHEN** the bound controller has been broken and the player switches to FIND
- **THEN** the panel says the handheld is not bound

### Requirement: FIND mode
The FIND screen SHALL list the items stored in the bound warehouse with their counts, largest first, with a search line. Picking an item in the list SHALL colour every loaded shelf of the bound warehouse holding that item, a tint and an outline over all its bays, seen through blocks, for some seconds (example 8), while the handheld is in hand. The panel SHALL show the item, its total and each holding shelf's code once.

#### Scenario: Pick from the list
- **WHEN** the bound warehouse holds bread in two bays of one shelf and in one bay of another shelf and the player picks bread on the FIND screen
- **THEN** both shelves are tinted and outlined as a whole, seen through the walls, and the panel shows bread, its total and the two shelf codes

#### Scenario: Glow only in hand
- **WHEN** shelves glow after a FIND and the player switches to another hotbar slot
- **THEN** no shelf is tinted until the handheld is in hand again

#### Scenario: Glow ends
- **WHEN** the glow time has passed
- **THEN** no shelf is tinted or outlined any more

### Requirement: FIND scan shows the shelf
Right clicking a bay in FIND mode SHALL flash its shelf and open the GUI with the FIND list showing only what that shelf holds and the shelf's code in the title.

#### Scenario: Scan a shelf
- **WHEN** a player in FIND mode right clicks a bay of the shelf `N8.W3+0` holding cobblestone and bread
- **THEN** the shelf flashes and the GUI opens with "Shelf N8.W3+0" in the title and only cobblestone and bread in the list

#### Scenario: Empty shelf
- **WHEN** a player in FIND mode right clicks a bay of an empty shelf
- **THEN** the GUI opens with the shelf's code and an empty list

### Requirement: INFO mode
INFO SHALL show a shelf only after it is scanned; aiming shows nothing about its contents. Right clicking a bay in INFO mode SHALL scan its shelf with a beep and a flash. Until the next scan the panel and the INFO screen SHALL show its code, zone, zone filter, slots in use and its items with counts, or a Fluid Rack shelf's fluid and amount; the panel lists the items held most, the INFO screen all of them. The readout SHALL change nothing.

#### Scenario: GUI shows the whole shelf
- **WHEN** a player scans a shelf holding 12 different items and opens the GUI
- **THEN** the INFO screen shows all 12 items with their counts, while the panel lists only the few held most

#### Scenario: Aim shows nothing
- **WHEN** a player in INFO mode aims at a bay without clicking
- **THEN** the panel shows the shelf's code in its top line and no zone, slots or items

#### Scenario: Scan a shelf
- **WHEN** the player right clicks a bay of a shelf of three bays of 27 slots in "Ores" holding 320 raw iron spread over 5 slots of two bays
- **THEN** a beep plays, the shelf flashes, and the panel shows the shelf's code, "Ores", its filter, 5/81 and raw iron 320

#### Scenario: Readout stays
- **WHEN** the player then looks away or opens the GUI
- **THEN** the panel and the INFO screen still show that shelf until another shelf is scanned

### Requirement: Handheld keeps other uses out
Right clicking a Heavy Duty Rack or Fluid Rack with the handheld SHALL never open the rack's GUI and SHALL never place a block. Right clicking any other block with the handheld SHALL do what any other item does there, except the Warehouse Controller.

#### Scenario: Rack GUI stays shut
- **WHEN** a player in INFO mode right clicks a rack front
- **THEN** no rack GUI opens

#### Scenario: Chest
- **WHEN** a player right clicks a chest with the handheld
- **THEN** the chest opens
