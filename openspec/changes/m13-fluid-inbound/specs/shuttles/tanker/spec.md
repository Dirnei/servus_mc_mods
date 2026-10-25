## MODIFIED Requirements

### Requirement: Coupling at a port
A Tanker shuttle that stops to take or put fluid at a Fluid Rack or a Fluid Inbound SHALL stand on the rail in front of its port and SHALL slide a telescopic coupling out of its tank's side toward the block until it meets the port, keep it there while it pumps and pull it back in before it drives on. The block itself SHALL not move. Its glass SHALL follow the transfer. Clients SHALL learn the stop's start and end, not every tick of it.

#### Scenario: Docking at a rack
- **WHEN** a tanker stops at the port of a Fluid Rack section to put water into it
- **THEN** its coupling slides out into the port, the rack's level rises, and the coupling slides back in when the tanker leaves

#### Scenario: Rack on either side
- **WHEN** a rail has Fluid Rack sections on both sides and a tanker stops at the port of the section on its right
- **THEN** the coupling slides out to the right

#### Scenario: Docking at a Fluid Inbound
- **WHEN** a tanker stops at a Fluid Inbound to take water
- **THEN** its coupling slides into the station's port, the station's sight glass falls and the tanker's sight glass rises

### Requirement: Fluid put-away runs
Every `fluid_check_interval` ticks (in `data/<ns>/shuttles/warehouse.json`, example 40) an online warehouse SHALL look at each tank of its Import gantries and its Fluid Inbounds. For a tank, the free fluid SHALL be its amount minus what tanker runs already queued or under way will take from it. The warehouse SHALL queue one tanker run for that tank when no run for it is queued or under way, the free fluid is at least `tanker_min_load` (in `warehouse.json`, example 1000 mB) or the tank is full, the warehouse has a Tanker shuttle that reaches the source, and a fluid store (a Storage gantry tank or a Fluid Rack section) has room for the fluid. The run SHALL take the least of the free fluid, the fluid capacity of the shuttle that takes it, and the room of the chosen target.

#### Scenario: Pipe fills, tanker collects
- **WHEN** a pump fills an Import gantry tank with 2000 mB of water and a Storage gantry has an empty tank on Any
- **THEN** a Tanker shuttle drives to the Import gantry, takes the water and brings it into the Storage tank

#### Scenario: Small amounts wait
- **WHEN** an Import gantry tank holds 300 mB of water and is not full
- **THEN** no tanker run is queued for it

#### Scenario: Into a Fluid Rack
- **WHEN** an Import gantry tank holds 4000 mB of water and the warehouse's only fluid store is an empty, unzoned Fluid Rack section
- **THEN** a Tanker shuttle brings the water to the rack's port and the section holds 4000 mB of water

#### Scenario: From a Fluid Inbound
- **WHEN** a Fluid Inbound tank holds 2000 mB of lava and a Fluid Rack section is empty and unzoned
- **THEN** a Tanker shuttle takes the lava at the station's port and brings it into the rack

### Requirement: No room, no run
When an Import gantry tank or a Fluid Inbound tank holds fluid and no fluid store of the warehouse has room for that fluid, the warehouse SHALL queue no run for it, the fluid SHALL stay in the source, and the pipes feeding it SHALL back up once it is full. The source's activity lamp SHALL be red and its GUI state line SHALL name the fluid that has no room. When room appears, runs SHALL start again by themselves. A source holding fluid that no Tanker shuttle of the warehouse reaches SHALL show the red lamp as well, and its state line SHALL say that no Tanker shuttle reaches it.

#### Scenario: Backed up import
- **WHEN** every Storage tank is locked to water or full, every Fluid Rack section is zoned to water or full, and an Import gantry holds lava
- **THEN** no tanker drives, the Import gantry's lamp is red and its GUI says lava has no room

#### Scenario: Backed up Fluid Inbound
- **WHEN** a Fluid Inbound holds lava and no fluid store takes lava
- **THEN** no tanker drives, the station's lamp is red and a duct feeding more lava backs up once the tanks are full

### Requirement: Tanker run
On a tanker run the shuttle SHALL drive to the source's stop (the rail under a gantry, or the rail in front of the port of a Fluid Rack section or a Fluid Inbound), stop there for `tanker_pump_ticks` (in `warehouse.json`, example 40) shortened by Handling like other stops, and take the run's fluid out of the source, as much as it still holds up to the run's amount. It SHALL then drive to the target's stop, stop the same way and put the fluid into the target. Fluid that does not fit there SHALL go to the next fluid store with room by the target order; when none has room, the rest SHALL go back into the source as far as it has room and what still does not fit SHALL be lost. Then the shuttle SHALL go home. The warehouse's fluid stock SHALL follow every transfer.

#### Scenario: Partial pick
- **WHEN** a run for 4000 mB arrives and the source tank holds only 2500 mB because an Export pipe drained it meanwhile
- **THEN** the tanker takes 2500 mB and brings them to the target

#### Scenario: Target filled by hand meanwhile
- **WHEN** the target tank was filled by bucket while the tanker was on its way and only half of the cargo fits
- **THEN** the other half goes to the next fluid store with room

### Requirement: Tanker runs that cannot go on
When the source gantry is removed or leaves Import mode, or the source Fluid Inbound is removed or loses its front rail, before the tanker took its fluid, the tanker SHALL go home empty. When the target gantry is removed or leaves Storage mode, or the target Fluid Rack section is broken, loses the rail in front of its port or is zoned against the fluid during a run, the tanker SHALL bring its cargo to another fluid store by the target order and otherwise back to the source as in a tanker run. When the rail it stands on or drives over is removed, its garage loses the way home, its garage is removed or the player recalls the garage's shuttles, it SHALL be back in its garage and its fluid cargo SHALL be lost. A run that ends in one of these ways SHALL NOT be queued again by itself; the next check MAY queue a new one.

#### Scenario: Recall with cargo
- **WHEN** a Tanker shuttle carrying 3000 mB of water is recalled
- **THEN** it is back in its garage and carries nothing

#### Scenario: Target rack broken
- **WHEN** a tanker is on its way with water to a Fluid Rack section and a player breaks that section's port bay
- **THEN** the tanker brings the water to another fluid store with room, or back to the source

#### Scenario: Source station broken
- **WHEN** a tanker is on its way to a Fluid Inbound and a player breaks the station
- **THEN** the tanker goes home empty
