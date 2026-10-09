# Proposal

## Why

Play test feedback: terminals, Crafting Stations and Pattern Storages should connect when they touch, like devices in Refined Storage, instead of each needing its own data duct. A wall of devices with one duct anywhere should be one data network.

This is a follow up within PLAN.md M8 (on-demand autocrafting); it changes how the M8 devices join the data line.

## What Changes

- Foundation gets data carriers: a block entity can carry the data network to touching data nodes like a piece of cable (`DataCarrier`). Carriers link to touching carriers and to duct data sides that are on. A carrier is a member of its network with the roles of its own capabilities, but never makes its other neighbours members.
- The duct summary counts only ducts as cables; carriers are not cables.
- Storage carriers: Counter Terminal, Production Terminal, Crafting Station, Pattern Storage, and every block of a formed Crafting CPU. An unformed CPU block carries nothing.
- Not carriers: Access Point, Machine Adapter, Warehouse Controller, other mods' blocks, chests.
- Packets route through carriers like through ducts.

## Capabilities

### New Capabilities
- `storage/data-carriers`: which Storage blocks carry the data network to touching blocks.

### Modified Capabilities
- `foundation/data-cable`: adds data carriers next to the duct data line.

## Non-goals

- Access Point, Machine Adapter and Warehouse Controller as carriers (they sit on rails or foreign machines and would join networks by surprise).
- Carriers that make touching chests or machines members.
- A per side on/off for carriers.

## Impact

- Foundation `data` package: `DataNode.dataCable`, `DataCarrier`, `DataMembers.self`, `DataNetwork` self membership and cable count.
- Storage: the five block entities join and leave the data network; the CPU joins when formed and leaves when unformed.
- GameTests in Storage for stacks without ducts, splits, chests and non-carriers.
