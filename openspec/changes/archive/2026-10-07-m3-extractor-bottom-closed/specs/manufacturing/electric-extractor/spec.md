## ADDED Requirements

### Requirement: Bottom side is closed
The Electric Extractor's bottom side, which always touches its deposit, SHALL be fixed to Off for items like its front: no item automation through it, greyed in the side panel, unchanged by the carrot and loaded as Off. Energy SHALL still be accepted on every face.

#### Scenario: Carrot on the bottom
- **WHEN** the player uses the carrot on the bottom face of an Electric Extractor
- **THEN** the bottom stays Off

#### Scenario: Energy from below
- **WHEN** a Power Cable arm touches the extractor's bottom
- **THEN** the extractor still receives energy
