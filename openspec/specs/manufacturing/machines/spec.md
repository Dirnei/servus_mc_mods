# manufacturing/machines Specification

## Purpose

Rules every Servus Manufacturing machine follows, so players and other mods can rely on the same handling for facing, inventories, GUIs, output and the wrench.

## Requirements

### Requirement: Machines face the player and open a GUI
A machine SHALL be placed facing the player and SHALL open its GUI on right click with an empty hand or any non wrench item.

#### Scenario: Placement
- **WHEN** a player places a machine while looking north
- **THEN** the machine's front faces south, toward the player

#### Scenario: Open GUI
- **WHEN** a player right clicks a machine
- **THEN** the machine's GUI opens showing its slots and progress

### Requirement: Inventory sides
A machine SHALL expose its inventory through the item handler capability on every side. Input and fuel slots accept insertion only, output slots allow extraction only, and automation SHALL never extract inputs or insert into outputs.

#### Scenario: Hopper feeds fuel
- **WHEN** a hopper above a burner machine holds coal
- **THEN** the coal moves into the machine's fuel slot

#### Scenario: Hopper pulls output
- **WHEN** a hopper below a machine with an output item runs
- **THEN** it extracts only the output, never fuel or inputs

### Requirement: Output push
A machine with items in an output slot SHALL push them into any adjacent inventory that accepts them, checking regularly, so a chest next to a machine fills without pipes.

#### Scenario: Chest beside the machine
- **WHEN** a machine produces an item and a chest is adjacent
- **THEN** within a second the item is in the chest and the output slot is empty

### Requirement: Breaking drops contents
Breaking a machine SHALL drop the machine block and every item in its slots.

#### Scenario: Pickaxe
- **WHEN** a player breaks a machine containing fuel and output
- **THEN** the machine item, the fuel and the output drop as items

### Requirement: Wrench rotates and dismantles
Using the wrench on a machine SHALL rotate it to the next horizontal facing. Sneaking with the wrench SHALL dismantle it: the machine and its contents drop as items without tool requirements.

#### Scenario: Rotate
- **WHEN** a player uses the wrench on a machine facing north
- **THEN** it faces east

#### Scenario: Dismantle
- **WHEN** a player sneaks and uses the wrench on a machine
- **THEN** the machine block is removed and it drops itself and its contents

### Requirement: Machine stats are data
Work time and other balance numbers of a machine SHALL be loaded from `data/<namespace>/machine/<machine>.json` and SHALL fall back to built in defaults when the file is missing.

#### Scenario: Retuned work time
- **WHEN** a datapack sets `work_ticks` of `burner_smelter` to 40 and the server reloads
- **THEN** the smelter finishes an ingot every 2 seconds instead of 4

### Requirement: Lit state
A machine SHALL show a lit front while it is working and an unlit front otherwise.

#### Scenario: Working
- **WHEN** a burner machine is burning fuel and processing
- **THEN** its front texture is the lit variant

### Requirement: Powered machines
A powered machine SHALL have an FE buffer that accepts energy through the energy capability on every side, SHALL spend its energy per tick only while it has work and enough energy, and SHALL pause with its progress kept when energy runs out.

#### Scenario: Enough energy
- **WHEN** a powered machine has work and at least its per tick cost in the buffer
- **THEN** it advances one tick of progress and the buffer drops by the per tick cost

#### Scenario: Out of energy
- **WHEN** the buffer holds less than the per tick cost
- **THEN** progress does not advance and is not reset

#### Scenario: Buffer never gives energy away
- **WHEN** a cable or generator tries to extract from a powered machine
- **THEN** it receives nothing

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
