# Spec Delta

## ADDED Requirements

### Requirement: Intake stops at a rule's cap
An online Inbound Station SHALL take an item through its Input and Pull sides only while the warehouse holds less than the cap of that item's stock rule. Held counts the indexed stock, every Inbound buffer, put-aways on shuttles and what open work orders still make for storage. An insert or pull SHALL take at most the difference to the cap; the rest stays with the sender. Items without a cap, and items put in through the station GUI, SHALL be taken as before.

#### Scenario: Hopper at the cap
- **WHEN** cobblestone has a cap of 4096, the warehouse holds 4090 and a hopper above the station offers 64 cobblestone
- **THEN** 6 cobblestone move into the buffer and the rest stays in the hopper

#### Scenario: Duct falls back
- **WHEN** cobblestone is at its cap and a duct carries cobblestone with the station at priority 1 and a Sink at priority 0 on the same channel
- **THEN** the station takes none and the duct delivers the cobblestone to the Sink

#### Scenario: Pull side at the cap
- **WHEN** the back side is Pull, a chest behind it holds 30 bread, bread has a cap of 100 and the warehouse holds 90
- **THEN** 10 bread move into the buffer and 20 stay in the chest

#### Scenario: Waiting stacks count
- **WHEN** cobblestone has a cap of 128, the warehouse holds 64 and two buffers hold 64 cobblestone waiting for a shuttle
- **THEN** the station takes no more cobblestone

#### Scenario: Open orders count
- **WHEN** sticks have a cap of 10, the warehouse holds none, an open work order still makes 8 sticks for storage and a duct pulls sticks from a chest into the station
- **THEN** 2 sticks move into the buffer and the rest stays in the chest

#### Scenario: Hand insert ignores the cap
- **WHEN** cobblestone is at its cap and the player shift clicks 20 cobblestone into the station GUI
- **THEN** the cobblestone is in the buffer and is put away

#### Scenario: Other items pass
- **WHEN** cobblestone is at its cap and a hopper offers raw iron, which has no rule
- **THEN** the raw iron moves into the buffer

#### Scenario: Offline station
- **WHEN** the station is offline and a hopper offers cobblestone that has a cap in another warehouse
- **THEN** the station takes the cobblestone as before and it waits in the buffer
