## Purpose

The Fluid Rack is the warehouse's fluid storage bay: it joins neighbouring bays into sections like the Heavy Duty Rack, holds one fluid per section behind a glass front, and is served by Tanker shuttles through one port per section.

## ADDED Requirements

### Requirement: Section tank
A Fluid Rack section SHALL hold one fluid at a time in one tank of `capacity_per_bay` per bay (in `data/<ns>/shuttles/fluid_rack.json`, example 16,000 mB). A section holding a fluid SHALL refuse every other fluid until it is empty. The bays SHALL share the section's fluid evenly, each bay storing its share, so that taking a bay away takes its share with it. The content SHALL be kept when the world is saved.

#### Scenario: Capacity of a full section
- **WHEN** a section has three bays
- **THEN** it holds up to 48,000 mB of one fluid

#### Scenario: One fluid per section
- **WHEN** a section holds water and lava is offered to it
- **THEN** it takes none

#### Scenario: Shares follow the bays
- **WHEN** a three bay section holds 30,000 mB of water and a player breaks its right bay
- **THEN** the dropped rack item carries 10,000 mB of water and the two bays left behind hold 20,000 mB together

### Requirement: Bays form sections
Fluid Racks with the same facing and the same wood that stand in a row SHALL form sections of up to `bays_per_section` bays (`fluid_rack.json`, example 3), with joins decided at placement as for the Heavy Duty Rack; a bay placed on top of a row SHALL copy the row below's section boundaries. A bay holding a fluid SHALL join only a section that is empty or holds the same fluid. Stacked rows SHALL keep separate tanks, and inside a section the posts SHALL give way to a frame that runs through.

#### Scenario: Three in a row
- **WHEN** a player places three Fluid Racks of the same wood side by side, all facing north
- **THEN** they form one section of three bays with posts only at both ends

#### Scenario: Different fluid starts a new section
- **WHEN** a rack item carrying lava is placed beside a single bay holding water
- **THEN** it does not join that bay and stands as a section of its own

#### Scenario: Stacked rows stay apart
- **WHEN** a row of three bays holding water has a row of three empty bays placed on top
- **THEN** the top row forms its own section with the same boundaries and holds no water

### Requirement: One port per section
Every section SHALL have exactly one port, a copper coupling flange on the front of its port bay, at the height of a tanker's coupling: the centre bay of a three bay section, the left bay seen from the front of a two bay section, the only bay of a single bay. The port SHALL move when the section changes, for example when a bay is broken. Each stacked row SHALL have its own port.

#### Scenario: Port of three
- **WHEN** a section has three bays
- **THEN** only the middle bay shows a port

#### Scenario: Port of two
- **WHEN** a player stands in front of a two bay section
- **THEN** the port is on the bay to the player's left

#### Scenario: Port moves
- **WHEN** the left bay of a three bay section is broken
- **THEN** the remaining two bays form a section whose port is on its left bay

### Requirement: Joins only through the port's rail
A Fluid Rack section SHALL be a stock holding member of the warehouse network only through a rail directly in front of its port bay. Rails SHALL dock onto the port bay's front only and SHALL show no dock toward the other bays. A Fluid Rack SHALL NOT join through touching controllers, stations, garages, elevators or other racks, and SHALL pass no connection on. It SHALL NOT carry a connection lamp.

#### Scenario: Rail in front of the port
- **WHEN** a three bay section has a rail of an online warehouse in front of its middle bay
- **THEN** the rail docks toward the middle bay and the section's fluid counts as warehouse stock

#### Scenario: Rail in front of another bay
- **WHEN** a three bay section has a rail of an online warehouse only in front of its left bay
- **THEN** the rail shows no dock, the section is not part of the warehouse and its fluid is not stock

### Requirement: Sections are fluid stock
Every Fluid Rack section that belongs to a warehouse SHALL be part of its fluid stock, next to Storage gantry tanks: the warehouse SHALL know per section its zone, fluid, amount and capacity. A change of a section (by bucket, by shuttle, by a pipe while standalone, by a bay joining or leaving) SHALL reach the warehouse within a second, without polling every tick.

#### Scenario: Bucket reaches the stock
- **WHEN** a player empties a water bucket into a docked section
- **THEN** the warehouse's water total grows by 1000 mB within a second

### Requirement: Fluids command lists racks
`/servus_warehouse fluids <pos>` SHALL list every Fluid Rack section of the warehouse with its port position, zone, fluid, amount and capacity after the gantries, and its totals SHALL include the sections.

#### Scenario: Racks and gantries add up
- **WHEN** a Storage gantry holds 3000 mB of water and a docked Fluid Rack section holds 5000 mB of water
- **THEN** `/servus_warehouse fluids` lists 8000 mB of water, the gantry and the section

### Requirement: Docked sections are tankers only
While a rail stands in front of a section's port bay, no bay of that section SHALL expose a fluid handler, so pipes neither fill nor drain it and show no fluid arm toward it. Without such a rail every bay SHALL expose the section's tank on every side, filling and draining it. Placing or breaking that rail, and the section changing, SHALL update every bay at once, without polling. Whether the rail belongs to an online warehouse SHALL NOT matter.

#### Scenario: Standalone rack as a tank
- **WHEN** a pump feeds a duct touching the side of a single bay with no rail in front of it
- **THEN** the bay fills with water

#### Scenario: Rail closes the section
- **WHEN** a rail is placed in front of the port bay of a section a duct is filling
- **THEN** the duct stops filling and loses its fluid arm toward the section

### Requirement: Buckets by hand
Using a filled fluid container on the front of any bay SHALL empty it into the section when the section takes that fluid. Using an empty container SHALL fill it from the section when the section holds at least one container's worth. Both SHALL work whether or not the section is docked. A player in creative mode SHALL keep the container and get the filled one too. A zoned section SHALL refuse by hand what its zone refuses. The rack SHALL open no GUI.

#### Scenario: Water bucket into a rack
- **WHEN** a player uses a water bucket on the front of an empty, unzoned bay
- **THEN** the section holds 1,000 mB of water and the player holds an empty bucket

#### Scenario: No GUI
- **WHEN** a player right clicks a Fluid Rack with an empty hand
- **THEN** nothing opens

### Requirement: Glass front shows the level
Each bay SHALL show a glass front over the whole bay with the section's fluid drawn as real fluid (its still texture and tint) from the bottom up to the section's fill share, the same height on every bay of the section. The client SHALL learn the level in steps (`level_steps` in `fluid_rack.json`, example 16) and only when a step changes. The frame SHALL be blackened wrought iron on every face, the boards SHALL show the crafting wood, and no two faces SHALL share a plane where they overlap.

#### Scenario: Half full
- **WHEN** a three bay section is filled to half
- **THEN** every bay shows fluid up to half the height of its glass front

#### Scenario: Empty
- **WHEN** a section is empty
- **THEN** its glass fronts show no fluid

### Requirement: Sides climb like a ladder
A player touching an exposed side of a Fluid Rack SHALL climb it as the Heavy Duty Rack's sides allow. The front and the back SHALL NOT be climbable.

#### Scenario: Climb a rack side
- **WHEN** a player pushes against the exposed side of a stacked Fluid Rack row
- **THEN** the player moves up as on a ladder

### Requirement: Fluid Rack is obtainable and dismantled
The Fluid Rack SHALL be craftable from vanilla materials including planks and glass (example: iron ingots, glass, planks and a copper ingot), remembering the wood of its planks as the Heavy Duty Rack does. Breaking a bay or dismantling it by sneaking with the Configuration Carrot SHALL drop one rack item that carries the bay's share of the fluid, its wood and its zone, showing fluid and amount in its tooltip. Placing that item SHALL bring them back.

#### Scenario: Dismantle keeps the share
- **WHEN** a player dismantles a single bay holding 12,000 mB of lava and places it again
- **THEN** the placed bay holds 12,000 mB of lava

#### Scenario: Crafted wood
- **WHEN** a player crafts a Fluid Rack with spruce planks
- **THEN** the rack item names spruce and the placed rack shows spruce boards

### Requirement: One Fluid Rack in the creative tab and JEI
The creative tab and the recipe viewer SHALL list one Fluid Rack, the oak one, and the recipe viewer SHALL show its recipe accepting any planks.

#### Scenario: Creative tab
- **WHEN** a player opens the Servus creative tab
- **THEN** it lists one Fluid Rack with oak boards

### Requirement: Fluid Rack inspect
Using the Data Potato on any face of a Fluid Rack SHALL print a chat report for its section: the zone name or that it has none, the number of bays, the fluid with its amount against the section's capacity in mB or that it is empty with its capacity, and whether the section is docked. It SHALL open no GUI and change nothing.

#### Scenario: Section report
- **WHEN** a three bay section zoned "Cooling" holds 26,500 mB of water and a rail stands in front of its port, and a player uses the potato on its right bay
- **THEN** the chat shows zone "Cooling", 3 bays, water 26,500 of 48,000 mB, docked

### Requirement: Section rules are pure
The section's port bay, the split of a section's fluid into bay shares and the joining of a bay that carries a fluid SHALL have no Minecraft dependency and SHALL be covered by JUnit tests.

#### Scenario: Unit tested
- **WHEN** the unit tests run
- **THEN** they cover the port for one, two and three bays, even shares with a remainder, and joins refused for a different fluid
