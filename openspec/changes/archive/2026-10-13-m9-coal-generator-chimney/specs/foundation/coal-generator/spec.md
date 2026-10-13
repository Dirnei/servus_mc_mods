## MODIFIED Requirements

### Requirement: Energy is offered and pushed to neighbours
The generator SHALL expose its buffer through the energy capability so neighbours can extract, and SHALL push energy into any adjacent energy receiver every tick up to its output rate. Only the faces of the lower half SHALL do so, except its top, which carries the chimney; the chimney SHALL offer and push nothing.

#### Scenario: Consumer beside the generator
- **WHEN** a machine that accepts FE is adjacent to a running generator
- **THEN** the machine's buffer fills without any cable

#### Scenario: Generator never receives
- **WHEN** another generator or cable offers energy to this generator
- **THEN** it accepts none

#### Scenario: Nothing from the chimney
- **WHEN** a Power Cable duct touches the chimney of a running generator
- **THEN** the duct shows no arm toward it and receives no FE from it

## ADDED Requirements

### Requirement: Generator is a dark steel machine with a chimney
The Coal Generator SHALL be two blocks tall. Its lower half SHALL be a full cube in the dark steel frame: a front with copper coils behind vents, a fire window low down that glows while burning and a small red lamp that lights while burning, sides with vent slats and copper behind them, and a vent plate on top. Its upper half SHALL be a straight square dark steel chimney with a copper band and an overhanging cap, standing centred on the cube. The chimney's outline and collision SHALL follow the stack, so the blocks around it stay visible. The item SHALL show both halves.

#### Scenario: Placed
- **WHEN** a player places a Coal Generator with air above
- **THEN** a dark steel cube with the fire window on the side facing the player and a chimney centred on top appear

#### Scenario: Burning
- **WHEN** the generator burns coal
- **THEN** the fire window glows and the red lamp lights; both go dark when the burn ends

#### Scenario: Chimney outline
- **WHEN** the player looks at the chimney
- **THEN** the selection outline follows the stack instead of a full cube

### Requirement: Generator places and breaks as one
One Coal Generator item SHALL place both halves, facing the same way. Placing SHALL fail and keep the item when the block above the clicked position cannot be replaced. Breaking either half SHALL remove both and drop one Coal Generator, plus the contents of its fuel slot. In creative mode no Coal Generator SHALL drop, and a player without a pickaxe SHALL get no Coal Generator either, like any machine. Sneaking with the Configuration Carrot on either half SHALL dismantle the whole generator into one item. Pistons SHALL move neither half.

#### Scenario: No room above
- **WHEN** a player tries to place a Coal Generator directly under a stone block
- **THEN** nothing is placed and the item stays in the player's hand

#### Scenario: Break the chimney
- **WHEN** a player in survival breaks the chimney of a generator holding coal
- **THEN** both halves disappear and one Coal Generator and the coal drop

#### Scenario: Creative
- **WHEN** a player in creative mode breaks either half
- **THEN** both halves disappear and no Coal Generator drops

#### Scenario: Carrot dismantle
- **WHEN** a player sneaks and uses the Configuration Carrot on the chimney
- **THEN** both halves disappear and one Coal Generator drops

#### Scenario: Piston
- **WHEN** a piston pushes against either half
- **THEN** neither half moves

### Requirement: Chimney forwards to the generator
Right clicking the chimney SHALL open the generator's GUI. The chimney SHALL have no inventory, no buffer and no item, energy, fluid or upkeep capability on any face, so ducts next to it SHALL neither connect to it nor show an arm toward it. Fuel from automation SHALL enter only through the faces of the lower half.

#### Scenario: GUI from the chimney
- **WHEN** a player right clicks the chimney
- **THEN** the Coal Generator GUI opens with its fuel slot and energy bar

#### Scenario: No fuel through the chimney
- **WHEN** an Item Pipe duct with coal touches the chimney
- **THEN** the duct shows no arm toward it and the coal stays in the duct

### Requirement: Smoke while burning
While the generator burns, smoke SHALL rise from the chimney cap. While it does not burn, no smoke SHALL rise. The amount of smoke is look only and does not depend on balance data.

#### Scenario: Burning
- **WHEN** the generator burns coal
- **THEN** smoke rises from the top of the chimney

#### Scenario: Out of fuel
- **WHEN** the last burn ends
- **THEN** the smoke stops
