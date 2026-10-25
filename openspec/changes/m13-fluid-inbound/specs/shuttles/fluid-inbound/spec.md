## Purpose

The Fluid Inbound is the warehouse's fluid intake: pumps and pipes fill its buffer tanks, and Tanker shuttles carry the fluid to Fluid Racks, like the Inbound Station does for items.

## ADDED Requirements

### Requirement: Two blocks high station
The Fluid Inbound SHALL be one item that places a station two blocks high with a horizontal facing toward the player, when both spaces are free and the block below has a solid top; otherwise nothing SHALL be placed and the player SHALL be told why.

#### Scenario: Placed on the floor
- **WHEN** a player places a Fluid Inbound on a stone floor with two free blocks of room
- **THEN** a station two blocks high stands there with its front toward the player

#### Scenario: No room
- **WHEN** a block stands where the upper half would go
- **THEN** nothing is placed and the player is told there is no room

### Requirement: Breaking keeps tanks and sides
Breaking either half SHALL remove both and drop one item that keeps the tanks and the side configuration; sneaking with the Configuration Carrot on either half SHALL dismantle it the same way. Placing such an item SHALL bring the tanks and sides back.

#### Scenario: Break keeps the fluid
- **WHEN** a player breaks the upper half of a Fluid Inbound holding 6,000 mB of water in tank 1 and places the dropped item again
- **THEN** both halves of the old station are gone and the new one holds 6,000 mB of water in tank 1 with the same sides

### Requirement: Buffer tanks
A Fluid Inbound SHALL have `fluid_station_tanks` tanks (in `data/<ns>/shuttles/warehouse.json`, example 3) of `fluid_station_tank_capacity` mB each (example 8,000). Each tank SHALL hold one fluid at a time and refuse other fluids until it is empty. The content SHALL be kept when the world is saved.

#### Scenario: Two fluids in two tanks
- **WHEN** a pipe brings water and then lava into an empty Fluid Inbound through a side on All
- **THEN** tank 1 holds the water and tank 2 the lava

### Requirement: Fluid sides take fluid in
The station SHALL have only a Fluids side layer with the modes Input, Pull and Off, Input on All by default, and its front fixed. Both halves SHALL share it: the sides of both halves count as the station's left, right and back, the top of the upper half as its top and the bottom of the lower half as its bottom. Input and Pull sides SHALL take fluid in and give none back; a Pull side SHALL take from the neighbour at `push_interval`. Nothing SHALL be drained through any side.

#### Scenario: Pump fills the station
- **WHEN** a pump on water feeds a duct that touches the back of the upper half
- **THEN** the station fills with water and the duct gets nothing back

#### Scenario: Pull from a Fluid Tank
- **WHEN** the left side is Pull on tank 2 and a Fluid Tank holding lava touches the lower half's left side
- **THEN** the station takes lava into tank 2 at its push interval

#### Scenario: Front closed
- **WHEN** a duct with a fluid line is placed against the front of either half
- **THEN** the duct finds no fluid endpoint there

### Requirement: Joins only through the front rail
The station SHALL be a member of the warehouse network only through a rail directly in front of its lower half, where its copper port sits at the height of a tanker's coupling. Rails SHALL dock onto that face only. It SHALL NOT link to touching controllers, racks, garages, stations or elevator segments and SHALL pass no connection on. Its tanks SHALL NOT count as fluid stock.

#### Scenario: Rail in front
- **WHEN** a Fluid Inbound is placed with a rail of an online warehouse in front of its lower half
- **THEN** the rail docks toward the station and its connection lamp is on

#### Scenario: Not stock
- **WHEN** a Fluid Inbound of the warehouse holds 8,000 mB of water
- **THEN** the water is not part of the warehouse's fluid totals or its fluid fill

### Requirement: Put-away by tanker
Each buffer tank SHALL be a source of tanker put-away runs by the rules for fluid put-away runs: a run is queued when the tank's free fluid is at least `tanker_min_load` or the tank is full, a Tanker shuttle reaches the station and a fluid store has room. The tanker SHALL stop at the rail in front of the port and dock its coupling there. The buffer SHALL stay untouched while the warehouse is offline.

#### Scenario: Pump to rack
- **WHEN** a pump fills a Fluid Inbound tank with 4,000 mB of water and the warehouse has an empty Fluid Rack section and a Tanker shuttle
- **THEN** the tanker docks at the station's port, takes the water and brings it into the rack

### Requirement: Lamps and state line
The station SHALL carry a connection lamp lit only while a rail links it to an online warehouse, and an activity lamp: green while every tank is empty, orange while runs for its tanks are queued or under way or a tank waits below `tanker_min_load`, red while a tank holds fluid that no fluid store has room for or that no Tanker shuttle can reach it for, and dark while the warehouse is offline.

#### Scenario: No room
- **WHEN** a Fluid Inbound holds lava and every Fluid Rack is zoned to water or full
- **THEN** its activity lamp is red and its GUI says lava has no room

#### Scenario: No tanker
- **WHEN** a Fluid Inbound holds water and the warehouse has no Tanker shuttle that reaches it
- **THEN** its activity lamp is red and its GUI says no Tanker shuttle reaches it

### Requirement: Station GUI
Right clicking either half with an empty hand or a non fluid item other than the Data Potato and the carrot SHALL open a GUI with a gauge per tank showing its fluid and amount against its capacity, the Foundation side panel with only the Fluids layer, a state line and the player inventory. The state line SHALL show only while the warehouse is offline, naming the reason, or while a tank is red, naming the fluid and why.

#### Scenario: Gauges and panel
- **WHEN** a player opens a Fluid Inbound holding 6,000 mB of water in tank 1
- **THEN** the GUI shows three gauges, the first two thirds full of water, and the side panel without tabs

### Requirement: Buckets by hand
Using a filled fluid container on either half SHALL empty it into the tanks by the fill order of a side on All. Using an empty container SHALL fill it from the first tank that holds at least one container's worth. A player in creative mode SHALL keep the container and get the filled one too.

#### Scenario: Water bucket
- **WHEN** a player uses a water bucket on an empty Fluid Inbound
- **THEN** tank 1 holds 1,000 mB of water and the player holds an empty bucket

### Requirement: Fluid Inbound inspect
Using the Data Potato on either half SHALL print a chat report with one line per tank: its fluid, amount and capacity in mB, or that it is empty with its capacity. It SHALL open no GUI and change nothing.

#### Scenario: Report
- **WHEN** a Fluid Inbound holds 6,000 mB of water in tank 1 and nothing else and a player uses the potato on it
- **THEN** the chat shows tank 1 water 6,000 of 8,000 mB, and tanks 2 and 3 empty of 8,000 mB

### Requirement: Fluid Inbound look
The station SHALL be a steel body two blocks high with a teal band at the top. It SHALL have one sight glass per tank on the front of the upper half, each filled to its tank's share with the fluid drawn as real fluid in steps, the connection and activity lamps on the front, and a copper port on the front of the lower half. Every face of moving and metal parts SHALL use the family's steel textures, and no two faces SHALL share a plane where they overlap.

#### Scenario: Sight glass follows a tank
- **WHEN** tank 2 of a Fluid Inbound is filled to half with lava
- **THEN** the middle sight glass shows lava up to half its height

### Requirement: Fluid Inbound is obtainable
The Fluid Inbound SHALL be craftable from vanilla materials and Foundation parts (example: a Fluid Tank, iron ingots, a copper ingot and a cyan dye). The creative tab and the recipe viewer SHALL list it.

#### Scenario: Crafting
- **WHEN** a player crafts a Fluid Tank, iron ingots, a copper ingot and a cyan dye in the Fluid Inbound pattern
- **THEN** the result is one Fluid Inbound
