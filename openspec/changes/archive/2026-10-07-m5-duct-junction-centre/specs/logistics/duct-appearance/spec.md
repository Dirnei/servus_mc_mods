# Spec Delta

## MODIFIED Requirements

### Requirement: Each line has a color
The Item Pipe and the Power Cable SHALL each have their own color on the block. A duct with one line SHALL show that line's color. A duct with two lines SHALL show both colors as bands running along the duct and its arms, so the player can tell from a distance which lines the block carries. The centre's bands SHALL run along the axis of its arms when every arm lies on one axis. A centre whose arms lie on more than one axis SHALL be a uniform cube in a neutral duct colour, so bends, branches and crossings read as one block.

#### Scenario: Item only
- **WHEN** a duct carries only an Item Pipe
- **THEN** the duct shows the item color only

#### Scenario: Both lines
- **WHEN** a duct carries both lines
- **THEN** both colors are visible on the centre and on every arm that carries both

#### Scenario: East west run
- **WHEN** a two line duct has arms to the east and west only
- **THEN** the bands run through the centre without a break, the same as on a north south run

#### Scenario: Crossing
- **WHEN** a two line duct has arms to the north and to the east
- **THEN** its centre is a uniform cube and the bands start on the arms
