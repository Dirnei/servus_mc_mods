## ADDED Requirements

### Requirement: Claim an export picker
Left clicking an empty keep slot of an online station with an empty hand SHALL open a Claim an export picker over the station GUI. Clicking an empty keep slot while carrying an item SHALL set a keep amount as before and SHALL NOT open the picker.

#### Scenario: Open the picker
- **WHEN** the player clicks an empty keep slot of Outbound #3 with an empty hand
- **THEN** the Claim an export picker opens over the station GUI

#### Scenario: Carrying an item
- **WHEN** the player clicks an empty keep slot carrying 32 iron ingots
- **THEN** the slot keeps 32 iron ingots and no picker opens

### Requirement: Picker lists
The picker SHALL list the exports of the station's warehouse that no station holds, each with its item and threshold, and below them the exports held by other stations of that warehouse, each with the holding station's label. Exports held by this station SHALL NOT be listed. With no export to list the picker SHALL say so.

#### Scenario: Unassigned and held elsewhere
- **WHEN** the wheat export has no station and Smoker Line (#1) holds the kelp export, and the player opens the picker at Outbound #3
- **THEN** wheat is listed with its threshold under the exports without a station and kelp is listed below with Smoker Line (#1)

#### Scenario: Nothing to claim
- **WHEN** every export of the warehouse is held by this station
- **THEN** the picker says there is no export to claim

### Requirement: Picker search
A search box SHALL filter both lists of the picker by item name or station label as the player types.

#### Scenario: Search
- **WHEN** the picker lists rotten flesh, wheat seeds and kelp at Smoker Line (#1) and the player types "smoker"
- **THEN** only kelp is listed

### Requirement: Claiming an export
Picking an entry SHALL assign that export to this station and that slot and close the picker. For an export held elsewhere it SHALL also free the slot at the other station.

#### Scenario: Claim an unassigned export
- **WHEN** the wheat rule exports above 2000 with no station and the player clicks the third empty keep slot of Outbound #3 with an empty hand and picks wheat
- **THEN** the third keep slot shows wheat with the export marker and the wheat rule names Outbound #3

#### Scenario: Move from another station
- **WHEN** Smoker Line (#1) holds the kelp export and the player claims kelp at Outbound #3
- **THEN** Outbound #3 holds the kelp export and the keep slot at Smoker Line (#1) is empty

### Requirement: Export slots
A keep slot holding an export SHALL show the item with an export marker and the threshold, and its tooltip SHALL name the rule's threshold and export state. The export slot SHALL NOT keep anything stocked in the buffer.

#### Scenario: Marker and tooltip
- **WHEN** Outbound #3 holds the wheat export above 2000 in its third keep slot and the player hovers it
- **THEN** the slot shows wheat with the export marker and the tooltip names the threshold 2000 and the export state

### Requirement: Export threshold is read only at the station
The threshold of an export slot SHALL be edited only on the rule. Scrolling over an export slot SHALL change nothing and clicking it while carrying an item SHALL change nothing.

#### Scenario: Read only threshold
- **WHEN** the player scrolls over the wheat export slot
- **THEN** the threshold still reads the rule's 2000

#### Scenario: Click with an item
- **WHEN** the player clicks the wheat export slot carrying 32 iron ingots
- **THEN** the slot still holds the wheat export and the player still carries the iron ingots

### Requirement: Releasing an export at the station
Right clicking an export slot with an empty hand SHALL release the export, leaving the rule without a station and the slot empty.

#### Scenario: Release at the station
- **WHEN** the player right clicks the wheat export slot with an empty hand
- **THEN** the slot is empty and the wheat rule shows no station

### Requirement: Exports are delivered by shuttle
Every `outbound_check_interval` ticks an online warehouse SHALL, for each export a station holds, compare the indexed count of the item with the threshold and request the surplus not yet on its way into that station's buffer through the normal delivery path, by shuttle from racks a shuttle can reach, limited by the buffer's free unlocked slots like a keep check. Surplus that cannot be planned at a check SHALL be requested again at the next one.

#### Scenario: Surplus shipped
- **WHEN** Outbound #3 holds the wheat export above 2000, the racks hold 2300 wheat and a garage holds a shuttle
- **THEN** shuttles bring 300 wheat into the station's buffer and 2000 remain in the racks

#### Scenario: Below the threshold
- **WHEN** the racks hold 1800 wheat
- **THEN** no wheat is shipped to the station

#### Scenario: Not asked twice
- **WHEN** 300 wheat surplus is on its way to the station and the next check runs
- **THEN** no further wheat is requested for those 300

### Requirement: Delivered surplus leaves the station
The station SHALL push delivered surplus out of its Push sides and offer it on its Output sides like any buffer stack.

#### Scenario: Pushed into a Sink
- **WHEN** the station's back faces a Sink and wheat surplus arrives
- **THEN** the wheat moves on into the Sink and the next check ships more surplus if the racks hold any

### Requirement: Export lamp and state line
While surplus waits and the buffer has no free unlocked slot, the activity lamp SHALL be red and the GUI state line SHALL name the item and say the station is full. While surplus is on its way the lamp SHALL be orange.

#### Scenario: Nothing takes it
- **WHEN** the station has no Push side and every buffer slot holds wheat while the racks still hold 3000
- **THEN** no more wheat is planned, the lamp is red and the GUI says the station is full of wheat

#### Scenario: On its way
- **WHEN** a shuttle is bringing wheat surplus to the station
- **THEN** the lamp is orange
