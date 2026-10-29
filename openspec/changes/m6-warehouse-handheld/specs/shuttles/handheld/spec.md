# Spec Delta

## Purpose

The Warehouse Handheld is the player's WMS tool: one scanner item that assigns zones to rack bays, finds stock in a warehouse and shows what a bay holds, with modes that later changes extend.

## ADDED Requirements

### Requirement: Handheld item
The Handheld HH-1 SHALL be a Shuttles item that stacks to 1, crafted from vanilla materials and redstone, and shown in the Shuttles creative tab. Its look SHALL be a dark steel scanner with a yellow rubber bumper, a red scan window and a green screen.

#### Scenario: Crafting
- **WHEN** a player crafts the handheld recipe (for example iron, yellow dye, glass, redstone and a red dye)
- **THEN** the result is one Handheld HH-1

#### Scenario: Does not stack
- **WHEN** a player picks up a second handheld
- **THEN** it takes its own slot

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
While a player holds the handheld in either hand, a green screen panel SHALL be drawn in a corner of the HUD. Its top line SHALL show the mode and the location code of the bay the player aims at, or dashes when the player aims at no bay. Below it the panel SHALL show what the current mode describes.

#### Scenario: Panel while held
- **WHEN** a player holds the handheld in the off hand and aims at a bay with code `N8.W3+0`
- **THEN** the panel shows the mode and `N8.W3+0` in its top line

#### Scenario: Panel gone
- **WHEN** the player switches to another hotbar slot
- **THEN** the panel is no longer drawn

### Requirement: Handheld GUI
Right clicking the air with the handheld SHALL open its GUI. The GUI SHALL list the zones of the player's team with colour, name, filter summary and number of loaded bays, offer a "No zone" entry, let the player choose the active zone, and let the player create, edit and delete zones.

#### Scenario: Choose the active zone
- **WHEN** a player opens the GUI and clicks the zone "Ores"
- **THEN** "Ores" is the handheld's active zone and the panel in ZONE mode shows it

#### Scenario: No zone entry
- **WHEN** a player chooses "No zone"
- **THEN** scanning a bay in ZONE mode clears that bay's zone

### Requirement: Scanning a bay in ZONE mode
Right clicking a Heavy Duty Rack bay with the handheld in ZONE mode SHALL put that bay into the active zone, or clear its zone when "No zone" is active, and SHALL NOT open the rack's GUI. The action bar SHALL show the bay's location code, its zone before and its zone after. The bay SHALL flash green and the handheld SHALL beep.

#### Scenario: Assign
- **WHEN** a player with "Ores" active right clicks the bay `N8.W3+0` that is in "Food"
- **THEN** the bay is in "Ores", the action bar reads `N8.W3+0  Food -> Ores`, the bay flashes green and a beep plays

#### Scenario: Already in the zone
- **WHEN** a player with "Ores" active right clicks a bay already in "Ores"
- **THEN** nothing changes, the bay flashes red and a low buzz plays

#### Scenario: No active zone chosen
- **WHEN** a player who never chose a zone right clicks a bay in ZONE mode
- **THEN** the action bar says to choose a zone in the handheld's GUI and the bay is unchanged

### Requirement: Sweeping
Holding right click with the handheld in ZONE mode SHALL keep scanning: every further bay the crosshair moves onto while the button is held SHALL be assigned like a single scan, with a beep whose pitch rises with each bay of the sweep. A bay already in the active zone SHALL be passed over silently during a sweep.

#### Scenario: Sweep a row
- **WHEN** a player with "Bulk" active holds right click on the bay `N8.W4+0` and moves the crosshair across `N8.W3+0` and `N8.W2+0`
- **THEN** all three bays are in "Bulk" and three rising beeps play

#### Scenario: Sweep over a zoned bay
- **WHEN** the sweep crosses a bay already in "Bulk"
- **THEN** that bay is unchanged and makes no sound

### Requirement: Whole section
Sneak right clicking a Heavy Duty Rack bay in ZONE mode SHALL assign every bay of that bay's section at once, with one beep and a flash on every bay. Right clicking any bay of a Fluid Rack in ZONE mode SHALL always assign its whole section.

#### Scenario: Sneak on a section
- **WHEN** a player with "Food" active sneaks and right clicks one bay of a section of three bays
- **THEN** all three bays are in "Food" and the action bar says 3 bays

#### Scenario: Fluid Rack
- **WHEN** a player with "Cooling" active right clicks the right bay of a three bay Fluid Rack section
- **THEN** all three bays are in "Cooling"

### Requirement: Binding to a warehouse
Right clicking a Warehouse Controller with the handheld SHALL bind the handheld to that controller's warehouse, show the warehouse name in the action bar and keep the binding on the item. FIND SHALL search the bound warehouse. A handheld that is not bound, or whose controller is gone, SHALL say so on its panel in FIND mode.

#### Scenario: Bind
- **WHEN** a player right clicks the controller of "Main Base" with the handheld
- **THEN** the handheld is bound to "Main Base" and the action bar says so

#### Scenario: Controller gone
- **WHEN** the bound controller has been broken and the player switches to FIND
- **THEN** the panel says the handheld is not bound

### Requirement: FIND mode
In FIND mode the handheld GUI SHALL list the items stored in the bound warehouse with their counts, largest first, with a search line. Picking an item, or right clicking a bay in FIND mode to take that bay's most held item, SHALL make every loaded bay of the bound warehouse holding that item glow, visible through blocks, for some seconds (example 8). The panel SHALL show the item, its total and the codes of the bays holding it.

#### Scenario: Pick from the list
- **WHEN** the bound warehouse holds bread in two bays and the player picks bread in the GUI
- **THEN** both bays glow through the walls and the panel shows bread, its total and both codes

#### Scenario: Scan a bay
- **WHEN** a player in FIND mode right clicks a bay holding mostly cobblestone
- **THEN** every bay of the bound warehouse holding cobblestone glows

#### Scenario: Empty bay
- **WHEN** a player in FIND mode right clicks an empty bay
- **THEN** nothing glows and a low buzz plays

#### Scenario: Glow ends
- **WHEN** the glow time has passed
- **THEN** no bay glows any more

### Requirement: INFO mode
In INFO mode the panel SHALL show for the bay the player aims at its location code, its zone or none, its zone's filter, its slots in use against all its slots, and its most held items with their counts. Right clicking a bay in INFO mode SHALL pin it, so the panel keeps showing that bay until another bay is pinned or the same bay is clicked again. The readout SHALL change nothing.

#### Scenario: Aim at a bay
- **WHEN** a player in INFO mode aims at a bay in "Ores" holding 320 raw iron in 5 of 27 slots
- **THEN** the panel shows the code, "Ores", its filter, 5/27 and raw iron 320

#### Scenario: Pin
- **WHEN** the player right clicks that bay and then looks away
- **THEN** the panel still shows that bay, marked as pinned

### Requirement: Handheld keeps other uses out
Right clicking a Heavy Duty Rack or Fluid Rack with the handheld SHALL never open the rack's GUI and SHALL never place a block. Right clicking any other block with the handheld SHALL do what any other item does there, except the Warehouse Controller.

#### Scenario: Rack GUI stays shut
- **WHEN** a player in INFO mode right clicks a rack front
- **THEN** no rack GUI opens

#### Scenario: Chest
- **WHEN** a player right clicks a chest with the handheld
- **THEN** the chest opens
