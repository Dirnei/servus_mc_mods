# Spec Delta

## ADDED Requirements

### Requirement: Side configuration in the GUI
Every machine GUI SHALL show a panel with its six relative sides. Clicking a side cycles its mode and the panel SHALL show the current mode by color and tooltip. The configuration SHALL apply at once and persist with the machine.

#### Scenario: Set top to Input
- **WHEN** the player clicks the top cell until it shows Input
- **THEN** a hopper above the machine can insert, nothing is pushed upward, and the setting survives closing the GUI and reloading the world

### Requirement: Side modes govern automation
A machine SHALL accept items from automation only through sides that allow insertion, SHALL give items only through sides that allow extraction, and SHALL push outputs only through sides that allow output. Auto sides keep the unconfigured behavior.

#### Scenario: Disabled side
- **WHEN** the side facing a hopper is Disabled
- **THEN** the hopper can neither insert into nor extract from the machine

#### Scenario: Output only toward the duct
- **WHEN** only the right side is Output and all others are Input
- **THEN** finished items go only into the block on the right, and other neighbours receive nothing

### Requirement: Dismantling keeps the configuration
A machine dismantled with the sneak wrench SHALL drop as an item that restores its side configuration, selected recipe and owner when placed again. A machine broken with a tool drops a fresh machine.

#### Scenario: Move a configured assembler
- **WHEN** the player sneak wrenches an assembler with sides configured and a recipe selected and places it elsewhere
- **THEN** the new assembler has the same sides and recipe
