## ADDED Requirements

### Requirement: Station sides pull and push
The station SHALL register no item capability, so hoppers, item pipes and machines can neither insert into nor extract from it and pipes show no item arm toward it. Packets SHALL feed its inputs and collect its outputs through its packet inlet, which takes only items the current job's pattern uses and nothing while the station has no job with crafts left. Every side but the front SHALL be Pull, Push or Off, all Push by default; the front is fixed to Off, shown in the Lib side panel and cycled by the carrot. While the station has no job, items in the outputs SHALL be pushed into adjacent inventories through Push sides, checking regularly; while it holds a job it SHALL NOT push, because the CPU collects the job's outputs. Items the job uses SHALL be pulled from neighbours through Pull sides.

#### Scenario: Packets feed the job
- **WHEN** the station has a sticks job and packets of planks arrive over a data duct
- **THEN** the planks go into the inputs

#### Scenario: No job refuses packets
- **WHEN** the station has no job and packets of planks are sent to it
- **THEN** the planks stay at their source

#### Scenario: Unused item refused
- **WHEN** the station has a sticks job and packets of cobblestone are sent to it
- **THEN** the cobblestone stays at its source

#### Scenario: Hopper above the station
- **WHEN** the station has a sticks job and a hopper above it offers planks
- **THEN** the planks stay in the hopper

#### Scenario: Chest feeds and takes leftovers
- **WHEN** the station has a sticks job, its top side is Pull with a chest of planks above it, its back is Push against an empty chest, and the order is cancelled after the first craft
- **THEN** the planks move into the station, the sticks stay in its outputs while the job is held, and they end up in the back chest once the job ends

#### Scenario: Job outputs stay for the CPU
- **WHEN** the station holds a sticks job and has crafted sticks
- **THEN** the sticks stay in the outputs until the CPU collects them as packets, and the order finishes

#### Scenario: Item pipe beside the station
- **WHEN** a duct with the Item Pipe and the data line is placed beside the station
- **THEN** the duct shows a data arm but no item arm toward the station

#### Scenario: Front closed
- **WHEN** the player uses the carrot on the front
- **THEN** the front stays Off

## REMOVED Requirements

### Requirement: Machine like sides
**Reason**: The station registers no item capability any more, so the passive modes (Auto, Input, Output, Both) that let hoppers and pipes reach it have no effect.
**Migration**: See "Station sides pull and push": packets feed and collect jobs; Pull and Push sides move items to and from neighbouring inventories.
