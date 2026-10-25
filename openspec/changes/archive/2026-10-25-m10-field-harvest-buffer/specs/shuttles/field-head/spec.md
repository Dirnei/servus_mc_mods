## ADDED Requirements

### Requirement: To storage buffer
Each Field Head SHALL keep a To storage buffer of `harvest_buffer_slots` slots (in `field.json`, example 9) for items leaving its field. Only the field's Harvester and Spreader SHALL put items in, and only plain shuttles SHALL take them out for put-away; players, hoppers and pipes SHALL do neither. Its slots SHALL take slot locks. The buffer and its locks SHALL be saved, and its items SHALL drop when the head is broken.

#### Scenario: Buffer survives a reload
- **WHEN** a head holds 64 wheat and 12 seeds in its To storage buffer and the server restarts
- **THEN** after the restart the buffer holds the same items

#### Scenario: Broken head drops it
- **WHEN** a player breaks a head with 64 wheat in its To storage buffer
- **THEN** the wheat drops with the head

#### Scenario: Not for players
- **WHEN** a hopper sits below a Field Head with wheat in its To storage buffer
- **THEN** the hopper takes nothing

### Requirement: To storage buffer is put away
The warehouse SHALL put the To storage buffer away like the buffer of an Inbound Station (see `shuttles/inbound-station`): by the put-away rule and zones, locking each stack it plans, with plain shuttles that fetch at the farm rail beside the head and may ride lifts to the rack. When no plain shuttle can reach the head or no rack has room, the items SHALL stay in the buffer and the field work status SHALL say why.

#### Scenario: Racks upstairs
- **WHEN** a field and its garage lie in the basement, the rack zoned for wheat stands one floor up, and a plain shuttle of the warehouse can ride the shaft
- **THEN** the harvester unloads its wheat at the head and a plain shuttle brings it to the zoned rack upstairs

#### Scenario: No plain shuttle
- **WHEN** the warehouse has only Harvester shuttles and the To storage buffer holds wheat
- **THEN** the wheat stays in the buffer and the field work status says that it waits for a shuttle to collect the harvest

### Requirement: To storage row in the GUI
The Field Head GUI SHALL show the To storage buffer as a labelled read-only row of slots with a tooltip saying that the items wait for a shuttle to put them away. Clicking its slots SHALL neither take nor put items.

#### Scenario: Look but do not touch
- **WHEN** the To storage row holds 64 wheat and the player clicks the slot
- **THEN** the wheat stays in the slot and nothing lands on the cursor
