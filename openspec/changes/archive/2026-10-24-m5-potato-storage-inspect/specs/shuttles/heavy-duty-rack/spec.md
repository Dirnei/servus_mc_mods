# Spec Delta

## ADDED Requirements

### Requirement: Rack section inspect
Using the Data Potato on any face of a Heavy Duty Rack SHALL print a chat report for the whole section the bay belongs to: the zone names of its bays (or that it has no zone), the number of bays, the slots in use (holding items or locked by a job) against all slots of the section, a full mark when no slot is free, and the items it holds most (example: the top 5) with their total counts, largest first. An empty section SHALL say it is empty. It SHALL open no GUI and change nothing.

#### Scenario: Section report
- **WHEN** a section of three bays of 27 slots, stamped "Ores", holds 40 iron ore in 1 slot and 1,800 cobblestone in 29 slots, and a player uses the potato on its middle bay
- **THEN** the chat shows zone "Ores", 3 bays, 30 of 81 slots, then cobblestone 1,800 and iron ore 40

#### Scenario: Locked slot counts as used
- **WHEN** a single bay holds items in 10 slots and a put-away has locked one more empty slot
- **THEN** the slot line shows 11 of 27 slots

#### Scenario: Full section
- **WHEN** every slot of a single bay holds items and a player uses the potato on it
- **THEN** the slot line shows 27 of 27 slots and marks the section as full

#### Scenario: Empty and unzoned
- **WHEN** a player uses the potato on an empty bay without a zone
- **THEN** the chat shows no zone, 1 bay, 0 of 27 slots and that it is empty

#### Scenario: Nothing opens
- **WHEN** a player uses the potato on the front of a rack
- **THEN** the report is printed, no GUI opens and the rack's items are unchanged

### Requirement: Top items tally is pure
Counting the slots of a section in use and adding up its stacks into totals per item, picking the items held most, SHALL be plain Java without Minecraft classes and covered by unit tests.

#### Scenario: Unit tested
- **WHEN** the unit tests run
- **THEN** they cover totals over several slots of one item, the order largest first, ties in slot order, the limit on how many items are listed, and a locked empty slot counting as used
