# shuttles/fluid-inbound Specification

## Purpose
The Fluid Inbound is the warehouse's fluid intake: pumps and pipes fill its buffer tank, and Tanker shuttles carry the fluid to Fluid Racks, like the Inbound Station does for items.

## Requirements

### Requirement: Two blocks high station
The Fluid Inbound SHALL be one item that places a station two blocks high with a horizontal facing chosen by the shared rail facing rule (see `shuttles/rail-facing`) from the rails beside its lower half, so its front faces a rail or else the player, when both spaces are free and the block below has a solid top; otherwise nothing SHALL be placed and the player SHALL be told why.

#### Scenario: Placed on the floor
- **WHEN** a player places a Fluid Inbound on a stone floor with two free blocks of room
- **THEN** a station two blocks high stands there with its front toward the player

#### Scenario: Placed beside a rail
- **WHEN** a player looking north places a Fluid Inbound with a rail only on the west side of its lower half
- **THEN** its front faces west, towards the rail

#### Scenario: No room
- **WHEN** a block stands where the upper half would go
- **THEN** nothing is placed and the player is told there is no room

### Requirement: Breaking keeps tanks and sides
Breaking either half SHALL remove both and drop one item that keeps the tank, the side configuration and the wood; sneaking with the Configuration Carrot on either half SHALL dismantle it the same way. Placing such an item SHALL bring the tank, sides and wood back.

#### Scenario: Break keeps the fluid
- **WHEN** a player breaks the upper half of a Fluid Inbound holding 6,000 mB of water and places the dropped item again
- **THEN** both halves of the old station are gone and the new one holds 6,000 mB of water with the same sides

#### Scenario: Break keeps the wood
- **WHEN** a player breaks a birch Fluid Inbound and places the dropped item again
- **THEN** the new station is birch on its upper half

### Requirement: Buffer tank
A Fluid Inbound SHALL have one tank of `fluid_station_tank_capacity` mB (in `data/<ns>/shuttles/warehouse.json`, example 16,000). The tank SHALL hold one fluid at a time and refuse other fluids until it is empty. The content SHALL be kept when the world is saved.

#### Scenario: One fluid at a time
- **WHEN** a pipe brings water and then lava into an empty Fluid Inbound through a side on Input
- **THEN** the tank holds the water and the lava stays in the pipe

### Requirement: Fluid sides take fluid in
The station SHALL have only a Fluids side layer with the modes Input, Pull and Off, Input by default, and its front fixed. With one tank, every Fluids side SHALL reach that tank and offer no tank choice. Both halves SHALL share the layer: the sides of both halves count as the station's left, right and back, the top of the upper half as its top and the bottom of the lower half as its bottom. Input and Pull sides SHALL take fluid in and give none back; a Pull side SHALL take from the neighbour at `push_interval`. Nothing SHALL be drained through any side.

#### Scenario: Pump fills the station
- **WHEN** a pump on water feeds a duct that touches the back of the upper half
- **THEN** the station fills with water and the duct gets nothing back

#### Scenario: Pull from a Fluid Tank
- **WHEN** the left side is Pull and a Fluid Tank holding lava touches the lower half's left side
- **THEN** the station takes lava at its push interval

#### Scenario: Front closed
- **WHEN** a duct with a fluid line is placed against the front of either half
- **THEN** the duct finds no fluid endpoint there

### Requirement: Joins only through the front rail
The station SHALL be a member of the warehouse network only through a rail directly in front of its lower half, where tankers stop. Rails SHALL dock onto that face only. It SHALL NOT link to touching controllers, racks, garages, stations or elevator segments and SHALL pass no connection on. Its tank SHALL NOT count as fluid stock.

#### Scenario: Rail in front
- **WHEN** a Fluid Inbound is placed with a rail of an online warehouse in front of its lower half
- **THEN** the rail docks toward the station and its connection lamp is on

#### Scenario: Not stock
- **WHEN** a Fluid Inbound of the warehouse holds 8,000 mB of water
- **THEN** the water is not part of the warehouse's fluid totals or its fluid fill

### Requirement: Put-away by tanker
The buffer tank SHALL be a source of tanker put-away runs by the rules for fluid put-away runs: a run is queued when the tank's free fluid is at least `tanker_min_load` or the tank is full, a Tanker shuttle reaches the station and a fluid store has room. The tanker SHALL stop at the rail in front of the station's lower half. The buffer SHALL stay untouched while the warehouse is offline.

#### Scenario: Pump to rack
- **WHEN** a pump fills a Fluid Inbound with 4,000 mB of water and the warehouse has an empty Fluid Rack section and a Tanker shuttle
- **THEN** the tanker stops in front of the station, takes the water and brings it into the rack

### Requirement: Lamps and state line
The station SHALL carry a connection lamp lit only while a rail links it to an online warehouse, and an activity lamp: green while the tank is empty, orange while runs for its tank are queued or under way or the tank waits below `tanker_min_load`, red while the tank holds fluid that no fluid store has room for or that no Tanker shuttle can reach it for, and dark while the warehouse is offline.

#### Scenario: No room
- **WHEN** a Fluid Inbound holds lava and every Fluid Rack is zoned to water or full
- **THEN** its activity lamp is red and its GUI says lava has no room

#### Scenario: No tanker
- **WHEN** a Fluid Inbound holds water and the warehouse has no Tanker shuttle that reaches it
- **THEN** its activity lamp is red and its GUI says no Tanker shuttle reaches it

### Requirement: Station GUI
Right clicking either half with an empty hand or an item other than a fluid container, the Data Potato and the carrot SHALL open a GUI with one gauge showing the tank's fluid and amount against its capacity, the Foundation side panel with only the Fluids layer, a state line and the player inventory. The state line SHALL show only while the warehouse is offline, naming the reason, or while the station is red, naming the fluid and why.

#### Scenario: Gauges and panel
- **WHEN** a player opens a Fluid Inbound holding 8,000 mB of water in a tank of 16,000 mB
- **THEN** the GUI shows one gauge half full of water, and the side panel without tabs and without tank chips

### Requirement: Buckets by hand
Using a filled fluid container on either half SHALL empty it into the tank when the tank is empty or holds the same fluid and has room. Using an empty container SHALL fill it from the tank when it holds at least one container's worth. A player in creative mode SHALL keep the container and get the filled one too.

#### Scenario: Water bucket
- **WHEN** a player uses a water bucket on an empty Fluid Inbound
- **THEN** the tank holds 1,000 mB of water and the player holds an empty bucket

### Requirement: Fluid Inbound inspect
Using the Data Potato on either half SHALL print a chat report with one line for the tank: its fluid, amount and capacity in mB, or that it is empty with its capacity. It SHALL open no GUI and change nothing.

#### Scenario: Report
- **WHEN** a Fluid Inbound holds 6,000 mB of water and a player uses the potato on it
- **THEN** the chat shows water 6,000 of 16,000 mB in one line

### Requirement: Fluid Inbound look
The upper half SHALL be the Inbound Station block as it is, with its front, sides and top and its boards in the station's wood. Its front lamps SHALL show the station's state: dark while the warehouse is offline, else idle, working or blocked as the activity lamp says. The lower half SHALL be one tank with glass on all four sides inside a 2 px iron frame, its fluid drawn as real fluid up to the tank's share in steps and seen from every side. The station SHALL have no port. Every metal part SHALL use the family's metal textures on every face, and no two faces SHALL share a plane where they overlap.

#### Scenario: Glass follows the tank
- **WHEN** the tank of a Fluid Inbound is filled to half with lava
- **THEN** lava shows up to half the height of the lower half's glass on all four sides

#### Scenario: Station on a tank
- **WHEN** a player looks at a placed spruce Fluid Inbound
- **THEN** the upper half looks like a spruce Inbound Station and the lower half like a glass tank

#### Scenario: Lamps on the upper front
- **WHEN** the Fluid Inbound's activity lamp turns red
- **THEN** the Inbound Station front of its upper half shows the blocked lamp

### Requirement: Swing hose arm
A swing hose arm (copper stub and swivel, steel boom, dark hose, copper head) SHALL sit on the front where the halves meet, the boom upright against the upper half at rest. For a Tanker shuttle's stop in front, the boom SHALL swing down level over the rail, the hose SHALL lower the head onto the filler cap, and both SHALL go back up by the stop's end, or from where they are when it ends early. The arm SHALL reach into neither station nor tanker; the look's rules SHALL hold while it moves.

#### Scenario: Arm docks onto the tanker
- **WHEN** a Tanker shuttle stops in front of a Fluid Inbound to take water
- **THEN** the station's boom swings down over the rail, its head lowers onto the tanker's filler cap, and the arm stands upright again when the tanker drives on

#### Scenario: Arm at rest
- **WHEN** no tanker stands in front of a Fluid Inbound
- **THEN** its boom stands upright against the front of the upper half

#### Scenario: Recall during a stop
- **WHEN** the tanker's garage recalls its shuttles while the arm's head sits on the filler cap
- **THEN** the arm goes back up from there without jumping

### Requirement: Fluid Inbound is obtainable
The Fluid Inbound SHALL be crafted shaped from an Inbound Station on top of a Fluid Tank, one above the other, and SHALL keep the wood of that Inbound Station. The creative tab and the recipe viewer SHALL list it.

#### Scenario: Crafting
- **WHEN** a player crafts a spruce Inbound Station on top of a Fluid Tank
- **THEN** the result is one spruce Fluid Inbound
