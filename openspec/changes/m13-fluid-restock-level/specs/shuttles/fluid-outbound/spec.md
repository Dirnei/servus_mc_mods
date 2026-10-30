## MODIFIED Requirements

### Requirement: Buffer tank and keep
A Fluid Outbound SHALL have one tank of `fluid_station_tank_capacity` mB (in `warehouse.json`, example 16,000). The tank SHALL have a keep entry: a fluid, or none, an amount and a restock level, both from 0 up to the tank's capacity, the restock level never above the amount. A tank with a keep fluid SHALL take only that fluid. A tank without a keep fluid SHALL take nothing and SHALL keep what it already holds until it is drained.

#### Scenario: Keep fluid guards the tank
- **WHEN** the station keeps water and a bucket of lava is used on it
- **THEN** the tank takes no lava

#### Scenario: Keep cleared with fluid inside
- **WHEN** the tank holds 3,000 mB of lava and its keep fluid is cleared
- **THEN** the lava stays in the tank and can still be drained, and nothing new goes in

### Requirement: Keep row in the GUI
Above the gauge the GUI SHALL show a keep fluid picker: a button with the keep fluid and its icon, or "Keep nothing", opening a popup with a search field over "Keep nothing" and every registered source fluid with its icon. A pick SHALL set the keep fluid; "Keep nothing" SHALL clear it. While the popup is open nothing else SHALL react.

#### Scenario: Set a keep
- **WHEN** a player opens the picker, types "wat", picks Water and then drags the white arrow to 8 B
- **THEN** the station keeps 8,000 mB of water

#### Scenario: Keep nothing
- **WHEN** the station keeps water and the player picks "Keep nothing"
- **THEN** the station keeps nothing and the arrows and fields do nothing

#### Scenario: Limited to the capacity
- **WHEN** a player types 40 into the keep field of a station with a tank of 16,000 mB
- **THEN** the station keeps 16,000 mB

### Requirement: Restock lamp and state line
The station SHALL carry a connection lamp lit only while a rail links it to an online warehouse, and an activity lamp: green while no restock is needed or it keeps nothing, orange while restock runs are queued or under way, red while a kept fluid is missing from storage or no Tanker shuttle of the warehouse reaches the station, and dark while offline. The GUI state line SHALL name the kept fluid that cannot be restocked and why, or the offline reason.

#### Scenario: Missing fluid
- **WHEN** the station keeps 4 B of Mercury, its tank is empty and no Fluid Rack holds Mercury
- **THEN** the lamp is red and the GUI says Mercury is missing from storage

#### Scenario: No tanker reaches
- **WHEN** the station keeps water, its tank is empty, a Fluid Rack holds water and the warehouse has no Tanker shuttle that reaches the station
- **THEN** the lamp is red and the GUI says no Tanker shuttle reaches it

#### Scenario: Above the restock level
- **WHEN** the station keeps 8 B of water with a restock level of 4 B and its tank holds 5 B
- **THEN** the lamp is green and no tanker drives

### Requirement: Station GUI
Right clicking either half with an empty hand or an item other than a fluid container, the Data Potato and the carrot SHALL open a GUI with the keep fluid picker, one gauge with the two arrows and their fields, a mark at the keep amount and a second mark at the restock level, the side panel with only the Fluids layer, the state line and the player inventory.

#### Scenario: Keep mark
- **WHEN** the station keeps 8,000 mB of water with a restock level of 4,000 mB and holds 5,200 mB
- **THEN** its gauge shows water to 5,200 mB, a mark at 8,000 mB and a restock mark at 4,000 mB

### Requirement: Fluid Outbound inspect
Using the Data Potato on either half SHALL print one line for the tank: its fluid and amount against its capacity or that it is empty, and its keep fluid, amount and restock level or that it keeps nothing. It SHALL open no GUI and change nothing.

#### Scenario: Report
- **WHEN** the station keeps 8,000 mB of water with a restock level of 4,000 mB and holds 5,200 mB
- **THEN** the chat shows one line: water 5,200 of 16,000 mB, keeping 8,000 mB of water, restocking at 4,000 mB

## ADDED Requirements

### Requirement: Keep and restock levels move each other
Raising the restock level above the keep amount SHALL raise the keep amount with it, up to the capacity; lowering the keep amount below the restock level SHALL lower the restock level with it. Keeping a fluid where none was kept SHALL start at the full capacity with a restock level of 1,000 mB. Changing the keep fluid SHALL keep both levels.

#### Scenario: Keep drags the restock level down
- **WHEN** a station keeps 8,000 mB of water with a restock level of 6,000 mB and the keep amount is set to 4,000 mB
- **THEN** the restock level is 4,000 mB too

#### Scenario: Restock level pushes the keep up
- **WHEN** a station keeps 8,000 mB of water with a restock level of 6,000 mB and the restock level is set to 10,000 mB
- **THEN** the keep amount is 10,000 mB too

#### Scenario: New keep starts full
- **WHEN** a station that kept nothing is set to keep water
- **THEN** it keeps 16,000 mB of water in a tank of 16,000 mB with a restock level of 1,000 mB

#### Scenario: Changing the fluid keeps the levels
- **WHEN** a station keeps 8,000 mB of water with a restock level of 2,000 mB and the keep fluid is changed to lava
- **THEN** it keeps 8,000 mB of lava with a restock level of 2,000 mB

### Requirement: Level arrows and fields
Left of the gauge a white arrow SHALL point at the keep amount and right of it an orange arrow at the restock level. Dragging an arrow SHALL set its value in whole buckets; the mouse wheel over it SHALL step 1 B, with shift 10 B. A field at a fixed place on each side SHALL show the value in buckets and set a typed number on Enter, limited to the capacity. Without a keep fluid both SHALL do nothing.

#### Scenario: Wheel steps
- **WHEN** a station keeps 16,000 mB, restocks at 4,000 mB and the player turns the wheel one notch up over the orange arrow while holding shift
- **THEN** the restock level is 14,000 mB

#### Scenario: Typed restock level
- **WHEN** a station keeps 16,000 mB and the player types 3 into the restock field and presses Enter
- **THEN** the station restocks at 3,000 mB and the orange arrow points at 3 B
