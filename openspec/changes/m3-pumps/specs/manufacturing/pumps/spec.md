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
The Electric Pump SHALL spend `energy_per_tick` FE from its buffer while working, SHALL accept FE on every face except its front and bottom, and SHALL pump more water per second than the Burner Pump with the default data.

#### Scenario: Powered
- **WHEN** an Electric Pump on a pond has energy
- **THEN** it fills its buffer faster than a Burner Pump beside it

#### Scenario: No power
- **WHEN** its energy buffer is empty
- **THEN** progress pauses and resumes when energy arrives

### Requirement: Water leaves through the sides
A pump SHALL have no configurable sides: no side panel in its GUI, no side markers, and the carrot SHALL not change its sides. It SHALL never accept fluid. On every face except its front and its bottom it SHALL offer its water to neighbours that drain it and SHALL fill touching fluid handlers itself. The Burner Pump SHALL accept fuel from automation on the same faces.

#### Scenario: Pump beside a tank
- **WHEN** a pump stands next to a Fluid Tank
- **THEN** the tank fills with water without a pipe

#### Scenario: No side settings
- **WHEN** the player opens a pump's GUI or uses the carrot on one of its faces
- **THEN** no side panel shows and no face changes

#### Scenario: Front and bottom closed
- **WHEN** a tank touches the pump's front
- **THEN** no water reaches it

#### Scenario: Fuel by automation
- **WHEN** a duct pushes coal into the side of a Burner Pump
- **THEN** the coal goes into the fuel slot

#### Scenario: Refuses fluid
- **WHEN** a pipe tries to fill water into a pump
- **THEN** the pump accepts nothing

### Requirement: Pump connects to pipes
A pump SHALL show a short pipe stub from its body to the block edge on every face except its front and bottom where the neighbour is a duct reaching the pump with any line, or a block that takes or gives fluid, so ducts and tanks meet it without a gap. A stub toward a duct SHALL be as thick as that duct's arm toward the pump, which grows with the number of lines on it; a stub toward any other block SHALL have the two line size. The stubs SHALL follow neighbours being placed, removed or changed, and the outline SHALL include them.

#### Scenario: Pipe beside the pump
- **WHEN** a duct carrying the Fluid Pipe is placed east of a pump
- **THEN** the pump shows a stub on its east face that meets the duct's arm

#### Scenario: Pipe on top
- **WHEN** a Fluid Pipe duct is placed above a pump
- **THEN** the outlet on top reaches up to the duct

#### Scenario: Power Cable
- **WHEN** a duct carrying only a Power Cable touches the side of an Electric Pump
- **THEN** the pump shows a stub on that face that meets the cable's arm

#### Scenario: Item duct
- **WHEN** a duct carrying only an Item Pipe touches the side of a Burner Pump
- **THEN** the pump shows a stub on that face, since the duct can bring fuel

#### Scenario: Stub follows the duct size
- **WHEN** an Item Pipe duct beside a Burner Pump gets a Fluid Pipe added, then a Power Cable
- **THEN** the stub grows from thin to the two line size and stays there, since a Burner Pump takes no power and at most two lines meet a pump

#### Scenario: Tank beside the pump
- **WHEN** a Fluid Tank touches the pump's side
- **THEN** the pump shows a medium stub on that face

#### Scenario: Plain block
- **WHEN** stone touches the pump's side
- **THEN** no stub shows on that face

#### Scenario: Pipe removed
- **WHEN** the duct is broken
- **THEN** the stub on that face disappears

### Requirement: Pump GUI
A pump's GUI SHALL show, without a side panel, its buffer as a fluid gauge with the amount and capacity on hover, its progress, and the fuel slot and burn indicator (Burner Pump) or the energy bar (Electric Pump), plus a line telling whether the water source below is valid.

#### Scenario: No source shown
- **WHEN** a player opens the GUI of a pump that stands on dirt
- **THEN** the GUI says the pump needs an infinite water source below

### Requirement: Compact pump body
A pump SHALL be smaller than a full block: a base plate that covers most of the bottom face and rests on the block below, a narrower body that carries the front face, and a short outlet on top. Its outline and collision SHALL follow that shape, and the blocks around it SHALL stay visible.

#### Scenario: On a pond
- **WHEN** a pump stands on the water of a pond
- **THEN** its base plate sits on the water surface and the water and blocks around the narrower body stay visible

#### Scenario: Outline
- **WHEN** the player looks at a pump
- **THEN** the selection outline follows the base, the body and the outlet instead of a full cube

### Requirement: Crafting and research
The Burner Pump SHALL be craftable by hand from iron ingots, cobblestone, a bucket and a furnace. The Electric Pump SHALL be made in the assembler from a Burner Pump, iron gears and copper wire, and the recipe SHALL require the `electric_extractor` tech.

#### Scenario: Burner recipe
- **WHEN** the player crafts the Burner Pump pattern
- **THEN** one Burner Pump is produced

#### Scenario: Before research
- **WHEN** a player whose team has not researched `electric_extractor` adds the Electric Pump recipe to an assembler
- **THEN** the assembler refuses it with the research message
