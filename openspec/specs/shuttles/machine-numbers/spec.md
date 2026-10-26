# shuttles/machine-numbers Specification

## Purpose
Machine numbers give every named machine of a warehouse a short number per kind ("Field Head #2", "CPU #1"), handed out by the warehouse when the machine joins its rails, its fields or its data line, so labels in GUIs, reports and messages tell machines apart without coordinates.

## Requirements

### Requirement: A machine is numbered by its warehouse
A named machine without a number, or with a number from another warehouse, SHALL get a number when it belongs to an online warehouse. A machine SHALL keep its number while the warehouse is offline and while the machine is out of the warehouse.

#### Scenario: Offline warehouse
- **WHEN** a Shuttle Garage stands on a rail of a network without a controller
- **THEN** it gets no number until a controller makes the warehouse online

#### Scenario: Old world
- **WHEN** a world saved before machine numbers loads a warehouse with two Field Heads, a Shuttle Garage and a Crafting Station on its data line
- **THEN** they become Field Head #1 and #2, Garage #1 and Crafting Station #1 once the warehouse is online, and the warehouse works as before

### Requirement: How a machine belongs to a warehouse
A station, a Shuttle Garage and an elevator shaft SHALL belong to the warehouse whose rails they join. A Field Head SHALL belong to the warehouse whose rails reach the field it serves. A Crafting Station, Crafting CPU, Machine Adapter, Coal Generator, Burner Pump and an Alchemy machine SHALL belong to the warehouse whose data line it is a member of. A Packing Station on no warehouse's rails SHALL belong to the warehouse whose data line it is a member of; one on a warehouse's rails keeps that warehouse's number.

#### Scenario: Packing Station only on the data line
- **WHEN** a Packing Station without rails is cabled to the data line of a warehouse that has Packing #1
- **THEN** it becomes Packing #2

#### Scenario: Generator joins the data line
- **WHEN** a Coal Generator is cabled to a warehouse's data line
- **THEN** it becomes Coal Generator #1

#### Scenario: Field Head joins
- **WHEN** a Field Head's field gets a rail link to a warehouse that already has Field Head #1
- **THEN** the Field Head becomes Field Head #2

#### Scenario: Garage joins
- **WHEN** a player places a Shuttle Garage at a rail of a warehouse without a garage
- **THEN** it becomes Garage #1

#### Scenario: Crafting Station joins the data line
- **WHEN** a Crafting Station is connected by a data cable to the data line of a warehouse whose last Crafting Station is #1
- **THEN** it becomes Crafting Station #2

#### Scenario: CPU formed
- **WHEN** a player forms a Crafting CPU on a warehouse's data line that has no CPU yet
- **THEN** it becomes CPU #1

#### Scenario: Machine Adapter joins
- **WHEN** a Machine Adapter facing a furnace joins a warehouse's data line
- **THEN** it becomes Adapter #1

#### Scenario: Alchemy machine joins
- **WHEN** a Coagulator and an Athanor join the data line of a warehouse
- **THEN** they become Coagulator #1 and Athanor #1

#### Scenario: Data line without warehouse
- **WHEN** a Crafting Station is on a data line that reaches no warehouse
- **THEN** it has no number and reads "Crafting Station", or its name

### Requirement: An elevator shaft has one number
An elevator shaft SHALL have one number and one name for all its segments, kept with the shaft's Speed upgrades on its lowest segment. When the lowest segment is removed, the number and name SHALL move to the segment above with the upgrades; when a segment is placed below the shaft, they SHALL move down to it.

#### Scenario: Shaft numbered once
- **WHEN** a shaft of five segments joins a warehouse without lifts
- **THEN** every segment's GUI shows Lift #1 and the next shaft becomes Lift #2

#### Scenario: Lowest segment removed
- **WHEN** the lowest segment of "Ore Lift (Lift #1)" is broken
- **THEN** the rest of the shaft still reads "Ore Lift (Lift #1)"

#### Scenario: Two shafts joined
- **WHEN** a segment placed between Lift #1 below and "Ore Lift (Lift #2)" above joins them into one shaft
- **THEN** the shaft reads "Lift #1" and the upper shaft's name and number are gone

### Requirement: Numbers count per kind and are never reused
Each warehouse SHALL count every named machine kind on its own, each counter starting at 1, and SHALL never give a number twice for the same kind, also after the machine that had it was broken. Machines that get their numbers at the same moment SHALL be numbered in a fixed order of their positions.

#### Scenario: Kinds are independent
- **WHEN** a warehouse has Inbound #1, Outbound #1 and Packing #1 and a Field Head joins
- **THEN** the Field Head is Field Head #1

#### Scenario: Number not reused
- **WHEN** Garage #2 is broken and a new Shuttle Garage is placed at the same warehouse
- **THEN** the new garage is Garage #3

#### Scenario: Survives a reload
- **WHEN** CPU #1 and CPU #2 exist, the world is saved and loaded again, and a new CPU is formed on the same line
- **THEN** the old CPUs keep their numbers and the new one is CPU #3

### Requirement: The number follows the machine
A machine that keeps its number in its dismantled item SHALL keep that number when placed back into the warehouse that gave it, and SHALL get a new number from another warehouse while keeping its name.

#### Scenario: Moved within the warehouse
- **WHEN** the player dismantles Garage #2 with the carrot and places it at another rail of the same warehouse
- **THEN** it is still Garage #2 and the next new garage is #3

#### Scenario: Moved to another warehouse
- **WHEN** the player dismantles Crafting Station "Sticks (Crafting Station #2)" and places it on the data line of a second warehouse whose last Crafting Station is #4
- **THEN** it reads "Sticks (Crafting Station #5)"

### Requirement: The controller carries the warehouse's count
The warehouse's identity and the counters of every kind SHALL travel with the Warehouse Controller when it is dismantled with the carrot. A controller broken any other way SHALL start a new warehouse whose counters start at 1.

#### Scenario: Controller moved
- **WHEN** the player dismantles the controller with the carrot and places it elsewhere on the same rails
- **THEN** every machine keeps its number and the next new Field Head continues the count

#### Scenario: Controller broken
- **WHEN** the controller is broken with a pickaxe and a new one is placed on the same rails
- **THEN** every kind is numbered again from #1

### Requirement: CPU and production messages name machines by label
Every tab of the Production Terminal (machine rows, Packing Station rows, CPU rows and their tooltips, job states and buffer lines), the Crafting CPU's chat summary and any message that names the CPU of an order SHALL name producers, Packing Stations and CPUs by their labels; positions appear only as an extra "At x, y, z" tooltip line. A Machine Adapter without a name SHALL read as its faced machine followed by its numbered label, for example "Furnace (Adapter #1)".

#### Scenario: Producer row
- **WHEN** the Production Terminal lists Crafting Station "Sticks (Crafting Station #2)" and Crafting Station #1
- **THEN** the rows read "Sticks (Crafting Station #2)" and "Crafting Station #1"

#### Scenario: Step blocked at a producer
- **WHEN** an order step cannot start because Crafting Station "Sticks (Crafting Station #2)" has no room
- **THEN** the step's state names "Sticks (Crafting Station #2)"

#### Scenario: CPU summary
- **WHEN** a player uses the carrot on a formed CPU #2 that runs 3 of 4 orders
- **THEN** the summary starts with "CPU #2"

#### Scenario: Packing and CPU rows
- **WHEN** the Production Terminal shows Packing #1 on the Machines tab and CPU #1 on the CPUs tab
- **THEN** they read "Packing #1" and "CPU #1, 2x2x2", not "Packing Station" or "CPU at x, y, z"

#### Scenario: Unnamed adapter row
- **WHEN** Adapter #1 faces a furnace and has no name
- **THEN** its machine row reads "Furnace (Adapter #1)"

### Requirement: Shuttle report names machines by label
The Data Potato's shuttle report SHALL name the machine a shuttle stops at by its label instead of coordinates when a named machine is there, the shaft a shuttle waits at or rides by its lift label and floor, and a shuttle at field work by its Field Head's label.

#### Scenario: Waiting at a lift
- **WHEN** a shuttle waits for the car of "Ore Lift (Lift #1)" on floor 3
- **THEN** the report reads that it waits for "Ore Lift (Lift #1)" on floor 3

#### Scenario: Stopped at a station
- **WHEN** a shuttle stands at the rail of Outbound #2 unloading
- **THEN** the report names "Outbound #2" and no coordinates

#### Scenario: Field work
- **WHEN** a harvester works the field of "Wheat North (Field Head #2)"
- **THEN** the report names "Wheat North (Field Head #2)"

### Requirement: Labels in stock rule states and the export picker
Stock rule states and the export picker SHALL keep naming stations by their labels from this shared rule, unchanged for the player.

#### Scenario: Export picker
- **WHEN** a player opens the export picker of a stock rule and Outbound #2 is named "Compost Dock"
- **THEN** it lists "Compost Dock (Outbound #2)"
