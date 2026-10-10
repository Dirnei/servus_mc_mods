# Spec Delta

## ADDED Requirements

### Requirement: Burner Pump is a two block steam engine
The Burner Pump SHALL look like a small beam engine one block wide and two blocks tall. Its lower half SHALL rest on the block below with a base plate and carry the firebox on its front and the boiler. Its upper half SHALL carry a chimney, a cylinder and the pivot of a beam whose rod reaches down beside the boiler. The outline and collision of each half SHALL follow its parts, so the water and blocks around the pump stay visible.

#### Scenario: Looks like an engine
- **WHEN** a player places a Burner Pump on a pond
- **THEN** it shows a firebox and boiler in the lower block and a chimney, cylinder and beam in the block above

#### Scenario: Water source is checked below the lower half
- **WHEN** a Burner Pump's lower half stands on a water source with two water source neighbours
- **THEN** it can work, whatever is next to its upper half

#### Scenario: Outline per half
- **WHEN** the player looks at either half
- **THEN** the selection outline follows that half's parts instead of a full cube

### Requirement: Burner Pump places and breaks as one
One Burner Pump item SHALL place both halves, facing the same way. Placing SHALL fail and keep the item when the block above the clicked position cannot be replaced. Breaking either half SHALL remove both and drop one Burner Pump, plus the contents of its fuel slot. In creative mode no Burner Pump SHALL drop, and a player without a pickaxe SHALL get no Burner Pump either, like any machine. Sneaking with the Configuration Carrot on either half SHALL dismantle the whole pump into one item. Pistons SHALL move neither half.

#### Scenario: Placed as two blocks
- **WHEN** a player places a Burner Pump with air above
- **THEN** the pump fills the clicked position and the block above it

#### Scenario: No room above
- **WHEN** a player tries to place a Burner Pump directly under a stone block
- **THEN** nothing is placed and the item stays in the player's hand

#### Scenario: Break the top
- **WHEN** a player in survival breaks the upper half of a Burner Pump holding coal
- **THEN** both halves disappear and one Burner Pump and the coal drop

#### Scenario: Break the bottom
- **WHEN** a player in survival breaks the lower half
- **THEN** both halves disappear and exactly one Burner Pump drops

#### Scenario: Creative
- **WHEN** a player in creative mode breaks either half
- **THEN** both halves disappear and no Burner Pump drops

#### Scenario: Piston
- **WHEN** a piston pushes against either half
- **THEN** neither half moves

### Requirement: Pumps place on the water surface
Both pump items SHALL place on top of the water source the player looks at, like a lily pad, when that water source is the nearest thing in the line of sight. Without a water source in the line of sight they SHALL place like any block.

#### Scenario: Click through the water
- **WHEN** a player looks down at a pond and uses a Burner Pump or an Electric Pump
- **THEN** the pump is placed on the block above the water source and the water source stays

#### Scenario: Out of block reach
- **WHEN** the pond floor is too far away to click but the water surface is within reach
- **THEN** the pump is still placed on the water surface

#### Scenario: On land
- **WHEN** a player uses a pump on stone with no water in the line of sight
- **THEN** it places on the stone as before

### Requirement: Upper half forwards to the engine
Right clicking the upper half SHALL open the Burner Pump's GUI. The upper half SHALL have no inventory, no buffer and no fluid, item, energy or upkeep capability on any face, so ducts and tanks next to it SHALL neither connect to it nor show an arm or stub toward it.

#### Scenario: GUI from the top
- **WHEN** a player right clicks the chimney of a Burner Pump
- **THEN** the Burner Pump GUI opens with its fuel slot and buffer

#### Scenario: Duct beside the top
- **WHEN** a Fluid Pipe duct is placed beside the upper half
- **THEN** the duct shows no arm toward it and receives no water from it

### Requirement: Working engine moves and smokes
While the Burner Pump is lit, its beam SHALL rock about its pivot with a steady stroke, the pump rod SHALL rise and fall with the beam's end, and smoke SHALL rise from the top of the chimney. While it is not lit, the beam and rod SHALL stand still and no smoke SHALL rise. The stroke speed and the amount of smoke are look only and do not depend on balance data.

#### Scenario: Working
- **WHEN** a Burner Pump on a pond burns coal and fills its buffer
- **THEN** the beam rocks, the rod moves up and down and smoke rises from the chimney

#### Scenario: Buffer full
- **WHEN** the buffer is full and the pump pauses
- **THEN** once the burn ends and the pump goes unlit, the beam stops and the smoke stops

#### Scenario: No source
- **WHEN** a Burner Pump with coal stands on dirt
- **THEN** the beam does not move and no smoke rises

## MODIFIED Requirements

### Requirement: Water leaves through the sides
A pump SHALL have no configurable sides: no side panel in its GUI, no side markers, and the carrot SHALL not change its sides. It SHALL never accept fluid. The Electric Pump SHALL offer its water on every face except its front and its bottom; the Burner Pump SHALL offer it only on the back and the two sides of its lower half. On those faces a pump SHALL offer its water to neighbours that drain it and SHALL fill touching fluid handlers itself. The Burner Pump SHALL accept fuel from automation on the same faces.

#### Scenario: Pump beside a tank
- **WHEN** a pump stands next to a Fluid Tank
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

#### Scenario: Fuel by automation
- **WHEN** a duct pushes coal into the side of a Burner Pump's lower half
- **THEN** the coal goes into the fuel slot

#### Scenario: Refuses fluid
- **WHEN** a pipe tries to fill water into a pump
- **THEN** the pump accepts nothing

### Requirement: Pump connects to pipes
A pump SHALL show a short pipe stub from its body to the block edge on every face where it offers water and the neighbour is a duct reaching the pump with any line, or a block that takes or gives fluid, so ducts and tanks meet it without a gap. For the Electric Pump these are every face except its front and bottom; for the Burner Pump the back and the two sides of its lower half. A stub toward a duct SHALL be as thick as that duct's arm toward the pump, which grows with the number of lines on it; a stub toward any other block SHALL have the two line size. The stubs SHALL follow neighbours being placed, removed or changed, and the outline SHALL include them.

#### Scenario: Pipe beside the pump
- **WHEN** a duct carrying the Fluid Pipe is placed east of a pump
- **THEN** the pump shows a stub on its east face that meets the duct's arm

#### Scenario: Pipe on top
- **WHEN** a Fluid Pipe duct is placed above an Electric Pump
- **THEN** the outlet on top reaches up to the duct

#### Scenario: Power Cable
- **WHEN** a duct carrying only a Power Cable touches the side of an Electric Pump
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

### Requirement: Compact pump body
The Electric Pump SHALL be smaller than a full block: a base plate that covers most of the bottom face and rests on the block below, a narrower body that carries the front face, and a short outlet on top. Its outline and collision SHALL follow that shape, and the blocks around it SHALL stay visible.

#### Scenario: On a pond
- **WHEN** an Electric Pump stands on the water of a pond
- **THEN** its base plate sits on the water surface and the water and blocks around the narrower body stay visible

#### Scenario: Outline
- **WHEN** the player looks at an Electric Pump
- **THEN** the selection outline follows the base, the body and the outlet instead of a full cube
