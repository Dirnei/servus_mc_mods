## Purpose

The Fluid Outbound is where fluids leave the warehouse: it keeps a configured set of fluids stocked in its tanks, restocked by Tanker shuttles from Fluid Racks, and hands them to machines and pipes, like the Outbound Station does for items.

## ADDED Requirements

### Requirement: Two blocks high station
The Fluid Outbound SHALL be placed, broken and dismantled like the Fluid Inbound: one item placing a station two blocks high facing the player when both spaces are free and the block below has a solid top, both halves removed together, and one dropped item that keeps the tanks, the keep row and the side configuration and brings them back when placed.

#### Scenario: Break keeps the keep row
- **WHEN** a Fluid Outbound keeping 8,000 mB of water in tank 1 is broken and placed again
- **THEN** tank 1 still keeps 8,000 mB of water and holds what it held

### Requirement: Buffer tanks and keep row
A Fluid Outbound SHALL have `fluid_station_tanks` tanks of `fluid_station_tank_capacity` mB (in `warehouse.json`). Each tank SHALL have a keep entry: a fluid, or none, and an amount from 0 up to the tank's capacity. A tank with a keep fluid SHALL take only that fluid. A tank without a keep fluid SHALL take nothing and SHALL keep what it already holds until it is drained.

#### Scenario: Keep fluid guards the tank
- **WHEN** tank 1 keeps water and a bucket of lava is used on the station
- **THEN** tank 1 takes no lava

#### Scenario: Keep cleared with fluid inside
- **WHEN** tank 2 holds 3,000 mB of lava and its keep fluid is cleared
- **THEN** the lava stays in tank 2 and can still be drained, and nothing new goes in

### Requirement: Keep row in the GUI
The GUI SHALL show a keep cell above each tank's gauge. Clicking a cell with a filled fluid container on the cursor SHALL set that fluid as the tank's keep fluid without using the container; clicking it with an empty cursor SHALL clear it. Buttons under each cell SHALL change its amount by -1 B, +1 B, -10 B and +10 B, limited to 0 and the tank's capacity.

#### Scenario: Set a keep
- **WHEN** a player clicks the first keep cell with a water bucket on the cursor and then presses +1 B eight times
- **THEN** tank 1 keeps 8,000 mB of water and the player still holds the water bucket

#### Scenario: Limited to the capacity
- **WHEN** a keep stands at 8,000 mB in a tank of 8,000 mB and the player presses +10 B
- **THEN** it stays at 8,000 mB

### Requirement: Fluid sides hand fluid out
The station SHALL have only a Fluids side layer with the modes Push, Output and Off, shared by both halves as on the Fluid Inbound, the back on Push with All by default, the other sides Off, and the front fixed. Push and Output sides SHALL let neighbours drain the tanks they reach and SHALL refuse fills; a Push side SHALL push into the neighbour at `push_interval`, fluid the neighbour refuses staying in the tank.

#### Scenario: Push into a machine
- **WHEN** tank 1 holds water, the back is Push on All and a Fluid Tank touches the back of the lower half
- **THEN** water flows into the Fluid Tank at the station's push interval

#### Scenario: Output waits for a pipe
- **WHEN** the left side is Output on tank 2 holding lava and a duct on pull touches it
- **THEN** the duct drains lava and nothing flows into the station through that side

### Requirement: Joins only through the front rail
The station SHALL be a member of the warehouse network only through a rail directly in front of its lower half, where its port sits at tanker coupling height, as for the Fluid Inbound. Its tanks SHALL NOT count as fluid stock.

#### Scenario: Not stock
- **WHEN** a Fluid Outbound of the warehouse holds 6,000 mB of water
- **THEN** the water is not part of the warehouse's fluid totals

### Requirement: Restock lamp and state line
The station SHALL carry a connection lamp lit only while a rail links it to an online warehouse, and an activity lamp: green while every kept tank holds its keep amount, orange while restock runs are queued or under way, red while a kept fluid is missing from storage or no Tanker shuttle of the warehouse reaches the station, and dark while offline. The GUI state line SHALL name the first kept fluid that cannot be restocked and why, or the offline reason.

#### Scenario: Missing fluid
- **WHEN** tank 1 keeps 4 B of Mercury and no Fluid Rack holds Mercury
- **THEN** the lamp is red and the GUI says Mercury is missing from storage

#### Scenario: No tanker reaches
- **WHEN** tank 1 keeps water, a Fluid Rack holds water and the warehouse has no Tanker shuttle that reaches the station
- **THEN** the lamp is red and the GUI says no Tanker shuttle reaches it

### Requirement: Station GUI
Right clicking either half with an empty hand or a non fluid item other than the Data Potato and the carrot SHALL open a GUI with the keep row, a gauge per tank with a mark at its keep amount, the side panel with only the Fluids layer, the state line and the player inventory.

#### Scenario: Keep mark
- **WHEN** tank 1 keeps 8,000 mB of water and holds 5,200 mB
- **THEN** its gauge shows water to 5,200 mB and a mark at 8,000 mB

### Requirement: Buckets by hand
Using an empty fluid container on either half SHALL fill it from the first tank holding at least one container's worth. Using a filled container SHALL empty it into a tank that keeps that fluid and has room. A player in creative mode SHALL keep the container and get the filled one too.

#### Scenario: Take water out
- **WHEN** tank 1 holds 5,200 mB of water and a player uses an empty bucket on the station
- **THEN** the player gets a water bucket and tank 1 holds 4,200 mB

### Requirement: Fluid Outbound inspect
Using the Data Potato on either half SHALL print one line per tank: its fluid and amount against its capacity or that it is empty, and its keep fluid and amount or that it keeps nothing. It SHALL open no GUI and change nothing.

#### Scenario: Report
- **WHEN** tank 1 keeps 8,000 mB of water and holds 5,200 mB and the other tanks keep nothing and are empty
- **THEN** the chat shows tank 1 water 5,200 of 8,000 mB keeping 8,000 mB of water, and tanks 2 and 3 empty, keeping nothing

### Requirement: Fluid Outbound look
The station SHALL look like the Fluid Inbound with an amber band instead of the teal one: a steel body two blocks high, one sight glass per tank filled to its share, the connection and activity lamps and a copper port on the lower front. The texture and plane rules of the Fluid Inbound SHALL hold.

#### Scenario: Tell them apart
- **WHEN** a player sees a Fluid Inbound and a Fluid Outbound side by side
- **THEN** the Inbound has a teal band and the Outbound an amber band

### Requirement: Fluid Outbound is obtainable
The Fluid Outbound SHALL be craftable from vanilla materials and Foundation parts (example: a Fluid Tank, iron ingots, a copper ingot and an orange dye). The creative tab and the recipe viewer SHALL list it.

#### Scenario: Crafting
- **WHEN** a player crafts a Fluid Tank, iron ingots, a copper ingot and an orange dye in the Fluid Outbound pattern
- **THEN** the result is one Fluid Outbound
