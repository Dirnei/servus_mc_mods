# Spec Delta

## MODIFIED Requirements

### Requirement: Connected look
Blocks of a formed tank SHALL hide their frame edges toward each other so the tank looks like one glass box with a copper frame around its outside. A block SHALL show its frame toward blocks of another tank, and a block working on its own SHALL show its full frame. Instead of a glass bottom, a block with no block of its own tank below it SHALL show its floor block as a thin plate, visible from below and from inside. Tank blocks SHALL let light pass like air and cast no ambient occlusion shadow.

#### Scenario: Formed tank
- **WHEN** four blocks form a 2 by 2 tank
- **THEN** no frame shows between the blocks, only around the outside of the box

#### Scenario: Block on top
- **WHEN** a single block stands on top of a formed 2 by 2 tank
- **THEN** a frame shows between the single block and the tank below it

#### Scenario: Floor
- **WHEN** a player looks down into an empty tank crafted with stone bricks, or up at it from below
- **THEN** they see a stone brick floor, not the blocks beneath the tank

#### Scenario: Two layers
- **WHEN** a 2 by 2 tank is two layers high
- **THEN** only the lower layer shows its floor and no floor shows between the layers

#### Scenario: Even light
- **WHEN** a 3 by 3 tank stands in daylight
- **THEN** its middle column is as bright as its edges

### Requirement: Crafting
The Fluid Tank SHALL be crafted from copper ingots in the corners, glass on the top, left and right edges, an empty centre, and one solid full block without a block entity in the bottom middle, which becomes the tank's floor. Glass and other blocks that are not solid full cubes SHALL NOT be accepted as a floor.

#### Scenario: Recipe
- **WHEN** the player crafts copper ingots in the corners, glass on the three edges and stone bricks in the bottom middle around an empty centre
- **THEN** one Fluid Tank with a stone brick floor is produced

#### Scenario: Glass floor refused
- **WHEN** the bottom middle holds glass
- **THEN** nothing is crafted

### Requirement: Broken block keeps its share
A broken tank block SHALL drop as a tank item carrying the fluid that was in that block and its floor block, show the fluid and amount and the floor in its tooltip, and add that fluid back and show that floor when placed. Fluid that was in the other blocks SHALL stay in them.

#### Scenario: Move a full block
- **WHEN** a single full tank block of water is broken and placed elsewhere
- **THEN** the new tank holds the same amount of water

#### Scenario: Top of a tower
- **WHEN** the empty top block of a half full tower is broken
- **THEN** it drops an empty tank item and the remaining tower keeps all its fluid

#### Scenario: Floor travels with the item
- **WHEN** a tank block with a stone brick floor is broken and placed elsewhere
- **THEN** the new block shows a stone brick floor

### Requirement: Buckets
Using a filled bucket on a tank SHALL empty it into the tank when the tank has room and holds that fluid or nothing. Using an empty bucket SHALL fill it from the tank when the tank holds at least one bucket's worth; a player in creative mode SHALL keep the empty bucket and get the filled bucket into the inventory unless it holds one already, like vanilla buckets. Using an empty hand SHALL tell the player the fluid, the amount and the capacity.

#### Scenario: Water bucket
- **WHEN** a player uses a water bucket on an empty tank
- **THEN** the tank holds 1,000 mB of water and the player holds an empty bucket

#### Scenario: Creative scoop
- **WHEN** a creative player uses an empty bucket on a tank holding Salt
- **THEN** the tank loses 1,000 mB, the player still holds the empty bucket and a Salt bucket is in the inventory

#### Scenario: Read the tank
- **WHEN** a player right clicks a tank with an empty hand
- **THEN** the player sees the fluid name and the amount and capacity in mB
