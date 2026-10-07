## Purpose

Pumps are Manufacturing's water source: a Burner Pump and an Electric Pump stand on an infinite water source and produce water forever without using it up.

## ADDED Requirements

### Requirement: Needs an infinite water source below
A pump SHALL only work while the block directly below it is a water source block with at least `min_source_neighbours` (example 2) horizontally adjacent water source blocks. The pump SHALL never remove or change that water. The source SHALL be checked at the start of each work cycle, not every tick.

#### Scenario: On a pond
- **WHEN** a pump stands on a water source in a 2 by 2 pool of water sources
- **THEN** it can work

#### Scenario: Single bucket
- **WHEN** a pump stands on a lone water source block placed from one bucket
- **THEN** it does nothing and burns no fuel and spends no energy

#### Scenario: Source drained
- **WHEN** a player removes the neighbouring water sources while the pump runs
- **THEN** the pump stops at the start of its next work cycle

#### Scenario: Water stays
- **WHEN** a pump has run for an hour on a pond
- **THEN** the pond's blocks are unchanged

### Requirement: Pumps water into a buffer
A working pump SHALL add `fluid_per_cycle` mB of water to its internal buffer of `fluid_capacity` mB once per `work_ticks`, and SHALL pause without spending fuel or energy while the buffer cannot take a full cycle.

#### Scenario: Buffer fills
- **WHEN** a pump with nothing attached runs on a pond
- **THEN** its buffer fills up to `fluid_capacity` and the pump then pauses

#### Scenario: Room again
- **WHEN** a pipe drains the full buffer
- **THEN** the pump resumes

### Requirement: Burner Pump burns fuel
The Burner Pump SHALL burn any item vanilla furnaces accept as fuel, consuming one item when the previous burn ends and work is available, like the Burner Extractor.

#### Scenario: Coal
- **WHEN** a Burner Pump on a pond has one coal and an empty buffer
- **THEN** it pumps for the coal's furnace burn time unless its buffer fills first

#### Scenario: No fuel
- **WHEN** the fuel slot is empty and the burn has ended
- **THEN** progress stops and resumes when fuel is added

### Requirement: Electric Pump runs on energy
The Electric Pump SHALL spend `energy_per_tick` FE from its buffer while working, SHALL accept FE on every side like the other powered machines, and SHALL pump more water per second than the Burner Pump with the default data.

#### Scenario: Powered
- **WHEN** an Electric Pump on a pond has energy
- **THEN** it fills its buffer faster than a Burner Pump beside it

#### Scenario: No power
- **WHEN** its energy buffer is empty
- **THEN** progress pauses and resumes when energy arrives

### Requirement: Water leaves through the sides
A pump SHALL never accept fluid. Its sides use the usual machine side modes, set with the Configuration Carrot and shown by side markers: water SHALL be offered as a drain on sides whose mode lets neighbours extract (Auto, Output, Both, Push), and the pump SHALL fill touching fluid handlers itself on sides whose mode pushes (Auto, Push). The front and the bottom SHALL be fixed Off.

#### Scenario: Pump beside a tank
- **WHEN** a pump stands next to a Fluid Tank with that side left on Auto
- **THEN** the tank fills with water without a pipe

#### Scenario: Output side
- **WHEN** that side is set to Output
- **THEN** the pump no longer fills the tank itself, and a duct on that side can still drain it

#### Scenario: Input side
- **WHEN** that side is set to Input
- **THEN** no water leaves through it, and fuel can still be inserted there (Burner Pump)

#### Scenario: Refuses fluid
- **WHEN** a pipe tries to fill water into a pump
- **THEN** the pump accepts nothing

### Requirement: Pump GUI
A pump's GUI SHALL show its buffer as a fluid gauge with the amount and capacity on hover, its progress, and the fuel slot and burn indicator (Burner Pump) or the energy bar (Electric Pump), plus a line telling whether the water source below is valid.

#### Scenario: No source shown
- **WHEN** a player opens the GUI of a pump that stands on dirt
- **THEN** the GUI says the pump needs an infinite water source below

### Requirement: Crafting and research
The Burner Pump SHALL be craftable by hand from iron ingots, cobblestone, a bucket and a furnace. The Electric Pump SHALL be made in the assembler from a Burner Pump, iron gears and copper wire, and the recipe SHALL require the `electric_extractor` tech.

#### Scenario: Burner recipe
- **WHEN** the player crafts the Burner Pump pattern
- **THEN** one Burner Pump is produced

#### Scenario: Before research
- **WHEN** a player whose team has not researched `electric_extractor` adds the Electric Pump recipe to an assembler
- **THEN** the assembler refuses it with the research message
