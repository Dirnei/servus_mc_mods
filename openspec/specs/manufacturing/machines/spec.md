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
