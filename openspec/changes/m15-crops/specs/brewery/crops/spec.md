## Purpose

Defines the brewing crops of Servus: Brewery: barley as a field crop, hops that climb a placed Hop Pole, and how players first find Barley Seeds and Hop Rhizomes.

## ADDED Requirements

### Requirement: Barley crop
Barley Seeds SHALL plant a Barley crop on farmland. The crop SHALL grow through eight stages on random ticks under the same rules as vanilla wheat (light, farmland, neighbours) and bone meal SHALL advance it like wheat.

#### Scenario: Planting barley
- **WHEN** a player uses Barley Seeds on the top of farmland
- **THEN** a young Barley crop appears on it and one seed is used up

#### Scenario: Bone meal on barley
- **WHEN** a player uses bone meal on a young Barley crop until it is fully grown
- **THEN** the crop reaches its last stage and accepts no more bone meal

### Requirement: Barley drops by age
A ripe Barley crop SHALL drop one Barley and some Barley Seeds when broken; an unripe one SHALL drop one Barley Seeds only. The amounts SHALL come from the crop's loot table.

#### Scenario: Breaking ripe barley
- **WHEN** a player breaks a Barley crop at its last stage
- **THEN** it drops one Barley and at least one Barley Seeds

#### Scenario: Breaking young barley
- **WHEN** a player breaks a Barley crop below its last stage
- **THEN** it drops one Barley Seeds and no Barley

### Requirement: Barley on a Shuttles field
Barley SHALL be an ordinary age crop to other mods, so a field that grows ordinary age crops can plant, watch and harvest it.

#### Scenario: Barley as a Field Head crop
- **WHEN** Servus: Shuttles is installed and a player opens the crop picker of a Field Head
- **THEN** Barley Seeds is listed as a crop and a field set to it is planted and harvested like wheat

### Requirement: Hop Pole
The Hop Pole SHALL be a thin wooden pole block crafted from sticks. It SHALL stand on top of a block with a solid top centre, on farmland or on another Hop Pole, and SHALL break with its drop when that support goes. A column of Hop Poles SHALL be at most three blocks tall.

#### Scenario: Stacking poles
- **WHEN** a player places Hop Poles on top of each other
- **THEN** the second and third pole are placed and a fourth one on top of the column is refused

#### Scenario: Support removed
- **WHEN** the block under a column of Hop Poles is broken
- **THEN** every pole of the column breaks and drops itself

#### Scenario: Pole on farmland
- **WHEN** a Hop Pole stands on farmland
- **THEN** the farmland stays farmland

### Requirement: Planting hops
Using a Hop Rhizome on a Hop Pole SHALL plant a young hop vine at the foot of its column when the foot pole stands on farmland or on a dirt block and has no vine yet; the rhizome SHALL be used up. Otherwise the rhizome SHALL do nothing.

#### Scenario: Rhizome on a pole on farmland
- **WHEN** a player uses a Hop Rhizome on a bare Hop Pole that stands on farmland
- **THEN** a young vine with small leaves appears at the foot of the pole and one rhizome is used up

#### Scenario: Rhizome on an upper pole
- **WHEN** a player uses a Hop Rhizome on the top pole of a bare column that stands on grass
- **THEN** the young vine is planted at the foot pole of the column

#### Scenario: Rhizome on a pole on stone
- **WHEN** a player uses a Hop Rhizome on a Hop Pole that stands on stone
- **THEN** nothing is planted and the rhizome stays in the hand

### Requirement: Hops climb the pole
A hop vine SHALL grow in steps on random ticks of the pole parts it covers, only in enough light. A young part SHALL leaf out; a leafed part with a bare pole above it SHALL let the vine climb one block onto that pole as a young part. Growth SHALL only read the column itself, never scan around it.

#### Scenario: Vine climbs
- **WHEN** the foot part of a three pole column has leafed out and keeps receiving growth steps
- **THEN** the vine climbs onto the middle pole and then onto the top pole, one block per step after each part has leafed out

#### Scenario: Pole added on top
- **WHEN** a player adds a bare Hop Pole on top of a column whose vine has reached its top
- **THEN** the vine later climbs onto the new pole as well

### Requirement: Hop cones
Once the vine has leafed out on the top pole of its column, every leafed part above the foot SHALL grow green Hop cones on a later growth step. The foot part SHALL never bear cones, so a single pole bears none.

#### Scenario: Cones on a two pole column
- **WHEN** the vine of a two pole column has leafed out on both poles and receives a growth step
- **THEN** the top part shows green Hop cones and the foot part stays leaves only

#### Scenario: Single pole
- **WHEN** a hop vine grows on a column of a single Hop Pole
- **THEN** it leafs out and never bears cones

### Requirement: Picking hops
Using an empty hand, or any item that has no own use there, on a pole part with cones SHALL drop Hops from the hop harvest loot table, play a picking sound and turn the part back into leaves; the vine SHALL stay. The cones SHALL grow again later.

#### Scenario: Picking cones
- **WHEN** a player right clicks a pole part with Hop cones
- **THEN** Hops drop, the part shows leaves without cones, and the vine on the column is unchanged

#### Scenario: No cones
- **WHEN** a player right clicks a leafed pole part without cones
- **THEN** nothing drops

### Requirement: Bone meal on hops
Bone meal used on any part of a column with a vine SHALL make the next growth step of that column, from the foot up, as one random growth step would. A column with nothing left to grow SHALL refuse bone meal.

#### Scenario: Bone meal makes the vine climb
- **WHEN** a player uses bone meal on a column whose foot part has leafed out and whose middle pole is bare
- **THEN** the vine climbs onto the middle pole

#### Scenario: Fully grown column
- **WHEN** every upper part of a column bears cones and the vine covers the top pole
- **THEN** bone meal on the column is not used up

### Requirement: Breaking hop poles
Breaking a pole part SHALL drop the Hop Pole; the foot part with a vine SHALL also drop the Hop Rhizome, and a part with cones SHALL also drop some Hops. The drops SHALL come from the pole's loot table.

#### Scenario: Breaking the foot
- **WHEN** a player breaks the foot pole of a column with a vine
- **THEN** it drops a Hop Pole and a Hop Rhizome, and the poles above break and drop their own items

#### Scenario: Breaking a part with cones
- **WHEN** a player breaks the top pole of a column while it bears cones
- **THEN** it drops a Hop Pole and some Hops, and the vine below stays

### Requirement: Hops are not a field crop
The Hop Rhizome SHALL NOT be an ordinary age crop item, so field automation that plants age crops does not offer it, and a Hop Pole on a field plot SHALL count as a blocked plot.

#### Scenario: Field Head crop list
- **WHEN** Servus: Shuttles is installed and a player opens the crop picker of a Field Head
- **THEN** the Hop Rhizome is not listed

### Requirement: Finding seeds and rhizomes
Breaking short grass without shears SHALL sometimes drop Barley Seeds and rarely a Hop Rhizome, and the chests of plains and taiga village houses SHALL sometimes hold Barley Seeds and Hop Rhizomes. The chances SHALL live in Brewery loot tables added through loot modifiers, so a datapack can change them.

#### Scenario: Grass drops
- **WHEN** a player breaks many short grass blocks by hand
- **THEN** some of them drop Barley Seeds and very few drop a Hop Rhizome, besides the vanilla drops

#### Scenario: Shears
- **WHEN** a player breaks short grass with shears
- **THEN** it drops neither Barley Seeds nor a Hop Rhizome

#### Scenario: Village chest
- **WHEN** a player opens many plains or taiga village house chests
- **THEN** some of them hold Barley Seeds or a Hop Rhizome

### Requirement: Crop tags and composting
Barley and Hops SHALL be tagged `c:crops` with `c:crops/barley` and `c:crops/hops`; Barley Seeds SHALL be tagged `c:seeds` and `c:seeds/barley`, and farmer villagers and chickens SHALL treat them like wheat seeds. Barley, Barley Seeds, Hops and the Hop Rhizome SHALL be compostable, with chances from the compostables data map.

#### Scenario: Tag lookup
- **WHEN** another mod looks up the item tag `c:crops/barley`
- **THEN** it contains Barley

#### Scenario: Composting
- **WHEN** a player puts Barley into a composter
- **THEN** the composter may rise a level, like with wheat

### Requirement: Crop names and creative tab
The crop items SHALL be named Barley, Barley Seeds, Hops, Hop Rhizome and Hop Pole in English and SHALL be listed in the Brewery creative tab after the Beer Glass.

#### Scenario: Creative tab
- **WHEN** a player opens the Brewery creative tab
- **THEN** it lists Barley Seeds, Barley, Hop Rhizome, Hops and Hop Pole after the Beer Glass, each with its English name
