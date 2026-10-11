## ADDED Requirements

### Requirement: Rail track look
A rail SHALL look like a wide gauge track: two raised rail heads, each 2 pixels wide, at 3..5 and 11..13 pixels across the travel direction and one pixel above the sleepers, and four separate sleepers per block, each 2 pixels long along the travel direction with gaps between them and 14 pixels wide, so they stick out past the heads. The Warehouse Rail's heads SHALL be light steel. The whole rail SHALL stay at most 2 pixels high.

#### Scenario: Straight run from above
- **WHEN** a player looks down on three rails in a line
- **THEN** two steel heads run unbroken along the whole run and the ground shows between the sleepers

#### Scenario: Shuttle on the rail
- **WHEN** a shuttle drives along a straight rail
- **THEN** its wheels roll on the two heads

### Requirement: Rail pieces follow the gauge
Every piece of a rail SHALL keep the gauge of the straight track: a straight run, the end of a run that stops at the far side of the centre, a curve whose heads are stepped arcs from one side's heads to the other side's heads with sleepers along the arc, a `#` crossing where both pairs of heads cross over a sleeper plate, and a dock piece that ends the heads at a striped buffer beam as wide as the sleepers. A rail with no rail beside it SHALL show a short piece of track along the axis of its dock, if any.

#### Scenario: Curve
- **WHEN** a rail has rails on its north and east sides only
- **THEN** both heads bend in a stepped arc from the north edge to the east edge and meet the heads of both neighbours without a step

#### Scenario: Crossing
- **WHEN** a rail has rails on all four sides
- **THEN** both pairs of heads run across the block over a sleeper plate

#### Scenario: Dock
- **WHEN** a rail runs north into an Inbound Station
- **THEN** the heads end at a striped buffer beam in front of the station

### Requirement: Rail sleepers show the recipe's wood
The rail recipe SHALL take planks, and the crafted rails SHALL carry the wood of those planks. A placed rail SHALL show its sleepers in that wood and SHALL drop a rail carrying it again when broken or dismantled. A rail without a wood SHALL show oak. The item SHALL show a short piece of track with sleepers in its wood, and its tooltip SHALL name the wood. Rails of different woods do not stack.

#### Scenario: Spruce rails
- **WHEN** a player crafts rails with spruce planks and places them
- **THEN** the sleepers are spruce and a broken rail drops a spruce rail

#### Scenario: Sneak placement keeps the wood
- **WHEN** a player sneak places a run of birch rails
- **THEN** every rail of the run shows birch sleepers

## MODIFIED Requirements

### Requirement: Rail is obtainable and dismantled
The rail SHALL be craftable from vanilla materials in a batch, SHALL drop itself when broken, and SHALL be dismantled by sneaking with the Configuration Carrot. When the block under a rail is removed the rail SHALL become hung or free and SHALL drop itself only when it collapses with its gap.

#### Scenario: Crafting
- **WHEN** the player places iron ingots and planks in the rail pattern (for example two columns of three ingots with a plank between them)
- **THEN** the result is several rails (for example sixteen) carrying the wood of the planks

#### Scenario: Sneak dismantle
- **WHEN** the player sneaks and uses the Configuration Carrot on a rail
- **THEN** the rail drops as an item

#### Scenario: Floor removed
- **WHEN** the block under a rail is broken and the rail touches no anchor and no other rail
- **THEN** the rail drops as an item

#### Scenario: Floor removed beside a rack
- **WHEN** the block under a rail in front of a Heavy Duty Rack is broken and no other rail touches it
- **THEN** the rail stays, hung on the rack front
