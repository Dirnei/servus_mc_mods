## ADDED Requirements

### Requirement: Stone blocks
The Sink block and the Chimney block SHALL be blocks of the stone kind. Their recipes SHALL accept any item of `#servus_foundation:stones` and record the stone of the grid on the result by the material pick rule; the parts of their models drawn in the vanilla bricks texture SHALL show that stone.

#### Scenario: Mixed stones in a Sink recipe
- **WHEN** the player crafts Sink blocks from five Stone Bricks and three Cobblestone around iron bars
- **THEN** the result carries Stone Bricks

#### Scenario: Chimney from tuff
- **WHEN** the player crafts Chimney blocks from six Tuff Bricks
- **THEN** the result carries Tuff Bricks and its tooltip reads "Stone: Tuff Bricks"
