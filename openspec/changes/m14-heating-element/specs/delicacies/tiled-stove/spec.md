## ADDED Requirements

### Requirement: Installing a Heating Element
A formed Tiled Stove SHALL take one Foundation Heating Element: a right click on any tile of the stove with the element SHALL install it into the firebox and use up one element. A stove that already holds one SHALL refuse a second and tell the player so. A sneaking right click with an empty hand on any tile SHALL take the element out into the player's inventory. When the stove is broken or unformed, the element SHALL drop as an item with the fuel. Installing the element SHALL put out a burning wood fire. A loose tile SHALL NOT take the element.

#### Scenario: Installing
- **WHEN** a player right clicks a tile of a formed stove holding two Heating Elements
- **THEN** the stove holds one element, the player keeps one, and the player is told it now runs on FE

#### Scenario: A second element
- **WHEN** a player right clicks a stove that holds an element with another Heating Element
- **THEN** nothing is taken and the player is told the stove already has one

#### Scenario: Taking it out
- **WHEN** a sneaking player with an empty hand right clicks a tile of a stove with an element
- **THEN** the element goes into the player's inventory and the stove burns fuel again

#### Scenario: Dropped on unform
- **WHEN** a tile of a stove with an element is broken
- **THEN** the element drops as an item

### Requirement: Electric heat
With a Heating Element installed, the firebox SHALL burn no fuel and SHALL refuse new fuel by hand and by automation; fuel already in its slot SHALL stay there. It SHALL store FE in a buffer and SHALL have a temperature from cold to full heat. While a cooking spot asks for heat and the buffer holds at least the FE of one tick, the firebox SHALL draw that FE every tick and its temperature SHALL rise; otherwise it SHALL draw nothing and its temperature SHALL sink. The stove SHALL count as hot, and its spots SHALL cook, only while the temperature is at or above a threshold. Taking the element out SHALL drop the temperature and the stored FE. The FE per tick, the buffer size, the time from cold to full heat, the time from full heat to cold and the threshold SHALL come from datapack JSON (examples: 20 FE per tick, a 4000 FE buffer, 10 s to heat up, 60 s to cool down, hot from 60 percent).

#### Scenario: Preheating
- **WHEN** a Pot with a full recipe stands on a cold stove with an element and FE arrives every tick
- **THEN** the stove draws FE every tick, its temperature climbs, the Pot does not cook until the threshold is reached, and then it cooks without using any fuel

#### Scenario: Power stops
- **WHEN** the FE stops reaching a hot stove with an element while a Pot cooks
- **THEN** the temperature sinks slowly, the Pot cooks on until the temperature falls below the threshold, and then it stops

#### Scenario: Nothing to cook
- **WHEN** a stove with an element and a full buffer has nothing to cook
- **THEN** it draws no FE and its temperature sinks

#### Scenario: Fuel is refused
- **WHEN** a hopper holding Coal points into a stove with an element
- **THEN** the Coal stays in the hopper

#### Scenario: Numbers from data
- **WHEN** a datapack sets the stove's FE per tick to 50
- **THEN** a heating stove draws 50 FE every tick

### Requirement: Power port
With a Heating Element installed, the stove SHALL show a small flat iron power port on the end face to the right of its front (seen from the front), and SHALL take FE through the standard energy capability on that face only. Without the element there SHALL be no port and no face SHALL take FE. The port SHALL NOT give FE out.

#### Scenario: FE through the port
- **WHEN** a power cable runs to the port face of a stove with an element
- **THEN** FE flows into the firebox's buffer

#### Scenario: Other faces
- **WHEN** something offers FE to the front, back, top or left end face of a stove with an element
- **THEN** no face but the port takes it

#### Scenario: No element
- **WHEN** something offers FE to any face of a stove without an element
- **THEN** nothing takes it and no port is shown

### Requirement: Thermometer and coils
With a Heating Element installed, the door tile SHALL show a round brass thermometer above the door whose needle points left when cold and right at full heat and moves smoothly between, and the door slits SHALL show heating coils: warm orange while the stove is hot, dull while it is not. The door SHALL glow more softly than a wood fire and SHALL show no flames and no crackle. The server SHALL tell clients the temperature only now and then (when it crosses a coarse step, the threshold, or turns from heating to cooling), and clients SHALL move the needle on between those updates.

#### Scenario: Needle climbs
- **WHEN** a stove with an element preheats
- **THEN** the needle climbs steadily from the left, and the coils turn warm orange once the stove is hot

#### Scenario: Taking the element out
- **WHEN** the element is taken out
- **THEN** the thermometer, the coils and the port disappear

### Requirement: Wood fire without the element
A stove without a Heating Element SHALL heat with its wood fire exactly as before: hot at once when a fuel item lights, cold when the fuel ends, with no thermometer, no port and no FE.

#### Scenario: No element, no gauge
- **WHEN** a formed stove without an element burns Coal
- **THEN** it shows the flickering fire door and no thermometer or port, and it takes no FE
