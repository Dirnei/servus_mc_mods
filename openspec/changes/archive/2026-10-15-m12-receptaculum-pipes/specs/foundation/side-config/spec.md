# Spec Delta

## MODIFIED Requirements

### Requirement: Fixed sides
A block MAY fix single relative sides to one mode. A fixed side SHALL keep that mode through cycling, setting, the carrot and loading, SHALL count as default, SHALL be drawn greyed in the side panel, SHALL NOT react to clicks there, and SHALL show no marker on the block even while the carrot or the goggles are in use. A side fixed to Off SHALL offer no item capability, so pipes, hoppers and other mods do not connect to it; a side a player sets to Off SHALL keep its capability.

#### Scenario: Cycling a fixed side
- **WHEN** a machine's front is fixed to Off and the player clicks its cell or uses the carrot on the front
- **THEN** the front stays Off and the cell stays greyed

#### Scenario: Loading
- **WHEN** a saved configuration holds Input for a side the block fixes to Off
- **THEN** the side is Off after loading

#### Scenario: No marker on a fixed side
- **WHEN** the player holds the carrot and looks at the front of a machine
- **THEN** the front shows no frame and no glyph while the other faces show theirs

#### Scenario: No pipe at a fixed front
- **WHEN** an Item Pipe is placed against the front of a machine whose front is fixed to Off
- **THEN** the pipe draws no arm to the front and finds no item handler there

#### Scenario: A side set to Off stays connected
- **WHEN** a player sets a machine's side to Off and an Item Pipe touches that side
- **THEN** the pipe still connects, moves nothing while the side is Off, and works again once the side is set back
