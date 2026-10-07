## ADDED Requirements

### Requirement: Bottom side is closed
The Burner Extractor's bottom side, which always touches its deposit, SHALL be fixed to Off like its front: no automation through it, greyed in the side panel, unchanged by the carrot and loaded as Off.

#### Scenario: Carrot on the bottom
- **WHEN** the player uses the carrot on the bottom face of a Burner Extractor
- **THEN** the bottom stays Off

#### Scenario: Side panel
- **WHEN** the player opens the extractor's GUI
- **THEN** the bottom cell of the side panel is greyed and ignores clicks
