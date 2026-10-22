# foundation/machines Specification

## Purpose
Rules every Servus machine follows, in Foundation and in every mod built on it, so players and other mods can rely on the same handling for facing, inventories, GUIs, output and the wrench.

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
A machine SHALL expose its inventory through the item handler capability on every side but the front, which is fixed to Off and exposes nothing. Input and fuel slots accept insertion only, output slots allow extraction only, and automation SHALL never extract inputs or insert into outputs.

#### Scenario: Hopper feeds fuel
- **WHEN** a hopper above a burner machine holds coal
- **THEN** the coal moves into the machine's fuel slot

#### Scenario: Hopper pulls output
- **WHEN** a hopper below a machine with an output item runs
- **THEN** it extracts only the output, never fuel or inputs

#### Scenario: Front is closed
- **WHEN** a duct or hopper offers coal to the front of a burner machine
- **THEN** nothing goes in, nothing comes out, and the front cannot be set to another mode

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
Using the wrench on a machine face SHALL cycle that side's mode and show side and mode in the action bar. Sneaking with the wrench SHALL dismantle it: the machine and its contents drop as items without tool requirements, the dropped machine keeping its configuration. The wrench SHALL NOT rotate machines; the facing is set at placement.

#### Scenario: Rotate
- **WHEN** a player uses the wrench on the left face of a machine in Auto
- **THEN** the machine still faces the same way and its left side is Input

#### Scenario: Dismantle
- **WHEN** a player sneaks and uses the wrench on a machine
- **THEN** the machine block is removed and it drops itself and its contents

### Requirement: Machine stats are data
Work time and other balance numbers of a machine SHALL be loaded from `data/<namespace>/machine/<machine>.json` and SHALL fall back to built in defaults when the file is missing.

#### Scenario: Retuned work time
- **WHEN** a datapack sets `work_ticks` of `servus_foundation:burner_pump` to 10 and the server reloads
- **THEN** the Burner Pump finishes a work cycle every half second instead of every second

### Requirement: Lit state
A machine SHALL show a lit front while it is working and an unlit front otherwise. A burner machine whose burn is paused because it has no work SHALL show as not lit, and its lit state SHALL change only when it starts or stops working, not while it keeps working or keeps waiting.

#### Scenario: Working
- **WHEN** a burner machine is burning fuel and processing
- **THEN** its front texture is the lit variant

#### Scenario: Paused burn
- **WHEN** a burner machine with a burn in progress has no work
- **THEN** its front texture is the unlit variant and its GUI shows no flame

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
Every machine GUI SHALL show the Foundation side panel with its six relative sides. Clicking a side cycles its mode through Auto, Input, Output, Both, Pull, Push and Off, and the panel SHALL show the current mode by colour and tooltip. The configuration SHALL apply at once and persist with the machine.

#### Scenario: Set top to Input
- **WHEN** the player clicks the top cell until it shows Input
- **THEN** a hopper above the machine can insert, nothing is pushed upward, and the setting survives closing the GUI and reloading the world

#### Scenario: Pull from a chest
- **WHEN** the player sets the left side of a Burner Pump to Pull and a chest on the left holds coal and cobblestone
- **THEN** the coal moves into the fuel slot within a few ticks and the cobblestone stays in the chest

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

### Requirement: Dismantling keeps the configuration
A machine dismantled with the sneak wrench SHALL drop as an item that restores its side configuration, selected recipe and owner when placed again. A machine broken with a tool drops a fresh machine.

#### Scenario: Move a configured machine
- **WHEN** the player sneak wrenches a machine with sides configured and a recipe selected and places it elsewhere
- **THEN** the new machine has the same sides and recipe

### Requirement: Burner machines bank their fire
A burner machine SHALL count its burn down only on ticks it works. While it has no work, a burn in progress SHALL pause with its remaining time kept, so no fuel is wasted; when work comes back, the paused burn SHALL go on before a new fuel item is consumed. A new fuel item SHALL be consumed only when the previous burn has ended and work is available. The remaining burn SHALL be kept when the world is saved and loaded. A machine with an eternal fire SHALL work without fuel as before.

#### Scenario: No work pauses the burn
- **WHEN** a burner machine that has just lit a coal loses its work, for example because its output is full
- **THEN** its remaining burn time stays the same for as long as it has no work, and no new fuel is taken

#### Scenario: Work again
- **WHEN** the paused machine gets work again
- **THEN** it works on the paused burn and takes the next fuel item only when that burn has ended

### Requirement: Restart threshold
A machine with a buffer it fills (an FE buffer or a water tank) MAY have a restart threshold `restart_below` in its machine stats, a fraction of the buffer's capacity between 0 and 1. Once its buffer is full, which means it has no room left for one more work cycle (one tick of FE, one `fluid_per_cycle` of water), such a machine SHALL stay off until the buffer holds less than `restart_below` times its capacity, and SHALL then run until the buffer is full again. With `restart_below` 1, or without the field on a machine that has no threshold of its own, the machine SHALL run again as soon as the buffer has room. The waiting state SHALL be kept when the world is saved and loaded.

#### Scenario: Waits after full
- **WHEN** a machine with `restart_below` 0.5 fills its buffer and a consumer then takes a little
- **THEN** the machine stays off while the buffer holds half or more

#### Scenario: Restarts below the threshold
- **WHEN** the buffer of that machine drops below half
- **THEN** the machine runs again and keeps running until the buffer is full

#### Scenario: Waiting is visible
- **WHEN** a machine with a restart threshold waits with its buffer above the threshold
- **THEN** it is not lit, and its GUI tells that it waits and below which amount it restarts

#### Scenario: Retuned threshold
- **WHEN** a datapack sets `restart_below` of `servus_foundation:coal_generator` to 0.9 and the server reloads
- **THEN** the generator restarts once its buffer drops below 90 percent
