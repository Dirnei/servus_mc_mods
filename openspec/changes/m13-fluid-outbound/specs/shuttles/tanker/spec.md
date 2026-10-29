## MODIFIED Requirements

### Requirement: Tanker module
The Tanker SHALL be a craftable tool module (example: a Fluid Tank, two buckets and iron ingots). It SHALL go into a shuttle through the install slot of the Shuttle Workshop, into the shuttle's one tool place, replacing a fitted tool. A shuttle with a Tanker SHALL take only tanker runs: no warehouse picks, no put-aways and no field runs. A shuttle without a Tanker SHALL NOT take tanker runs. The Tanker SHALL NOT be wide: a Tanker shuttle SHALL ride lift cars like a shuttle without a tool. Its tooltip SHALL say that it carries fluids between Fluid Inbounds, Fluid Racks and Fluid Outbounds. A Tanker shuttle SHALL carry up to its tier's fluid capacity of one fluid at a time (`tanker_capacity` per tier in `data/<ns>/shuttles/shuttle.json`, examples 4000, 8000 and 16000 mB for Mk1 to Mk3).

#### Scenario: Install
- **WHEN** a player puts a Tanker into the install slot of a workshop holding a Mk1 shuttle with a Harvester
- **THEN** the shuttle has the Tanker as its tool and the Harvester goes to the player

#### Scenario: No item jobs
- **WHEN** a warehouse whose only free shuttle has a Tanker gets a terminal request
- **THEN** the request waits and the Tanker shuttle does not take it

#### Scenario: Rides the lift
- **WHEN** a Fluid Inbound is on the ground floor and the only Fluid Rack with room is on the floor above, reached by an elevator shaft
- **THEN** the Tanker shuttle rides the lift car to bring the fluid up

### Requirement: Fluid put-away runs
Every `fluid_check_interval` ticks (in `data/<ns>/shuttles/warehouse.json`, example 40) an online warehouse SHALL look at the tank of each of its Fluid Inbounds. For a tank, the free fluid SHALL be its amount minus what tanker runs already queued or under way will take from it. The warehouse SHALL queue one tanker run for that tank when no run for it is queued or under way, the free fluid is at least `tanker_min_load` (in `warehouse.json`, example 1000 mB) or the tank is full, the warehouse has a Tanker shuttle that reaches the station, and a Fluid Rack section has room for the fluid. The run SHALL take the least of the free fluid, the fluid capacity of the shuttle that takes it, and the room of the chosen section.

#### Scenario: Pipe fills, tanker collects
- **WHEN** a pump fills a Fluid Inbound tank with 2000 mB of water and the warehouse has an empty, unzoned Fluid Rack section
- **THEN** a Tanker shuttle drives to the Fluid Inbound, takes the water and brings it into the section

#### Scenario: Small amounts wait
- **WHEN** a Fluid Inbound tank holds 300 mB of water and is not full
- **THEN** no tanker run is queued for it

#### Scenario: Into a Fluid Rack
- **WHEN** a Fluid Inbound tank holds 4000 mB of water and the warehouse's only fluid store is an empty, unzoned Fluid Rack section
- **THEN** a Tanker shuttle brings the water to the rack's port and the section holds 4000 mB of water

#### Scenario: From a Fluid Inbound
- **WHEN** a Fluid Inbound tank holds 2000 mB of lava and a Fluid Rack section is empty and unzoned
- **THEN** a Tanker shuttle takes the lava in front of the station and brings it into the rack

### Requirement: Target tank by lock
The target of a fluid put-away SHALL be chosen among the Fluid Rack sections of the warehouse that a Tanker shuttle reaches, in this order: zoned sections whose filter lists the fluid, with room; then general sections (unzoned, or zoned without fluids in the filter) holding the fluid, with room; then empty general sections. Room SHALL be the section's capacity minus its amount minus what runs already on their way will put into it. Within one rank the section with the shortest way from the source SHALL be chosen.

#### Scenario: Locked tank first
- **WHEN** water arrives and the warehouse has an empty unzoned section and a half full section zoned with a water bucket
- **THEN** the water goes to the zoned section

#### Scenario: Room counts runs on the way
- **WHEN** a section zoned to water has 4000 mB of room and a run bringing 4000 mB is on its way to it
- **THEN** the next water goes to another section or waits

#### Scenario: Zoned rack first
- **WHEN** water arrives and the warehouse has an empty unzoned Fluid Rack section and an empty section zoned with a water bucket
- **THEN** the water goes to the zoned section

#### Scenario: Zoned rack refuses other fluids
- **WHEN** lava arrives and the only Fluid Rack section with room is zoned with a water bucket
- **THEN** the lava is not put there

### Requirement: No room, no run
When a Fluid Inbound tank holds fluid and no Fluid Rack section of the warehouse has room for that fluid, the warehouse SHALL queue no run for it, the fluid SHALL stay in the station, and the pipes feeding it SHALL back up once it is full. The station's activity lamp SHALL be red and its GUI state line SHALL name the fluid that has no room. When room appears, runs SHALL start again by themselves. A Fluid Inbound holding fluid that no Tanker shuttle of the warehouse reaches SHALL show the red lamp as well, and its state line SHALL say that no Tanker shuttle reaches it.

#### Scenario: Backed up import
- **WHEN** every Fluid Rack section is zoned to water or full and a Fluid Inbound holds lava
- **THEN** no tanker drives, the station's lamp is red and its GUI says lava has no room

#### Scenario: Backed up Fluid Inbound
- **WHEN** a Fluid Inbound holds lava and no fluid store takes lava
- **THEN** no tanker drives, the station's lamp is red and a duct feeding more lava backs up once the tanks are full

### Requirement: Tanker run
On a tanker run the shuttle SHALL drive to the source's stop (the rail in front of a Fluid Rack section's port or of a Fluid Inbound's lower half), stop there for `tanker_pump_ticks` (in `warehouse.json`, example 40) shortened by Handling like other stops, and take the run's fluid out of the source, as much as it still holds up to the run's amount. It SHALL then drive to the target's stop, stop the same way and put the fluid into the target. Fluid that does not fit there SHALL go to the next Fluid Rack section with room by the target order; when none has room, the rest SHALL go back into the source as far as it has room and what still does not fit SHALL be lost. Then the shuttle SHALL go home. The warehouse's fluid stock SHALL follow every transfer.

#### Scenario: Partial pick
- **WHEN** a run for 4000 mB arrives and the source tank holds only 2500 mB because a player emptied part of it by bucket meanwhile
- **THEN** the tanker takes 2500 mB and brings them to the target

#### Scenario: Target filled by hand meanwhile
- **WHEN** the target section was filled by bucket while the tanker was on its way and only half of the cargo fits
- **THEN** the other half goes to the next Fluid Rack section with room

### Requirement: Tanker runs that cannot go on
When the source Fluid Inbound is removed or loses its front rail before the tanker took its fluid, the tanker SHALL go home empty. When the target Fluid Rack section is broken, loses the rail in front of its port or is zoned against the fluid during a run, the tanker SHALL bring its cargo to another section by the target order and otherwise back to the source as in a tanker run. When the target Fluid Outbound of a restock run is removed or loses its front rail, the tanker SHALL bring its cargo back to a Fluid Rack section by the target order. When the rail it stands on or drives over is removed, its garage loses the way home, its garage is removed or the player recalls the garage's shuttles, it SHALL be back in its garage and its fluid cargo SHALL be lost. A run that ends in one of these ways SHALL NOT be queued again by itself; the next check MAY queue a new one.

#### Scenario: Recall with cargo
- **WHEN** a Tanker shuttle carrying 3000 mB of water is recalled
- **THEN** it is back in its garage and carries nothing

#### Scenario: Target rack broken
- **WHEN** a tanker is on its way with water to a Fluid Rack section and a player breaks that section's port bay
- **THEN** the tanker brings the water to another fluid store with room, or back to the source

#### Scenario: Source station broken
- **WHEN** a tanker is on its way to a Fluid Inbound and a player breaks the station
- **THEN** the tanker goes home empty

#### Scenario: Outbound broken during a restock
- **WHEN** a tanker brings water to a Fluid Outbound and a player breaks the station
- **THEN** the tanker brings the water back to a Fluid Rack section with room

### Requirement: Tanker look
A Tanker shuttle SHALL show a steel tank lying along its body with two bands, a copper filler cap on top and a telescopic coupling stowed in each side of the tank, staying within the shuttle body's width and below the top of the rail's block, so it fits on a lift car. A Tanker shuttle carrying fluid SHALL show a sight glass on each side filled to its share. Every tool look rule of the shuttles (steel and iron textures on every face, no shared planes where boxes overlap) SHALL hold, also for the coupling while it slides.

#### Scenario: Tanker on a lift car
- **WHEN** a Tanker shuttle stands on a lift car
- **THEN** no part of it reaches outside the car's posts or above its block

#### Scenario: Coupling stowed while driving
- **WHEN** a Tanker shuttle drives along a rail
- **THEN** its couplings sit flush in the tank's sides

### Requirement: Export restock runs
Every `fluid_check_interval` ticks an online warehouse SHALL look at the tank of each of its Fluid Outbounds that keeps a fluid with an amount above 0. Its have SHALL be its amount plus what tanker runs queued or under way will bring to it. When the have is below the keep amount, no run for that tank is queued or under way, the warehouse has a Tanker shuttle that reaches the station, and Fluid Rack sections hold the fluid, the warehouse SHALL queue one tanker run that takes the least of the missing amount, the shuttle's fluid capacity and what the chosen source section holds beyond what runs already on their way will take from it. Runs below `tanker_min_load` SHALL wait unless they fill the missing amount completely.

#### Scenario: Pipe drains, tanker restocks
- **WHEN** a Fluid Outbound tank keeps 8 B of water, holds 2 B and a Fluid Rack section holds 20 B of water
- **THEN** a Tanker shuttle brings water from the rack until the Outbound tank holds 8 B

#### Scenario: Runs on the way count
- **WHEN** a run bringing 4 B of water to a Fluid Outbound tank is under way and the tank holds 2 B of a keep of 8 B
- **THEN** the next run brings at most 2 B

### Requirement: Source tank for restock
The source of a restock run SHALL be the Fluid Rack section holding the fluid that the Tanker reaches, preferring the one with the most free fluid and then the shortest way. Fluid Inbounds SHALL NOT be sources of restock runs. When no section holds the fluid, no run SHALL be queued, the Fluid Outbound's activity lamp SHALL be red and its GUI state line SHALL name the fluid that is missing from storage; when stock appears, runs SHALL start again by themselves. A Fluid Outbound that no Tanker shuttle reaches SHALL show the red lamp, and its state line SHALL say so.

#### Scenario: Nothing in stock
- **WHEN** a Fluid Outbound keeps 4 B of Mercury and no Fluid Rack holds Mercury
- **THEN** no tanker drives, the station's lamp is red and its GUI says Mercury is missing

#### Scenario: Restock from a rack
- **WHEN** a Fluid Outbound tank keeps 8 B of water, holds 0 B and the only water is 20 B in a Fluid Rack section
- **THEN** a Tanker shuttle takes water at the rack's port and brings it to the station until the tank holds 8 B

#### Scenario: Inbound is no source
- **WHEN** a Fluid Outbound keeps water and the only water is in a Fluid Inbound
- **THEN** no restock run is queued and the station's lamp is red

### Requirement: Restock run
A restock run SHALL drive to the rail in front of the source section's port, take the run's fluid (as much as the source still holds up to the run's amount), drive to the rail in front of the Fluid Outbound's lower half and put it into the kept tank as far as it has room. Fluid that does not fit SHALL go back to a Fluid Rack section by the target order of fluid put-aways, and what still does not fit SHALL be lost. Then the shuttle SHALL go home. Removal, rail loss, garage loss and recall SHALL end a restock run like a fluid put-away run.

#### Scenario: Kept tank filled meanwhile
- **WHEN** a restock run arrives with 4 B but a player meanwhile poured 3 B into the Fluid Outbound tank by bucket
- **THEN** the tanker puts in what fits and brings the rest back to a Fluid Rack

### Requirement: Coupling at a port
A Tanker shuttle that stops to take or put fluid at a Fluid Rack SHALL stand on the rail in front of its port and SHALL slide a telescopic coupling out of its tank's side toward the block until it meets the port, keep it there while it pumps and pull it back in before it drives on. At a Fluid Inbound or a Fluid Outbound, which have no port, it SHALL stand on the rail in front of the station's lower half and pump the same way; its coupling MAY slide out toward the station's tank there too. The block itself SHALL not move. Its glass SHALL follow the transfer. Clients SHALL learn the stop's start and end, not every tick of it.

#### Scenario: Docking at a rack
- **WHEN** a tanker stops at the port of a Fluid Rack section to put water into it
- **THEN** its coupling slides out into the port, the rack's level rises, and the coupling slides back in when the tanker leaves

#### Scenario: Rack on either side
- **WHEN** a rail has Fluid Rack sections on both sides and a tanker stops at the port of the section on its right
- **THEN** the coupling slides out to the right

#### Scenario: Docking at a Fluid Inbound
- **WHEN** a tanker stops at a Fluid Inbound to take water
- **THEN** it stands in front of the station's lower half, the fluid behind the station's glass falls and the tanker's sight glass rises

#### Scenario: Docking at a Fluid Outbound
- **WHEN** a tanker stops at a Fluid Outbound to restock lava
- **THEN** it stands in front of the station's lower half and the lava behind the station's glass rises

## REMOVED Requirements

### Requirement: Spout and stream
**Reason**: The Fluid Gantry and its spout are removed; the tanker's coupling docks into the ports of Fluid Racks instead.
**Migration**: See "Coupling at a port".
