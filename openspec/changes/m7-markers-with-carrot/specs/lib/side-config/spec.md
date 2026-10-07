# Spec Delta

## MODIFIED Requirements

### Requirement: Side modes shown on the block
While the viewing player holds the Configuration Carrot or wears the goggles, every block with a side configuration SHALL draw on each face a thin frame along its edges in the colour the side panel uses for that side's mode, and a centred glyph: a square for Input and Pull, a diamond for Output and Push, a square with a dot for Both, a cross for Off, with Pull and Push filled and Input and Output outlined. Without the carrot or the goggles no marker SHALL be drawn. Markers SHALL render at full brightness and SHALL cost nothing while nobody configures.

#### Scenario: Configured side from afar
- **WHEN** the top of an Inbound Station is set to Pull and the player looks at it with empty hands
- **THEN** no marker is drawn on any face

#### Scenario: Carrot in hand
- **WHEN** the player holds the carrot and looks at a machine in Auto
- **THEN** every face shows the Auto frame and the Auto glyph, and after setting the left side to Push its frame and glyph change at once

#### Scenario: Plain block
- **WHEN** a machine has every side in Auto and the player holds nothing special
- **THEN** no marker is drawn
