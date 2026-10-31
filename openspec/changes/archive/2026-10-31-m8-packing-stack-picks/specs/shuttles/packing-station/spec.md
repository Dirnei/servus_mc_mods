# Spec Delta

## ADDED Requirements

### Requirement: Picks of one item share To network slots
Picks that bring an item into a Packing Station's To network buffer SHALL join a stack of that item with room, counting what picks on their way already bring there, before they use empty slots. Many small picks of one item SHALL fill as few slots as their total needs, so the buffer counts as full only when its room is used, not when every slot holds a little.

#### Scenario: Small coal picks
- **WHEN** a shipment of 14 coal to a Machine Adapter is picked in six picks of 1 to 3 coal into an empty To network buffer of 9 slots
- **THEN** all 14 coal land in one slot and the other 8 slots stay free for other items

#### Scenario: Another item still finds room
- **WHEN** the To network buffer holds those 14 coal in one slot and a shipment of 16 iron ingots follows
- **THEN** the iron ingots are picked into a free slot without waiting

#### Scenario: Stack of one item overflows
- **WHEN** a To network slot holds 64 coal and a pick of 8 coal is sent
- **THEN** the 8 coal take an empty slot

#### Scenario: Pick larger than the room on a stack
- **WHEN** a To network slot holds 60 coal and a pick of 10 coal is sent
- **THEN** the pick fills that stack to 64 and its other 6 coal take an empty slot

### Requirement: Picks never ask for more than fits in the To network buffer
A pick SHALL be sent into a Packing Station's To network buffer only once room for its whole amount is reserved there, counting what picks already on their way reserved, so the items in the buffer and on their way to it SHALL never be more than it holds. A pick that does not fit SHALL wait unsent, reserving nothing, until room frees. Shipment demand or a target's grant SHALL never let a pick through without that reservation.

#### Scenario: Shipments want more than fits
- **WHEN** the To network buffer has room for 20 coal, spread over a partial stack of coal and empty slots, and open shipments want 50 coal in picks of 10
- **THEN** two picks are sent and reserve the 20, the other three wait, and each is sent once the station has sent enough coal to free room for it

#### Scenario: Picks on their way take the room
- **WHEN** the To network buffer has room for 20 coal and a pick of 12 coal is on its way into it, and a pick of 10 coal is next
- **THEN** the pick of 10 waits, because only 8 are left, and is sent once room for 10 is free

#### Scenario: A pick bigger than the room waits whole
- **WHEN** the To network buffer has room for 20 coal and the next pick carries 32 coal
- **THEN** the pick is not sent and reserves nothing until room for all 32 is free

### Requirement: The station sends from slots with picks on their way
A Packing Station SHALL send the items of its To network buffer to their shipments' targets also from slots where more picks of that item are still on their way, and those picks SHALL still find their room there when they arrive.

#### Scenario: Send while more coal comes
- **WHEN** a To network slot holds 6 coal of a granted shipment and a pick of 4 more coal for the same shipment is on its way into that slot
- **THEN** the station sends the 6 coal at its rate, and the 4 coal land in the slot when the pick arrives and are sent after them
