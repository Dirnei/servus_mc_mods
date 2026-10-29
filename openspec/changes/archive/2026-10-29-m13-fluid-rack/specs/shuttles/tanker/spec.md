## ADDED Requirements

### Requirement: Coupling at a port
A Tanker shuttle that stops to take or put fluid at a Fluid Rack SHALL stand on the rail in front of the section's port and SHALL slide a telescopic coupling out of its tank's side toward the rack until it meets the port, keep it there while it pumps and pull it back in before it drives on. The rack itself SHALL not move. The glass front SHALL follow the transfer. Clients SHALL learn the stop's start and end, not every tick of it.

#### Scenario: Docking at a rack
- **WHEN** a tanker stops at the port of a Fluid Rack section to put water into it
- **THEN** its coupling slides out into the port, the rack's level rises, and the coupling slides back in when the tanker leaves

#### Scenario: Rack on either side
- **WHEN** a rail has Fluid Rack sections on both sides and a tanker stops at the port of the section on its right
- **THEN** the coupling slides out to the right

## MODIFIED Requirements

### Requirement: Fluid put-away runs
Every `fluid_check_interval` ticks (in `data/<ns>/shuttles/warehouse.json`, example 40) an online warehouse SHALL look at each tank of its Import gantries. For a tank, the free fluid SHALL be its amount minus what tanker runs already queued or under way will take from it. The warehouse SHALL queue one tanker run for that tank when no run for it is queued or under way, the free fluid is at least `tanker_min_load` (in `warehouse.json`, example 1000 mB) or the tank is full, the warehouse has a Tanker shuttle that reaches the gantry, and a fluid store (a Storage gantry tank or a Fluid Rack section) has room for the fluid. The run SHALL take the least of the free fluid, the fluid capacity of the shuttle that takes it, and the room of the chosen target.

#### Scenario: Pipe fills, tanker collects
- **WHEN** a pump fills an Import gantry tank with 2000 mB of water and a Storage gantry has an empty tank on Any
- **THEN** a Tanker shuttle drives to the Import gantry, takes the water and brings it into the Storage tank

#### Scenario: Small amounts wait
- **WHEN** an Import gantry tank holds 300 mB of water and is not full
- **THEN** no tanker run is queued for it

#### Scenario: Into a Fluid Rack
- **WHEN** an Import gantry tank holds 4000 mB of water and the warehouse's only fluid store is an empty, unzoned Fluid Rack section
- **THEN** a Tanker shuttle brings the water to the rack's port and the section holds 4000 mB of water

### Requirement: Target tank by lock
The target of a fluid put-away SHALL be chosen among the fluid stores of the warehouse that a Tanker shuttle reaches, in this order: Storage gantry tanks locked to the fluid and zoned Fluid Rack sections whose filter lists the fluid, with room; then Storage gantry tanks on Any and general Fluid Rack sections (unzoned, or zoned without fluids in the filter) holding the fluid, with room; then empty Storage gantry tanks on Any and empty general sections. Room SHALL be the store's capacity minus its amount minus what runs already on their way will put into it. Within one rank the store with the shortest way from the source SHALL be chosen.

#### Scenario: Locked tank first
- **WHEN** water arrives and the warehouse has an empty tank on Any and a half full tank locked to water
- **THEN** the water goes to the tank locked to water

#### Scenario: Room counts runs on the way
- **WHEN** a Storage tank locked to water has 4000 mB of room and a run bringing 4000 mB is on its way to it
- **THEN** the next water goes to another tank or waits

#### Scenario: Zoned rack first
- **WHEN** water arrives and the warehouse has an empty unzoned Fluid Rack section and an empty section zoned with a water bucket
- **THEN** the water goes to the zoned section

#### Scenario: Zoned rack refuses other fluids
- **WHEN** lava arrives and the only Fluid Rack section with room is zoned with a water bucket
- **THEN** the lava is not put there

### Requirement: No room, no run
When an Import gantry tank holds fluid and no fluid store of the warehouse has room for that fluid, the warehouse SHALL queue no run for it, the fluid SHALL stay in the Import gantry, and the pipes feeding it SHALL back up once it is full. The Import gantry's activity lamp SHALL be red and its GUI state line SHALL name the fluid that has no room. When room appears, runs SHALL start again by themselves. An Import gantry holding fluid that no Tanker shuttle of the warehouse reaches SHALL show the red lamp as well, and its state line SHALL say that no Tanker shuttle reaches it.

#### Scenario: Backed up import
- **WHEN** every Storage tank is locked to water or full, every Fluid Rack section is zoned to water or full, and an Import gantry holds lava
- **THEN** no tanker drives, the Import gantry's lamp is red and its GUI says lava has no room

### Requirement: Tanker run
On a tanker run the shuttle SHALL drive to the source's stop (the rail under a gantry, or the rail in front of a Fluid Rack section's port), stop there for `tanker_pump_ticks` (in `warehouse.json`, example 40) shortened by Handling like other stops, and take the run's fluid out of the source, as much as it still holds up to the run's amount. It SHALL then drive to the target's stop, stop the same way and put the fluid into the target. Fluid that does not fit there SHALL go to the next fluid store with room by the target order; when none has room, the rest SHALL go back into the source as far as it has room and what still does not fit SHALL be lost. Then the shuttle SHALL go home. The warehouse's fluid stock SHALL follow every transfer.

#### Scenario: Partial pick
- **WHEN** a run for 4000 mB arrives and the source tank holds only 2500 mB because an Export pipe drained it meanwhile
- **THEN** the tanker takes 2500 mB and brings them to the target

#### Scenario: Target filled by hand meanwhile
- **WHEN** the target tank was filled by bucket while the tanker was on its way and only half of the cargo fits
- **THEN** the other half goes to the next fluid store with room

### Requirement: Tanker runs that cannot go on
When the source gantry is removed or leaves Import mode before the tanker took its fluid, the tanker SHALL go home empty. When the target gantry is removed or leaves Storage mode, or the target Fluid Rack section is broken, loses the rail in front of its port or is zoned against the fluid during a run, the tanker SHALL bring its cargo to another fluid store by the target order and otherwise back to the source as in a tanker run. When the rail it stands on or drives over is removed, its garage loses the way home, its garage is removed or the player recalls the garage's shuttles, it SHALL be back in its garage and its fluid cargo SHALL be lost. A run that ends in one of these ways SHALL NOT be queued again by itself; the next check MAY queue a new one.

#### Scenario: Recall with cargo
- **WHEN** a Tanker shuttle carrying 3000 mB of water is recalled
- **THEN** it is back in its garage and carries nothing

#### Scenario: Target rack broken
- **WHEN** a tanker is on its way with water to a Fluid Rack section and a player breaks that section's port bay
- **THEN** the tanker brings the water to another fluid store with room, or back to the source

### Requirement: Tanker look
A Tanker shuttle SHALL show a steel tank lying along its body with two bands, a copper filler cap on top and a telescopic coupling stowed in each side of the tank, staying within the shuttle body's width and below the top of the rail's block, so it fits under the gantry spout and on a lift car. A Tanker shuttle carrying fluid SHALL show a sight glass on each side filled to its share. Every tool look rule of the shuttles (steel and iron textures on every face, no shared planes where boxes overlap) SHALL hold, also for the coupling while it slides.

#### Scenario: Tanker on a lift car
- **WHEN** a Tanker shuttle stands on a lift car
- **THEN** no part of it reaches outside the car's posts or above its block

#### Scenario: Coupling stowed while driving
- **WHEN** a Tanker shuttle drives along a rail
- **THEN** its couplings sit flush in the tank's sides

### Requirement: Source tank for restock
The source of a restock run SHALL be the fluid store holding the fluid that the Tanker reaches, a Storage gantry tank or a Fluid Rack section, preferring the one with the most free fluid and then the shortest way. Import gantries SHALL NOT be sources of restock runs. When no fluid store holds the fluid, no run SHALL be queued, the Export gantry's activity lamp SHALL be red and its GUI state line SHALL name the fluid that is missing from storage; when stock appears, runs SHALL start again by themselves.

#### Scenario: Nothing in stock
- **WHEN** an Export gantry keeps 4 B of Mercury and no Storage tank or Fluid Rack holds Mercury
- **THEN** no tanker drives, the Export gantry's lamp is red and its GUI says Mercury is missing

#### Scenario: Restock from a rack
- **WHEN** an Export gantry tank locked to water keeps 8 B, holds 0 B and the only water is 20 B in a Fluid Rack section
- **THEN** a Tanker shuttle takes water at the rack's port and brings it to the Export gantry until the tank holds 8 B
