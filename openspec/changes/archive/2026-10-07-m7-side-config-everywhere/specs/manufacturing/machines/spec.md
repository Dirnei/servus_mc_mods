# Spec Delta

## MODIFIED Requirements

### Requirement: Side configuration in the GUI
Every machine GUI SHALL show the Lib side panel with its six relative sides. Clicking a side cycles its mode through Auto, Input, Output, Both, Pull, Push and Off, and the panel SHALL show the current mode by colour and tooltip. The configuration SHALL apply at once and persist with the machine.

#### Scenario: Set top to Input
- **WHEN** the player clicks the top cell until it shows Input
- **THEN** a hopper above the machine can insert, nothing is pushed upward, and the setting survives closing the GUI and reloading the world

#### Scenario: Pull from a chest
- **WHEN** the player sets the left side of an Assembler to Pull and a chest on the left holds iron ingots the configured recipe uses
- **THEN** the ingots move into the Assembler's inputs within a few ticks and cobblestone in the chest stays there

### Requirement: Side modes govern automation
A machine SHALL accept items from automation only through sides that allow insertion, SHALL give items only through sides that allow extraction, SHALL push outputs only through Auto and Push sides, and SHALL pull items its inputs accept only through Pull sides. Auto sides keep the unconfigured behaviour.

#### Scenario: Disabled side
- **WHEN** the side facing a hopper is Off
- **THEN** the hopper can neither insert into nor extract from the machine

#### Scenario: Output only toward the duct
- **WHEN** only the right side is Push and all others are Input
- **THEN** finished items go only into the block on the right, and other neighbours receive nothing

#### Scenario: Output side does not push
- **WHEN** the right side is Output and a chest stands there
- **THEN** finished items stay in the machine until a hopper or duct takes them

### Requirement: Wrench rotates and dismantles
Using the wrench on a machine face SHALL cycle that side's mode and show side and mode in the action bar. Sneaking with the wrench SHALL dismantle it: the machine and its contents drop as items without tool requirements, the dropped machine keeping its configuration. The wrench SHALL NOT rotate machines; the facing is set at placement.

#### Scenario: Rotate
- **WHEN** a player uses the wrench on the left face of a machine in Auto
- **THEN** the machine still faces the same way and its left side is Input

#### Scenario: Dismantle
- **WHEN** a player sneaks and uses the wrench on a machine
- **THEN** the machine block is removed and it drops itself and its contents
