## ADDED Requirements

### Requirement: Export threshold
A stock rule SHALL have an optional Export above threshold, set and cleared in the Limit group of the Rule tab like the other thresholds. When the rule also has Fill up to set, Export above SHALL be greater than Fill up to; Save SHALL be refused with a message naming the two fields otherwise. The threshold SHALL persist with the rule across reloads and the carrot dismantle of the controller.

#### Scenario: Export saved
- **WHEN** the player sets Export above 2000 for wheat with no Make thresholds and presses Save
- **THEN** the wheat rule has an export above 2000

#### Scenario: Export not above Fill up to
- **WHEN** the player sets Fill up to 500 and Export above 400 for wheat and presses Save
- **THEN** the rule is not saved and the Rule tab says Export above must be over Fill up to

#### Scenario: Export cleared
- **WHEN** the player steps Export above of the wheat rule to off and presses Save
- **THEN** the wheat rule has no export and the station that held it shows an empty keep slot

### Requirement: An export has at most one station
An export SHALL be held by at most one Outbound Station of the rule's warehouse, together with the keep slot it occupies. A new export SHALL have no station. Claiming an export at a station SHALL take it from the station that held it. An export SHALL lose its station when that station is broken, dismantled, or no longer part of the warehouse, and when the export threshold is removed from the rule or the rule is removed. A station that is only unloaded SHALL keep its exports.

#### Scenario: New export
- **WHEN** an export above 2000 is saved for wheat
- **THEN** the wheat rule has no station

#### Scenario: Station broken
- **WHEN** Outbound #3 holds the wheat export and is broken
- **THEN** the wheat rule has no station

#### Scenario: Rail removed
- **WHEN** Outbound #3 holds the wheat export and the rail that linked it to the warehouse is broken
- **THEN** the wheat rule has no station and placing the rail again does not give it back

#### Scenario: Unloaded station
- **WHEN** the chunk of Outbound #3 unloads while the controller stays loaded
- **THEN** the wheat rule still names Outbound #3

### Requirement: Export state
A rule with an export SHALL show an export state beside its Make state: "no station" while no station holds it, "exporting" while surplus is planned or on its way to its station, "station full" while surplus exists and the station's buffer has no free unlocked slot, and "ok" otherwise.

#### Scenario: Unassigned state
- **WHEN** the wheat rule exports above 2000 and no station holds it
- **THEN** its export state reads no station

#### Scenario: Assigned and idle state
- **WHEN** Compost Dock (#3) holds the wheat export and the warehouse holds 1500 wheat
- **THEN** its export state reads ok

#### Scenario: Station full
- **WHEN** the warehouse holds 3000 wheat, the export is above 2000 and every buffer slot of the station holds wheat nobody takes
- **THEN** the export state reads station full

### Requirement: Export columns in the Rules tab
The Rules tab SHALL show each rule's export threshold in an Export column, a dash for a rule without an export, and in its Station column the label of the station that holds the export or "no station".

#### Scenario: Unassigned
- **WHEN** the wheat rule exports above 2000 and no station holds it
- **THEN** the Rules tab shows 2,000 under Export and "no station" under Station

#### Scenario: Assigned
- **WHEN** Compost Dock (#3) holds the wheat export
- **THEN** the Station column reads Compost Dock (#3)

### Requirement: Export line in the Rule tab
The Rule tab SHALL show a line "Export goes to" followed by the label of the station that holds the export, or "No station" with a hint to claim the export at an Outbound Station while none holds it.

#### Scenario: No station holds it
- **WHEN** the Rule tab shows the wheat rule exporting above 2000 and no station holds the export
- **THEN** the Rule tab says no station holds the export and to claim it at an Outbound Station

#### Scenario: Export goes to a station
- **WHEN** Compost Dock (#3) holds the wheat export
- **THEN** the Rule tab says Export goes to Compost Dock (#3)

### Requirement: Export in the grid tooltip
The terminal grid tooltip of an item whose rule has an export SHALL include the export threshold and the export state.

#### Scenario: Tooltip
- **WHEN** the player hovers wheat in the stock grid and its rule exports above 2000 with no station
- **THEN** the tooltip names export above 2000 and no station
