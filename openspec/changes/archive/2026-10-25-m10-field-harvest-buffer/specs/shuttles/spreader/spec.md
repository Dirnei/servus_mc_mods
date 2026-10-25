## ADDED Requirements

### Requirement: Leftover goes to the head
When no plot needs a dose any more, fertilizer the Spreader still carries SHALL go back into the head's supply buffer up to its level, less the room a supply run reserved, and the rest into the head's To storage buffer (see `shuttles/field-head`); the Spreader SHALL NOT drive to racks. What neither buffer takes SHALL stay aboard: the Spreader SHALL park in its garage with it and unload it at the head once the buffer has room again. Then it SHALL go home.

#### Scenario: Leftover back in the buffer
- **WHEN** a Spreader took 3 bone meal for a wheat plot one dose short of ripe
- **THEN** the plot gets one dose and the other 2 bone meal go back into the head's supply buffer

#### Scenario: More than the supply buffer takes
- **WHEN** a Spreader comes back with 20 bone meal and the supply buffer has room for 5
- **THEN** 5 go into the supply buffer, 15 into the To storage buffer, and a plain shuttle later puts the 15 away

## REMOVED Requirements

### Requirement: Leftover goes back to a rack
**Reason**: Spreaders never ride a lift, so racks on another floor were out of reach; leftovers now go to the head's To storage buffer and plain shuttles put them away.
**Migration**: None; leftovers reach storage through the head.
