## MODIFIED Requirements

### Requirement: Electric Pump runs on energy
The Electric Pump SHALL spend `energy_per_tick` FE from its buffer while working, SHALL accept FE only on the back and the two sides of its lower half, and SHALL pump more water per second than the Burner Pump with the default data.

#### Scenario: Powered
- **WHEN** an Electric Pump on a pond has energy
- **THEN** it fills its buffer faster than a Burner Pump beside it

#### Scenario: No power
- **WHEN** its energy buffer is empty
- **THEN** progress pauses and resumes when energy arrives

#### Scenario: Power from the side
- **WHEN** a Power Cable duct touches the east side of the lower half
- **THEN** FE flows into the pump

#### Scenario: No power through the top
- **WHEN** a Power Cable duct touches the upper half
- **THEN** no FE flows into the pump

### Requirement: Upper half forwards to the engine
Right clicking the upper half of a Burner Pump or an Electric Pump SHALL open that pump's GUI. The upper half SHALL have no inventory, no buffer and no fluid, item, energy or upkeep capability on any face, so ducts and tanks next to it SHALL neither connect to it nor show an arm or stub toward it.

#### Scenario: GUI from the top
- **WHEN** a player right clicks the chimney of a Burner Pump
- **THEN** the Burner Pump GUI opens with its fuel slot and buffer

#### Scenario: Electric GUI from the top
- **WHEN** a player right clicks the fan grille of an Electric Pump
- **THEN** the Electric Pump GUI opens with its energy bar and buffer

#### Scenario: Duct beside the top
- **WHEN** a Fluid Pipe duct is placed beside the upper half of either pump
- **THEN** the duct shows no arm toward it and receives no water from it

### Requirement: Working engine moves and smokes
While the Burner Pump is lit, its beam SHALL rock about its pivot with a steady stroke, the pump rod SHALL rise and fall with the beam's end, and smoke SHALL rise from the top of the chimney. While it is not lit, the beam and rod SHALL stand still and no smoke SHALL rise. Each time the pump rod of a working Burner Pump or Electric Pump reaches its lowest point, a small water splash SHALL rise at the socket where the rod dips. The stroke speed, the amount of smoke and the splash are look only and do not depend on balance data.

#### Scenario: Working
- **WHEN** a Burner Pump on a pond burns coal and fills its buffer
- **THEN** the beam rocks, the rod moves up and down, smoke rises from the chimney and water splashes at the socket once per stroke

#### Scenario: Buffer full
- **WHEN** the buffer is full and the pump pauses
- **THEN** once the burn ends and the pump goes unlit, the beam stops, the smoke stops and no more splashes rise

#### Scenario: No source
- **WHEN** a Burner Pump with coal stands on dirt
- **THEN** the beam does not move, no smoke rises and nothing splashes

### Requirement: Water leaves through the sides
A pump SHALL have no configurable sides: no side panel in its GUI, no side markers, and the carrot SHALL not change its sides. It SHALL never accept fluid. Both pumps SHALL offer their water only on the back and the two sides of their lower half. On those faces a pump SHALL offer its water to neighbours that drain it and SHALL fill touching fluid handlers itself. The Burner Pump SHALL accept fuel from automation on the same faces.

#### Scenario: Pump beside a tank
- **WHEN** a pump's lower half stands next to a Fluid Tank on its side
- **THEN** the tank fills with water without a pipe

#### Scenario: No side settings
- **WHEN** the player opens a pump's GUI or uses the carrot on one of its faces
- **THEN** no side panel shows and no face changes

#### Scenario: Front and bottom closed
- **WHEN** a tank touches the pump's front
- **THEN** no water reaches it

#### Scenario: Burner Pump top closed
- **WHEN** a Burner Pump stands in place
- **THEN** its lower half offers neither water nor a fuel slot toward its upper half

#### Scenario: Electric Pump top closed
- **WHEN** an Electric Pump stands in place
- **THEN** its lower half offers neither water nor an energy input toward its upper half

#### Scenario: Fuel by automation
- **WHEN** a duct pushes coal into the side of a Burner Pump's lower half
- **THEN** the coal goes into the fuel slot

#### Scenario: Refuses fluid
- **WHEN** a pipe tries to fill water into a pump
- **THEN** the pump accepts nothing

### Requirement: Pump connects to pipes
A pump SHALL show a short pipe stub from its body to the block edge on every face where it offers water and the neighbour is a duct reaching the pump with any line, or a block that takes or gives fluid, so ducts and tanks meet it without a gap. For both pumps these are the back and the two sides of the lower half. A stub toward a duct SHALL be as thick as that duct's arm toward the pump, which grows with the number of lines on it; a stub toward any other block SHALL have the two line size. The stubs SHALL follow neighbours being placed, removed or changed, and the outline SHALL include them.

#### Scenario: Pipe beside the pump
- **WHEN** a duct carrying the Fluid Pipe is placed east of a pump's lower half
- **THEN** the pump shows a stub on its east face that meets the duct's arm

#### Scenario: Pipe on top
- **WHEN** a Fluid Pipe duct is placed above an Electric Pump
- **THEN** no stub reaches up to it and the duct gets no water, since the top of the pump is its upper half

#### Scenario: Power Cable
- **WHEN** a duct carrying only a Power Cable touches the side of an Electric Pump's lower half
- **THEN** the pump shows a stub on that face that meets the cable's arm

#### Scenario: Item duct
- **WHEN** a duct carrying only an Item Pipe touches the side of a Burner Pump's lower half
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

## ADDED Requirements

### Requirement: Electric Pump is a two block electric engine
The Electric Pump SHALL look like the Burner Pump's electric sibling: a beam engine one block wide and two blocks tall with the Burner Pump's base plate, post, beam, pump rod and socket. Its lower half SHALL rest on the block below with the base plate and carry a motor housing with copper windings and vents, and a terminal box with a red power accent. Its upper half SHALL carry a crank wheel, a con rod from the crank wheel to the beam, the pivot of the beam whose pump rod reaches down beside the motor, and a fan grille with a fan behind it. It SHALL show no chimney, no smoke and no lit window. The outline and collision of each half SHALL follow its parts, so the water and blocks around the pump stay visible.

#### Scenario: Looks like an electric engine
- **WHEN** a player places an Electric Pump on a pond
- **THEN** it shows a motor housing with copper windings in the lower block and a crank wheel, con rod, beam and fan grille in the block above

#### Scenario: Water source is checked below the lower half
- **WHEN** an Electric Pump's lower half stands on a water source with two water source neighbours
- **THEN** it can work, whatever is next to its upper half

#### Scenario: Outline per half
- **WHEN** the player looks at either half
- **THEN** the selection outline follows that half's parts instead of a full cube

### Requirement: Electric Pump places and breaks as one
One Electric Pump item SHALL place both halves, facing the same way, and SHALL place on the water surface like the Burner Pump. Placing SHALL fail and keep the item when the block above the clicked position cannot be replaced. Breaking either half SHALL remove both and drop one Electric Pump. In creative mode no Electric Pump SHALL drop, and a player without a pickaxe SHALL get no Electric Pump either, like any machine. Sneaking with the Configuration Carrot on either half SHALL dismantle the whole pump into one item. Pistons SHALL move neither half.

#### Scenario: Placed as two blocks
- **WHEN** a player places an Electric Pump on a pond with air above
- **THEN** the pump fills the block above the water and the block above that

#### Scenario: No room above
- **WHEN** a player tries to place an Electric Pump directly under a stone block
- **THEN** nothing is placed and the item stays in the player's hand

#### Scenario: Break either half
- **WHEN** a player in survival breaks the upper half of an Electric Pump
- **THEN** both halves disappear and exactly one Electric Pump drops

#### Scenario: Carrot dismantle
- **WHEN** a player sneaks and uses the Configuration Carrot on the lower half
- **THEN** both halves disappear and one Electric Pump drops

#### Scenario: Piston
- **WHEN** a piston pushes against either half
- **THEN** neither half moves

### Requirement: Working electric engine moves and sparks
While the Electric Pump works, its crank wheel SHALL turn, the con rod SHALL follow the crank pin and drive the beam, the beam SHALL rock about its pivot with the Burner Pump's stroke, the pump rod SHALL rise and fall with the beam's end, the fan SHALL spin, and now and then a small electric spark SHALL jump at the motor, never a steady stream. While it does not work, every part SHALL stand still and no spark SHALL jump. The motion and the sparks are look only and do not depend on balance data.

#### Scenario: Working
- **WHEN** an Electric Pump on a pond has energy and fills its buffer
- **THEN** the crank wheel turns, the beam rocks, the pump rod moves up and down, the fan spins, a spark shows at the motor now and then and water splashes at the socket once per stroke

#### Scenario: No power
- **WHEN** its energy buffer runs empty
- **THEN** the crank wheel, beam, rod and fan stop where they are and no sparks or splashes show

#### Scenario: Con rod follows the crank
- **WHEN** the crank pin is at its highest point
- **THEN** the beam end above the crank is at its highest point and the pump rod at the other end is at its lowest

## REMOVED Requirements

### Requirement: Compact pump body
**Reason**: The Electric Pump becomes a two block beam engine; the compact one block body with a base plate, a narrower body and an outlet on top goes.
**Migration**: See "Electric Pump is a two block electric engine". Pipes on top of an Electric Pump no longer connect; they go to the back or the sides of the lower half.
