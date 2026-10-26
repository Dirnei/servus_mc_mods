# foundation/machine-names Specification

## Purpose
Machine names let a player tell machines of the same kind apart: every machine that the warehouse's shuttles or the Crafting CPU work with has a label made of an optional name and, where a warehouse gave one, a number, shown wherever such a machine is named.

## Requirements

### Requirement: Named machines
A machine SHALL be named when a shuttle serves it or when it shows on a data line, that is in the Production Terminal's lists. These are the Inbound, Outbound and Packing Station, Field Head, Shuttle Garage, Elevator shaft, Crafting Station, Crafting CPU, Machine Adapter, Coal Generator, Burner Pump, Salamander Forge, Receptaculum, Prelum, Coagulator, Athanor and Calciner. Racks, Pattern Storage, terminals, the Shuttle Workshop and the Warehouse Controller SHALL NOT be named machines; machines that never show on a data line, such as the Electric Pump and the Ender Anchor, are not named either.

#### Scenario: Generator on the data line
- **WHEN** a Coal Generator kept stocked from a warehouse's data line is named "Boiler"
- **THEN** the Production Terminal lists it as "Boiler (Coal Generator #1)"

#### Scenario: Part of a machine
- **WHEN** the data line reaches a Calciner through its Firebox, or a player uses the Data Potato on a Coal Generator's chimney
- **THEN** the part reads as its machine's label, "Calciner #1" or the generator's label

#### Scenario: Rack is not named
- **WHEN** a player opens a Heavy Duty Rack's GUI
- **THEN** its title is the block name and it has no pencil button

#### Scenario: Workshop is not named
- **WHEN** a player opens the Shuttle Workshop
- **THEN** its title is the block name and it has no pencil button

### Requirement: Machine name
A named machine SHALL have an optional name. A typed name SHALL be cleaned like a site name: control and formatting characters removed, trimmed and cut to the site name length (example 32). A name that is empty after cleaning SHALL remove the name.

#### Scenario: Name cleaned
- **WHEN** the player types "  Wheat\nNorth  " as a Field Head's name
- **THEN** the name is "WheatNorth"

#### Scenario: Name removed
- **WHEN** the player clicks the pencil of "Sticks (Crafting Station #2)", clears the text box and presses Enter
- **THEN** the Crafting Station's label reads "Crafting Station #2"

### Requirement: Machine label
A named machine SHALL be named by its label: "<kind> #n" with a number and no name, "<name> (<kind> #n)" with both, the name alone with only a name, and the block name with neither, the same way in every message, row and report, also where machines of different kinds are listed together. The kind word SHALL come from the player's language. The label SHALL be computed by pure logic that is unit tested without Minecraft.

#### Scenario: Numbered
- **WHEN** a Field Head has number 2 and no name
- **THEN** its label reads "Field Head #2"

#### Scenario: Numbered and named
- **WHEN** a Crafting Station has number 2 and the name "Sticks"
- **THEN** its label reads "Sticks (Crafting Station #2)"

#### Scenario: Named without number
- **WHEN** a Salamander Forge in a world without Servus: Shuttles is named "Big Forge"
- **THEN** its label reads "Big Forge"

#### Scenario: Neither
- **WHEN** a Machine Adapter was never on a warehouse's data line and has no name
- **THEN** its label reads "Machine Adapter"

### Requirement: Name in the title row
A named machine with a GUI SHALL show its name as plain text in place of the title, with a small pencil button ("Rename") right after it. Unnamed, the row SHALL show the label; named, it SHALL show the name with "#n" at the right end of the row. The pencil and the number SHALL NOT overlap any other widget, slot or text. Only a player who may use the GUI SHALL be able to rename the machine.

#### Scenario: Name shown as text
- **WHEN** the player opens Field Head #2 without a name
- **THEN** the title row reads "Field Head #2" as plain text with a pencil button after it and no text box

### Requirement: Rename in edit mode
Clicking the pencil SHALL switch the title row to edit mode: a focused text box holding the current name with the cursor at the end, and the pencil becomes a check button with the tooltip "Save name". Enter, the check or a click anywhere else in the GUI SHALL save and leave edit mode; Escape SHALL leave it without saving and SHALL NOT close the GUI. Every other key SHALL stay in the focused text box. A name still being edited SHALL be saved when the GUI closes.

#### Scenario: Name a Field Head
- **WHEN** the player opens Field Head #2, clicks the pencil, types "Wheat North" and presses Enter
- **THEN** the Field Head's label reads "Wheat North (Field Head #2)" and its GUI shows "Wheat North" with the pencil after it and "#2" at the right

#### Scenario: Name a Shuttle Garage
- **WHEN** the player opens Garage #1, clicks the pencil, types "Main Garage" then clicks the check button
- **THEN** the garage's label reads "Main Garage (Garage #1)"

#### Scenario: Click elsewhere saves
- **WHEN** the player is typing "Sticks" into the text box of Crafting Station #2 and clicks a slot of the GUI
- **THEN** the name is saved, the row shows "Sticks" as text again and the click acts on the slot

#### Scenario: Escape cancels
- **WHEN** the player opens "Main Garage (Garage #1)", clicks the pencil, types "Old Garage" and presses Escape
- **THEN** the GUI stays open, the row shows "Main Garage" as text again and the label is unchanged

#### Scenario: Name an elevator shaft from any segment
- **WHEN** the player opens the GUI of the third segment of Lift #1 and names it "Ore Lift"
- **THEN** opening the GUI from any segment of that shaft shows "Ore Lift" with "#1"

#### Scenario: Name a Crafting Station
- **WHEN** the player opens Crafting Station #2, clicks the pencil and types "Sticks"
- **THEN** its label reads "Sticks (Crafting Station #2)"

#### Scenario: Name a Machine Adapter
- **WHEN** the player opens Adapter #1 facing a furnace, clicks the pencil and types "Smelter"
- **THEN** its label reads "Smelter (Adapter #1)"

#### Scenario: Name an Alchemy machine
- **WHEN** the player opens Coagulator #1, or the GUI of a Salamander Forge, Receptaculum or Prelum, and names it with the pencil
- **THEN** that machine's label shows the name with its number

#### Scenario: Station keeps its row
- **WHEN** the player opens an Inbound, Outbound or Packing Station
- **THEN** its title row is the same name with the same pencil and edit mode as on every other named machine

#### Scenario: Site name works the same way
- **WHEN** the player opens a Warehouse Controller
- **THEN** its site name shows in the title row as text with the same pencil and edit mode, without a number

### Requirement: Machines without a GUI keep a number only
A named machine without a GUI (Crafting CPU, Athanor, Calciner) SHALL have a number and SHALL read "<kind> #n", and SHALL have no way to set a name.

#### Scenario: CPU label
- **WHEN** a warehouse's data line has two formed Crafting CPUs
- **THEN** they read "CPU #1" and "CPU #2"

#### Scenario: Athanor label
- **WHEN** a formed Athanor is on a warehouse's data line
- **THEN** it reads "Athanor #1"

### Requirement: Name and number follow the dismantled machine
A named machine dismantled with the Configuration Carrot SHALL keep its name, its number and the warehouse that numbered it in the dropped item, and SHALL have them again when placed. A machine broken any other way, or one the carrot cannot dismantle (such as the Field Head), SHALL drop without name or number. A dismantled elevator segment or Crafting CPU block SHALL carry neither: the shaft and the box keep them.

#### Scenario: Crafting Station moved
- **WHEN** the player dismantles "Sticks (Crafting Station #2)" with the carrot and places it elsewhere on the same data line
- **THEN** it still reads "Sticks (Crafting Station #2)"

#### Scenario: Field Head broken
- **WHEN** "Wheat North (Field Head #2)" is broken and placed at another field of the same warehouse
- **THEN** it has no name and gets the next Field Head number

#### Scenario: Garage broken with a pickaxe
- **WHEN** "Main Garage (Garage #1)" is broken with a pickaxe and placed again
- **THEN** it has no name and gets a new number

### Requirement: Data Potato names a named machine
Using the Data Potato on a named machine SHALL print its label as the first chat line, followed by any report the block gives, and SHALL NOT open the GUI or change the machine.

#### Scenario: Machine without own report
- **WHEN** a player uses the potato on Crafting Station "Sticks (Crafting Station #2)"
- **THEN** chat shows "Sticks (Crafting Station #2)" and no GUI opens

#### Scenario: Station report
- **WHEN** a player uses the potato on Outbound #3 named "Compost Dock"
- **THEN** the first line names "Compost Dock (Outbound #3)", followed by the station's site and online state

### Requirement: Messages name machines by label
Every chat line, GUI row or state text that names one particular named machine SHALL use its label, never its block name or coordinates, while the machine is loaded. An unloaded machine SHALL be named by a plain word for what it is, such as "Machine" or "Station", without loading its chunk.

#### Scenario: Blocked producer
- **WHEN** Crafting Station "Sticks (Crafting Station #2)" cannot take the inputs of a step
- **THEN** the Production Terminal's machine row and the step's state read "Sticks (Crafting Station #2)" instead of "Crafting Station"

#### Scenario: Unloaded machine
- **WHEN** an order step waits at a Crafting Station whose chunk is not loaded
- **THEN** the step names it "Machine"

### Requirement: Shuttle and lift labels are coloured in chat
In chat and action bar messages, such as the Data Potato's reports, a shuttle's label SHALL read green and a lift's label aqua. Other machine labels SHALL keep the message's colour. GUI text (terminal rows, screen titles, tooltips) SHALL show every label uncoloured. The colour SHALL come from the shared label code, per kind, not from each message.

#### Scenario: Potato on a rail
- **WHEN** a player uses the potato on a rail where "Shuttle #10" waits for "Lift #3"
- **THEN** "Shuttle #10" reads green and "Lift #3" aqua in chat

#### Scenario: Terminal row
- **WHEN** the Production Terminal lists a Packing Station or a Storage Terminal GUI names a shuttle
- **THEN** the label is drawn in the GUI's own colour

### Requirement: Machine kinds behave the same
Every named machine kind SHALL follow the same rules for numbering, naming, cleaning, labels, saving and carrying; a new kind SHALL join by being registered, without its own copy of these rules. A test SHALL check these rules for every registered kind.

#### Scenario: Every kind
- **WHEN** the naming test runs
- **THEN** it places one machine of each registered kind, numbers it, renames it, saves and loads it, carries it in its dismantled item, and checks the same results for every kind
