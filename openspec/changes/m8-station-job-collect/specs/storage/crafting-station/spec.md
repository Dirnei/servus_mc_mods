# Spec Delta

## MODIFIED Requirements

### Requirement: Machine like sides
The station SHALL have a Lib side configuration like a machine: Auto, Input, Output, Both, Pull, Push and Off, all sides Auto by default except the front, which is fixed to Off, shown in the Lib side panel and cycled by the carrot. Through sides that allow insertion the inputs accept only items the current job's pattern uses, and nothing while the station has no job with crafts left; through sides that allow extraction the outputs can be taken. While the station has no job, items in the outputs SHALL be pushed into adjacent inventories through Auto and Push sides, checking regularly; while it holds a job it SHALL NOT push, because the CPU collects the job's outputs. Items the job uses SHALL be pulled from neighbours through Pull sides.

#### Scenario: Packets feed the job
- **WHEN** the station has a sticks job and packets of planks arrive over a data duct on an Auto side
- **THEN** the planks go into the inputs

#### Scenario: No job refuses
- **WHEN** the station has no job and a hopper above offers planks
- **THEN** the planks stay in the hopper

#### Scenario: Unused item refused
- **WHEN** the station has a sticks job and a hopper offers cobblestone
- **THEN** the cobblestone is refused

#### Scenario: Side off
- **WHEN** the top side is Off, the station has a sticks job and a hopper above offers planks
- **THEN** the planks stay in the hopper

#### Scenario: Dock feeds and collects
- **WHEN** the station has a sticks job, an Outbound Station faces it with planks in its buffer and an Inbound Station touches it, and the job ends after its crafts
- **THEN** the planks move into the station, the sticks stay in its outputs while the job is held, and they end up in the Inbound Station's buffer once the job ends

#### Scenario: Job outputs stay for the CPU
- **WHEN** the station holds a sticks job, has crafted sticks and an Access Point touches it on an Auto side
- **THEN** the sticks stay in the outputs until the CPU collects them as packets, and the order finishes

#### Scenario: Front closed
- **WHEN** the station has a sticks job and an Outbound Station pushes planks against the station's front
- **THEN** the planks stay in the outbound buffer
