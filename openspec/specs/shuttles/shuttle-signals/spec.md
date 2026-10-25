# shuttles/shuttle-signals Specification

## Purpose
Shuttle signals show on the rails which way a shuttle faces, which shuttle it is and why it drives or stands, so players can read a traffic jam in the world and ask the Data Potato for the details.

## Requirements

### Requirement: Front and back lights
A shuttle drawn on the rails or on a lift car SHALL show two white headlights on its front and two tail lights on its back, on every tier, drive step and tool. Lit lights SHALL glow at full brightness also in the dark. The lights SHALL sit on the body without z-fighting and SHALL stay inside the rail's block.

#### Scenario: Telling front from back
- **WHEN** a player looks at a plain Mk1 shuttle standing on a rail at night
- **THEN** one end shows two glowing white lights and the other end shows the tail lights

#### Scenario: Lights with a tool
- **WHEN** a Harvester shuttle drives along a Farm Rail
- **THEN** its headlights still show beside the reel on the front and its tail lights on the back

### Requirement: Tail lights show what the shuttle does
The tail lights SHALL show:
- dim red while the shuttle drives freely,
- bright red while it brakes or is held behind another shuttle,
- blinking amber while it waits for something that is not a shuttle: the lift car, a deck, power or the chunk ahead,
- off while it stands for a stop, works at a plot, rides the lift car, hands over to the car or waits at its garage door.
Headlights SHALL stay lit in every state.

#### Scenario: Driving
- **WHEN** a shuttle drives along an empty aisle at full speed
- **THEN** its tail lights are dim red

#### Scenario: Braking for its stop
- **WHEN** a shuttle slows down before the bay it stops at
- **THEN** its tail lights turn bright red while it slows down and go off once it stands for its stop

#### Scenario: Held behind another shuttle
- **WHEN** a shuttle stands one block behind a shuttle that loads at a bay
- **THEN** its tail lights are bright red until it drives on

#### Scenario: Waiting for the lift car
- **WHEN** a shuttle stands beside a shaft and the car is on another floor
- **THEN** its tail lights blink amber until it drives onto the car

#### Scenario: Waiting for power
- **WHEN** a shuttle stands because its controller has no energy left for the next block
- **THEN** its tail lights blink amber

#### Scenario: Riding the car
- **WHEN** the car carries a shuttle up the shaft
- **THEN** the shuttle's tail lights are off and its headlights are lit

### Requirement: Every player sees the same signal
Clients SHALL learn why each drawn shuttle stands from the server, and the server SHALL sync a shuttle when that reason changes. A shuttle that starts or stops braking on its own path SHALL change its tail lights on every client without waiting for a sync.

#### Scenario: Two players
- **WHEN** two players watch a shuttle that waits beside a shaft for the car
- **THEN** both see its tail lights blink amber, and both see them change when it drives onto the car

### Requirement: Number plate on the sides
A numbered shuttle drawn on the rails or on a lift car SHALL show a dark steel plate on each long side with its number as `#` and digits in light text, readable from that side. The plate SHALL be only as wide as the number needs, and a longer number SHALL be drawn smaller so it fits the body. A shuttle without a number SHALL show no plate. The plate SHALL NOT show the engraved name.

#### Scenario: Plate on both sides
- **WHEN** Shuttle #12 drives east along an aisle
- **THEN** a player north of the aisle and a player south of it both read "#12" on the side facing them

#### Scenario: Three digits
- **WHEN** Shuttle #107 stands on a rail
- **THEN** its plate shows "#107" inside the body's length

#### Scenario: Engraved shuttle
- **WHEN** Mk2 Shuttle #12 with the plate "Berta" drives by
- **THEN** its side plate reads "#12"

#### Scenario: Unnumbered shuttle
- **WHEN** a shuttle without a number is drawn on the rails
- **THEN** it shows no side plate

### Requirement: Plates show only up close
The number plate text SHALL be drawn only for players within a short distance of the shuttle (example 16 blocks), like sign text. The plate itself MAY show farther away.

#### Scenario: Far away
- **WHEN** a player stands 40 blocks from an aisle full of shuttles
- **THEN** no plate numbers are drawn for those shuttles

### Requirement: Data Potato reports the shuttles on a rail
Using the Data Potato on a Warehouse Rail, Farm Rail or elevator segment of an online warehouse SHALL print a chat report of every shuttle on that block or driving off it: its label and what it does now. It SHALL report "No shuttle here" when there is none. The report SHALL change nothing.

#### Scenario: Driving shuttle
- **WHEN** a player uses the Data Potato on the rail Shuttle #4 drives over
- **THEN** chat shows "Shuttle #4" and that it is driving

#### Scenario: Empty rail
- **WHEN** a player uses the Data Potato on a rail no shuttle is on
- **THEN** chat shows "No shuttle here"

#### Scenario: Shuttle on the car
- **WHEN** a player uses the Data Potato on the segment where the car carries Shuttle #2
- **THEN** chat shows "Shuttle #2" riding the lift car to its floor

### Requirement: The report names the reason a shuttle stands
The report SHALL name why a standing shuttle stands: its stop and where, field work, its garage door, riding or handing over to the lift car, the shuttle it follows or waits behind by label, the lift car with the floor it waits on (counted from the shaft's lowest segment as floor 1), a deck, power, or the chunk ahead not being loaded.

#### Scenario: Waiting for the car
- **WHEN** Shuttle #5 stands on floor 2 of a shaft and the car is on floor 1 with another shuttle on it
- **THEN** the report reads "Shuttle #5" waiting for the lift car on floor 2

#### Scenario: Behind another shuttle
- **WHEN** Shuttle #3 stands behind Shuttle #7, which loads at a bay
- **THEN** the report on Shuttle #3's rail says it waits behind Shuttle #7

### Requirement: The report follows the chain of waiting shuttles
When a shuttle waits behind another shuttle, the report SHALL follow the chain of shuttles each one waits behind, up to a limit (example 8), and SHALL end with what the last one waits for. When the chain comes back to a shuttle already named, the report SHALL say the shuttles wait for each other in a cycle.

#### Scenario: Chain to the car
- **WHEN** Shuttle #3 waits behind Shuttle #5 and Shuttle #5 waits for the lift car
- **THEN** the report on Shuttle #3's rail names Shuttle #5 and that it waits for the lift car

#### Scenario: Cycle
- **WHEN** Shuttle #3 waits behind Shuttle #5 and Shuttle #5 waits behind Shuttle #3
- **THEN** the report names both and says they wait for each other
