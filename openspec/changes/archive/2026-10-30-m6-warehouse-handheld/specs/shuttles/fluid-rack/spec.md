# Spec Delta

## RENAMED Requirements

- FROM: `### Requirement: Section tank`
- TO: `### Requirement: Shelf tank`

- FROM: `### Requirement: Bays form sections`
- TO: `### Requirement: Bays form shelves`

- FROM: `### Requirement: One port per section`
- TO: `### Requirement: One port per shelf`

- FROM: `### Requirement: Sections are fluid stock`
- TO: `### Requirement: Shelves are fluid stock`

- FROM: `### Requirement: Docked sections are tankers only`
- TO: `### Requirement: Docked shelves are tankers only`

- FROM: `### Requirement: Section rules are pure`
- TO: `### Requirement: Shelf rules are pure`

- FROM: `### Requirement: Fluid Rack section layer on top`
- TO: `### Requirement: Fluid Rack shelf layer on top`

## MODIFIED Requirements

### Requirement: Shelf tank
A Fluid Rack shelf SHALL hold one fluid at a time in one tank of `capacity_per_bay` per bay (in `data/<ns>/shuttles/fluid_rack.json`, example 16,000 mB). A shelf holding a fluid SHALL refuse every other fluid until it is empty. The bays SHALL share the shelf's fluid evenly, each bay storing its share, so that taking a bay away takes its share with it. The content SHALL be kept when the world is saved.

#### Scenario: Capacity of a full section
- **WHEN** a shelf has three bays
- **THEN** it holds up to 48,000 mB of one fluid

#### Scenario: One fluid per section
- **WHEN** a shelf holds water and lava is offered to it
- **THEN** it takes none

#### Scenario: Shares follow the bays
- **WHEN** a three bay shelf holds 30,000 mB of water and a player breaks its right bay
- **THEN** the dropped rack item carries 10,000 mB of water and the two bays left behind hold 20,000 mB together

### Requirement: Bays form shelves
Fluid Racks with the same facing and the same wood that stand in a row SHALL form shelves of up to `bays_per_shelf` bays (`fluid_rack.json`, example 3), with joins decided at placement as for the Heavy Duty Rack; a bay placed on top of a row SHALL copy the row below's shelf boundaries. A bay holding a fluid SHALL join only a shelf that is empty or holds the same fluid. Stacked rows SHALL keep separate tanks, and inside a shelf the posts SHALL give way to a frame that runs through.

#### Scenario: Three in a row
- **WHEN** a player places three Fluid Racks of the same wood side by side, all facing north
- **THEN** they form one shelf of three bays with posts only at both ends

#### Scenario: Different fluid starts a new section
- **WHEN** a rack item carrying lava is placed beside a single bay holding water
- **THEN** it does not join that bay and stands as a shelf of its own

#### Scenario: Stacked rows stay apart
- **WHEN** a row of three bays holding water has a row of three empty bays placed on top
- **THEN** the top row forms its own shelf with the same boundaries and holds no water

### Requirement: One port per shelf
Every shelf SHALL have exactly one port, a copper coupling flange on the front of its port bay, at the height of a tanker's coupling: the centre bay of a three bay shelf, the left bay seen from the front of a two bay shelf, the only bay of a single bay. The port SHALL move when the shelf changes, for example when a bay is broken. Each stacked row SHALL have its own port.

#### Scenario: Port of three
- **WHEN** a shelf has three bays
- **THEN** only the middle bay shows a port

#### Scenario: Port of two
- **WHEN** a player stands in front of a two bay shelf
- **THEN** the port is on the bay to the player's left

#### Scenario: Port moves
- **WHEN** the left bay of a three bay shelf is broken
- **THEN** the remaining two bays form a shelf whose port is on its left bay

### Requirement: Joins only through the port's rail
A Fluid Rack shelf SHALL be a stock holding member of the warehouse network only through a rail directly in front of its port bay. Rails SHALL dock onto the port bay's front only and SHALL show no dock toward the other bays. A Fluid Rack SHALL NOT join through touching controllers, stations, garages, elevators or other racks, and SHALL pass no connection on. It SHALL NOT carry a connection lamp.

#### Scenario: Rail in front of the port
- **WHEN** a three bay shelf has a rail of an online warehouse in front of its middle bay
- **THEN** the rail docks toward the middle bay and the shelf's fluid counts as warehouse stock

#### Scenario: Rail in front of another bay
- **WHEN** a three bay shelf has a rail of an online warehouse only in front of its left bay
- **THEN** the rail shows no dock, the shelf is not part of the warehouse and its fluid is not stock

### Requirement: Every bay holds a rail
Every bay SHALL hold up a rail hung on its front as the Heavy Duty Rack does, so rails hang along the whole shelf. Only the port bay SHALL dock.

#### Scenario: Rails hang along the section
- **WHEN** a three bay shelf stands raised with air below its front and a player places rails in front of all three bays
- **THEN** all three rails stay and only the middle rail shows a dock

### Requirement: Shelves are fluid stock
Every Fluid Rack shelf that belongs to a warehouse SHALL be part of its fluid stock, next to Storage gantry tanks: the warehouse SHALL know per shelf its zone, fluid, amount and capacity. A change of a shelf (by bucket, by shuttle, by a pipe while standalone, by a bay joining or leaving) SHALL reach the warehouse within a second, without polling every tick.

#### Scenario: Bucket reaches the stock
- **WHEN** a player empties a water bucket into a docked shelf
- **THEN** the warehouse's water total grows by 1000 mB within a second

### Requirement: Fluids command lists racks
`/servus_warehouse fluids <pos>` SHALL list every Fluid Rack shelf of the warehouse with its port position, zone, fluid, amount and capacity after the gantries, and its totals SHALL include the shelves.

#### Scenario: Racks and gantries add up
- **WHEN** a Storage gantry holds 3000 mB of water and a docked Fluid Rack shelf holds 5000 mB of water
- **THEN** `/servus_warehouse fluids` lists 8000 mB of water, the gantry and the shelf

### Requirement: Docked shelves are tankers only
While a rail stands in front of a shelf's port bay, no bay of that shelf SHALL expose a fluid handler, so pipes neither fill nor drain it and show no fluid arm toward it. Without such a rail every bay SHALL expose the shelf's tank on every side, filling and draining it. Placing or breaking that rail, and the shelf changing, SHALL update every bay at once, without polling. Whether the rail belongs to an online warehouse SHALL NOT matter.

#### Scenario: Standalone rack as a tank
- **WHEN** a pump feeds a duct touching the side of a single bay with no rail in front of it
- **THEN** the bay fills with water

#### Scenario: Rail closes the section
- **WHEN** a rail is placed in front of the port bay of a shelf a duct is filling
- **THEN** the duct stops filling and loses its fluid arm toward the shelf

### Requirement: Buckets by hand
Using a filled fluid container on the front of any bay SHALL empty it into the shelf when the shelf takes that fluid. Using an empty container SHALL fill it from the shelf when the shelf holds at least one container's worth. Both SHALL work whether or not the shelf is docked. A player in creative mode SHALL keep the container and get the filled one too. A zoned shelf SHALL refuse by hand what its zone refuses. The rack SHALL open no GUI.

#### Scenario: Water bucket into a rack
- **WHEN** a player uses a water bucket on the front of an empty, unzoned bay
- **THEN** the shelf holds 1,000 mB of water and the player holds an empty bucket

#### Scenario: No GUI
- **WHEN** a player right clicks a Fluid Rack with an empty hand
- **THEN** nothing opens

### Requirement: Glass front shows the level
Each bay SHALL show a glass front over the whole bay with the shelf's fluid drawn as real fluid (its still texture and tint) from the bottom up to the shelf's fill share, the same height on every bay of the shelf. The client SHALL learn the level in steps (`level_steps` in `fluid_rack.json`, example 16) and only when a step changes. The frame SHALL be blackened wrought iron on every face, the boards SHALL show the crafting wood, and no two faces SHALL share a plane where they overlap.

#### Scenario: Half full
- **WHEN** a three bay shelf is filled to half
- **THEN** every bay shows fluid up to half the height of its glass front

#### Scenario: Empty
- **WHEN** a shelf is empty
- **THEN** its glass fronts show no fluid

### Requirement: Fluid Rack inspect
Using the Data Potato on any face of a Fluid Rack SHALL print a chat report for its shelf: the zone name or that it has none, the number of bays, the fluid with its amount against the shelf's capacity in mB or that it is empty with its capacity, and whether the shelf is docked. It SHALL open no GUI and change nothing.

#### Scenario: Section report
- **WHEN** a three bay shelf zoned "Cooling" holds 26,500 mB of water and a rail stands in front of its port, and a player uses the potato on its right bay
- **THEN** the chat shows zone "Cooling", 3 bays, water 26,500 of 48,000 mB, docked

### Requirement: Shelf rules are pure
The shelf's port bay, the split of a shelf's fluid into bay shares and the joining of a bay that carries a fluid SHALL have no Minecraft dependency and SHALL be covered by JUnit tests.

#### Scenario: Unit tested
- **WHEN** the unit tests run
- **THEN** they cover the port for one, two and three bays, even shares with a remainder, and joins refused for a different fluid

### Requirement: Fluid Rack shelf layer on top
Clicking the top face of a Fluid Rack with an empty Fluid Rack item, without sneaking, SHALL place a Fluid Rack on top of every bay of the clicked rack's shelf when every spot above is free and the held stack holds an item for every bay (always in creative), using one item per bay. The new bays SHALL take the facing and shelf boundaries below, so the new shelf gets its port in the same bay. A held item carrying fluid, a short stack or a taken spot SHALL place one rack as usual.

#### Scenario: Layer over a section of three
- **WHEN** a player holding 3 empty Fluid Racks clicks the top of a three bay shelf
- **THEN** a shelf of three stands on top with its port above the port below

#### Scenario: Item with fluid
- **WHEN** the held Fluid Rack item carries water
- **THEN** one rack is placed on the clicked bay
