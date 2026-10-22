## Purpose

The Chimney is a slim stone column block that raises a Sink's rate when stacked on it and, standing anywhere else, is a decorative chimney whose smoke the player sets with the Configuration Carrot and redstone.

## ADDED Requirements

### Requirement: Chimney keeps its stone
A Chimney block SHALL keep the stone of its item when placed, SHALL drop an item carrying that stone when mined, and SHALL show Bricks when it has no stone. Only its brick faces SHALL take the stone; the flue of the cap SHALL stay as it is.

#### Scenario: Break and place
- **WHEN** a Deepslate Tiles Chimney block is mined and placed again
- **THEN** the placed block shows deepslate tiles

#### Scenario: No stone
- **WHEN** a Chimney block is given by command without a stone
- **THEN** it shows bricks and its tooltip reads "Stone: Bricks"

### Requirement: Free standing column
A chimney column is a straight vertical run of Chimney blocks. A column whose lowest block does not stand on a formed Sink SHALL be free standing. Its top block SHALL emit smoke when its smoke setting is not off and its redstone mode allows it; no other block of the column SHALL emit smoke. Smoke SHALL be drawn on the client only and SHALL cost no work per tick on the server.

#### Scenario: Smoke from the top
- **WHEN** a free standing column of three Chimney blocks is set to cosy smoke and always
- **THEN** cosy campfire smoke rises above the third block only

#### Scenario: Off by default
- **WHEN** the player places a single Chimney block on grass
- **THEN** it emits no smoke

### Requirement: Smoke setting
Each Chimney block SHALL carry a smoke setting: off, thin (plain smoke particles), cosy (campfire smoke) or signal (tall signal fire smoke). A plain Configuration Carrot click on any block of a free standing column SHALL step every block of that column to the next setting after the clicked block's, in the order off, thin, cosy, signal, off, and the action bar SHALL name the new setting.

#### Scenario: Step the column
- **WHEN** the player clicks the middle block of a free standing column set to thin with the carrot
- **THEN** every block of the column is set to cosy and the action bar names Cosy smoke

#### Scenario: Wrap around
- **WHEN** the player clicks a column set to signal with the carrot
- **THEN** the column is set to off and stops smoking

### Requirement: Redstone mode
Each Chimney block SHALL carry a redstone mode: always, while powered or while unpowered. A sneak Configuration Carrot click on any block of a free standing column SHALL step every block of that column to the next mode after the clicked block's, and the action bar SHALL name the new mode. The column SHALL count as powered when any of its blocks receives a redstone signal. The column SHALL follow signal changes as they happen, without scanning blocks every tick.

#### Scenario: Smoke while powered
- **WHEN** a free standing column is set to thin and while powered, and a lever beside its lowest block is switched on
- **THEN** the top block starts smoking, and stops when the lever is switched off

#### Scenario: Smoke while unpowered
- **WHEN** a free standing column is set to signal and while unpowered, and a redstone block is placed beside its middle block
- **THEN** the top block stops smoking

#### Scenario: Step the mode
- **WHEN** the player sneak clicks a column in mode always with the carrot
- **THEN** every block of the column is in mode while powered and the action bar names it

### Requirement: New blocks copy the block below
A Chimney block placed straight on top of another Chimney block SHALL take that block's smoke setting and redstone mode. A Chimney block placed on anything else SHALL start at off and always.

#### Scenario: Grow a smoking column
- **WHEN** the player places a Chimney block on top of a free standing column set to cosy and while powered
- **THEN** the new block is cosy and while powered, and the smoke now rises above the new top

### Requirement: Settings set aside on a Sink
While a column stands on a formed Sink, the Sink SHALL control its smoke as defined in `foundation/sink`, the column's own settings SHALL be ignored, and Configuration Carrot clicks SHALL change neither setting; the action bar SHALL say that the Sink controls the smoke. The stored settings SHALL apply again as soon as the column no longer stands on a formed Sink.

#### Scenario: Carrot on a Sink chimney
- **WHEN** the player clicks a Chimney block of a column on a formed Sink with the carrot
- **THEN** nothing changes and the action bar says the Sink controls the smoke

#### Scenario: Settings come back
- **WHEN** a column set to signal and always stands on a formed Sink and the player breaks one Sink block
- **THEN** the column emits signal smoke from its top

### Requirement: No dismantle with the carrot
The Configuration Carrot SHALL NOT dismantle a Chimney block; the sneak carrot click SHALL only step the redstone mode.

#### Scenario: Sneak carrot keeps the block
- **WHEN** the player sneak clicks a single Chimney block with the carrot
- **THEN** the block stays and its redstone mode steps on

### Requirement: Chimney settings are pure logic
Stepping the smoke setting and the redstone mode, and deciding whether a top block smokes from its setting, its mode, the column's power and whether it stands on a formed Sink, SHALL be pure Java without Minecraft classes and covered by unit tests.

#### Scenario: Unit test of the smoke rule
- **WHEN** a unit test asks whether a column set to thin and while unpowered, powered and not on a Sink, smokes
- **THEN** the answer is no
