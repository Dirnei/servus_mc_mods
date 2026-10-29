## Purpose

The Fluid Outbound is where fluids leave the warehouse: it keeps a configured fluid stocked in its tank, restocked by Tanker shuttles from Fluid Racks, and hands it to machines and pipes, like the Outbound Station does for items.

## ADDED Requirements

### Requirement: Two blocks high station
The Fluid Outbound SHALL be placed, broken and dismantled like the Fluid Inbound: one item placing a station two blocks high facing the player when both spaces are free and the block below has a solid top, both halves removed together, and one dropped item that keeps the tank, the keep, the side configuration and the wood and brings them back when placed.

#### Scenario: Break keeps the keep
- **WHEN** a Fluid Outbound keeping 8,000 mB of water is broken and placed again
- **THEN** it still keeps 8,000 mB of water and holds what it held

### Requirement: Buffer tank and keep
A Fluid Outbound SHALL have one tank of `fluid_station_tank_capacity` mB (in `warehouse.json`, example 16,000). The tank SHALL have a keep entry: a fluid, or none, and an amount from 0 up to the tank's capacity. A tank with a keep fluid SHALL take only that fluid. A tank without a keep fluid SHALL take nothing and SHALL keep what it already holds until it is drained.

#### Scenario: Keep fluid guards the tank
- **WHEN** the station keeps water and a bucket of lava is used on it
- **THEN** the tank takes no lava

#### Scenario: Keep cleared with fluid inside
- **WHEN** the tank holds 3,000 mB of lava and its keep fluid is cleared
- **THEN** the lava stays in the tank and can still be drained, and nothing new goes in

### Requirement: Keep row in the GUI
Above the gauge the GUI SHALL show a keep fluid picker: a button with the keep fluid and its icon, or "Keep nothing", opening a popup with a search field over "Keep nothing" and every registered source fluid with its icon. A pick SHALL set the keep fluid; "Keep nothing" SHALL clear it. While the popup is open nothing else SHALL react. Buttons under the gauge SHALL step the keep amount by -1 B, +1 B, -10 B and +10 B within 0 and the capacity.

#### Scenario: Set a keep
- **WHEN** a player opens the picker, types "wat", picks Water and then presses +1 B eight times
- **THEN** the station keeps 8,000 mB of water

#### Scenario: Keep nothing
- **WHEN** the station keeps water and the player picks "Keep nothing"
- **THEN** the station keeps nothing and the step buttons do nothing

#### Scenario: Limited to the capacity
- **WHEN** a keep stands at 16,000 mB in a tank of 16,000 mB and the player presses +10 B
- **THEN** it stays at 16,000 mB

### Requirement: Keep fluid from a container or by id
A click on the picker button with a filled fluid container on the cursor SHALL keep that container's fluid, leave the container on the cursor and open no popup. The server SHALL ignore a pick that names no registered source fluid.

#### Scenario: Container shortcut
- **WHEN** a player clicks the picker button with a lava bucket on the cursor
- **THEN** the station keeps lava, no popup opens and the player still holds the lava bucket

#### Scenario: Unknown fluid refused
- **WHEN** the server gets a pick naming a fluid id that is not registered
- **THEN** the keep stays as it was

### Requirement: Fluid sides hand fluid out
The station SHALL have only a Fluids side layer with the modes Push, Output and Off, shared by both halves as on the Fluid Inbound, the back on Push by default, the other sides Off, and the front fixed. With one tank, every Fluids side SHALL reach that tank and offer no tank choice. Push and Output sides SHALL let neighbours drain the tank and SHALL refuse fills; a Push side SHALL push into the neighbour at `push_interval`, fluid the neighbour refuses staying in the tank.

#### Scenario: Push into a machine
- **WHEN** the tank holds water, the back is Push and a Fluid Tank touches the back of the lower half
- **THEN** water flows into the Fluid Tank at the station's push interval

#### Scenario: Output waits for a pipe
- **WHEN** the left side is Output, the tank holds lava and a duct on pull touches it
- **THEN** the duct drains lava and nothing flows into the station through that side

### Requirement: Joins only through the front rail
The station SHALL be a member of the warehouse network only through a rail directly in front of its lower half, where tankers stop, as for the Fluid Inbound. Its tank SHALL NOT count as fluid stock.

#### Scenario: Not stock
- **WHEN** a Fluid Outbound of the warehouse holds 6,000 mB of water
- **THEN** the water is not part of the warehouse's fluid totals

### Requirement: Restock lamp and state line
The station SHALL carry a connection lamp lit only while a rail links it to an online warehouse, and an activity lamp: green while the tank holds its keep amount or keeps nothing, orange while restock runs are queued or under way, red while a kept fluid is missing from storage or no Tanker shuttle of the warehouse reaches the station, and dark while offline. The GUI state line SHALL name the kept fluid that cannot be restocked and why, or the offline reason.

#### Scenario: Missing fluid
- **WHEN** the station keeps 4 B of Mercury and no Fluid Rack holds Mercury
- **THEN** the lamp is red and the GUI says Mercury is missing from storage

#### Scenario: No tanker reaches
- **WHEN** the station keeps water, a Fluid Rack holds water and the warehouse has no Tanker shuttle that reaches the station
- **THEN** the lamp is red and the GUI says no Tanker shuttle reaches it

### Requirement: Station GUI
Right clicking either half with an empty hand or an item other than a fluid container, the Data Potato and the carrot SHALL open a GUI with the keep fluid picker, one gauge with a mark at the keep amount, the side panel with only the Fluids layer, the state line and the player inventory.

#### Scenario: Keep mark
- **WHEN** the station keeps 8,000 mB of water and holds 5,200 mB
- **THEN** its gauge shows water to 5,200 mB and a mark at 8,000 mB

### Requirement: Buckets by hand
Using an empty fluid container on either half SHALL fill it from the tank when it holds at least one container's worth. Using a filled container SHALL empty it into the tank when it keeps that fluid and has room. A player in creative mode SHALL keep the container and get the filled one too.

#### Scenario: Take water out
- **WHEN** the tank holds 5,200 mB of water and a player uses an empty bucket on the station
- **THEN** the player gets a water bucket and the tank holds 4,200 mB

### Requirement: Fluid Outbound inspect
Using the Data Potato on either half SHALL print one line for the tank: its fluid and amount against its capacity or that it is empty, and its keep fluid and amount or that it keeps nothing. It SHALL open no GUI and change nothing.

#### Scenario: Report
- **WHEN** the station keeps 8,000 mB of water and holds 5,200 mB
- **THEN** the chat shows one line: water 5,200 of 16,000 mB, keeping 8,000 mB of water

### Requirement: Fluid Outbound look
The station SHALL look like the Fluid Inbound with the Outbound Station block as its upper half: its front, sides, top and wood, its front lamps showing the station's state. The lower half SHALL be the same glass tank in the iron frame, filled to its share in steps, with the same swing hose arm on its front that docks onto a stopped tanker's filler cap as on the Fluid Inbound. The texture and plane rules of the Fluid Inbound SHALL hold.

#### Scenario: Arm restocks
- **WHEN** a Tanker shuttle stops in front of a Fluid Outbound to restock lava
- **THEN** the station's boom swings down, its head lowers onto the tanker's filler cap, the lava behind the glass rises, and the arm goes back up before the tanker drives on

#### Scenario: Tell them apart
- **WHEN** a player sees a Fluid Inbound and a Fluid Outbound side by side
- **THEN** the Inbound shows an Inbound Station on its tank and the Outbound an Outbound Station on its tank

### Requirement: Fluid Outbound is obtainable
The Fluid Outbound SHALL be crafted shaped from an Outbound Station on top of a Fluid Tank, one above the other, and SHALL keep the wood of that Outbound Station. The creative tab and the recipe viewer SHALL list it.

#### Scenario: Crafting
- **WHEN** a player crafts a birch Outbound Station on top of a Fluid Tank
- **THEN** the result is one birch Fluid Outbound

