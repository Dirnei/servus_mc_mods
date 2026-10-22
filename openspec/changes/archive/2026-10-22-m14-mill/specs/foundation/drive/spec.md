## Purpose

The drive link lets a driver hand work to the one block it touches, without a shaft network. It covers the Hand Crank, the Electric Motor with its Speed Upgrade, their balance data and their look.

## ADDED Requirements

### Requirement: Drive link
Foundation SHALL provide a sided block capability `servus_foundation:drive` that a block registers to take drive. Through it a driver SHALL ask whether the block wants work right now and SHALL give it work as a whole number of work units. A driver SHALL drive only the one block it touches; drive SHALL NOT pass on through the driven block.

#### Scenario: Another mod's block
- **WHEN** a mod registers the drive capability on its block entity and a Hand Crank is placed against it
- **THEN** turning the crank gives that block work

#### Scenario: Block without drive
- **WHEN** a motor faces a stone block
- **THEN** it spends no energy and gives no work

### Requirement: Drive logic is pure
Work progress toward a goal, the motor's rate with upgrades, the crank's hold and turn counting and the flywheel curve SHALL be plain Java without Minecraft types, covered by JUnit tests.

#### Scenario: Unit tests
- **WHEN** `gradlew :foundation:test` runs
- **THEN** the drive logic tests pass without the game

### Requirement: Hand Crank placement
Foundation SHALL provide the Hand Crank, crafted from vanilla materials. It SHALL be placeable only against a horizontal side of a block that takes drive on that side, SHALL drive that block, and SHALL drop as an item when that block is removed.

#### Scenario: Nowhere to place
- **WHEN** a player tries to place a Hand Crank against a stone block
- **THEN** no crank is placed

#### Scenario: Mill broken
- **WHEN** the Mill a crank sits on is broken
- **THEN** the crank drops as an item

### Requirement: Turning the Hand Crank
While a player holds right click on the crank, the crank SHALL turn continuously at a constant speed, one full turn in a fixed time (about 0.8 seconds), and SHALL hand the driven block its work spread over the turn. Each full turn SHALL give a small amount of work (example 25) and cost the player a little food exhaustion (example 0.1), but only while the block wants work. When the player lets go the crank SHALL give no more work.

#### Scenario: Cranking a mill
- **WHEN** a player holds right click on a Hand Crank on a Mill holding Gravel long enough for the recipe's work
- **THEN** a Sand comes out and the player's exhaustion has grown

#### Scenario: Nothing to do
- **WHEN** a player turns a crank on an empty Mill
- **THEN** the crank turns but gives no work and costs no exhaustion

#### Scenario: Letting go
- **WHEN** the player stops holding right click on a crank on a Mill holding Gravel
- **THEN** the crank stops turning within half a second and the Mill's progress stops growing

### Requirement: Flywheel motion
The crank's arm and handle, the Mill's grinding wheel and the motor's coupling and fan SHALL turn smoothly between ticks, interpolated every frame. When their drive starts they SHALL reach full speed within a few ticks and then turn at a constant speed; when it stops they SHALL coast, slowing down to a stop within about a second, and then stand still. Coasting SHALL give no work.

#### Scenario: Let go of the crank
- **WHEN** a player cranking a Mill lets go
- **THEN** the crank arm and the grinding wheel slow down and stop within about a second, and the Mill grinds nothing more

### Requirement: Spin state sync
The server SHALL decide whether a crank, Mill or motor is turning and SHALL tell clients only when that starts or stops, not every tick; clients SHALL compute the motion from it.

#### Scenario: Steady cranking
- **WHEN** a player holds right click on a crank for several seconds
- **THEN** the server sends the crank's turning state once when it starts and once when it stops

### Requirement: Electric Motor
Foundation SHALL provide the Electric Motor, crafted from vanilla and Foundation materials. Its shaft SHALL point away from the player when placed, or toward the block taking drive whose side the player clicked, and SHALL drive the block it faces. It SHALL take FE only through its back through `Capabilities.Energy` into a buffer and SHALL never give energy away.

#### Scenario: Power only from the back
- **WHEN** a generator touches the motor's side
- **THEN** no energy goes in; touching the back, it does

### Requirement: Motor works only when there is work
Each tick that the driven block wants work and the buffer holds the tick's cost, the motor SHALL spend that cost (example 20 FE) and give the block its work per tick (example 5); otherwise it SHALL spend nothing. A Hand Crank and a motor MAY drive the same block from different sides.

#### Scenario: Powered motor
- **WHEN** a powered motor faces a Mill holding Gravel
- **THEN** the Mill turns the Gravel into Sand and the motor's buffer drops by its cost each working tick

#### Scenario: Idle motor
- **WHEN** a powered motor faces an empty Mill
- **THEN** its buffer stays full

#### Scenario: No power
- **WHEN** a motor with an empty buffer faces a Mill holding Gravel
- **THEN** the Mill's progress does not move

### Requirement: Motor Speed Upgrade
The Electric Motor SHALL have one upgrade slot in its screen that follows the Foundation upgrade slot rules. With upgrades its work per tick and its FE per tick SHALL both be multiplied by the upgrade multiplier, so the energy per unit of work stays the same.

#### Scenario: One Speed Upgrade
- **WHEN** a motor with base 5 work and 20 FE per tick holds a Speed Upgrade of multiplier 2
- **THEN** it gives 10 work and spends 40 FE per working tick

### Requirement: Drive balance is data
The crank's work per turn and exhaustion per turn and the motor's work per tick SHALL be loaded from `data/<namespace>/drive/<block>.json` and SHALL fall back to built in defaults when a file or field is missing. The motor's FE per tick and buffer SHALL be loaded from its machine stats `data/servus_foundation/machine/electric_motor.json`.

#### Scenario: Retuned crank
- **WHEN** a datapack sets `work_per_turn` of `servus_foundation:hand_crank` to 50 and the server reloads
- **THEN** each crank turn gives 50 work

### Requirement: Motor look
The Electric Motor SHALL look like the Electric Pump's motor: a steel body with copper windings on iron feet, a fan grille on the back with a fan behind it and a shaft with a coupling on the front. While it works the coupling and the fan SHALL turn and now and then a spark SHALL jump at the shaft; when it stops working they coast to a stop, and standing, nothing moves. No two overlapping faces of its parts or of the crank's parts SHALL share a plane.

#### Scenario: Working motor
- **WHEN** a motor drives a grinding Mill
- **THEN** its coupling and fan turn and sparks show; when the Mill runs out of Gravel they slow down and stop
