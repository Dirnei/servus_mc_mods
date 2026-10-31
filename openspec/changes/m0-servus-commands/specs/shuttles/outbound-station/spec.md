## MODIFIED Requirements

### Requirement: Deliveries on request
The warehouse SHALL deliver a requested amount of an item into the station's buffer when asked by `/servus shuttles feed <warehouse> <outbound> <item> <count>`, planned like a terminal request and answering with the refusal reason when it cannot. Such deliveries SHALL come on top of the keep amounts. Work orders SHALL NOT deliver to Outbound Stations.

#### Scenario: Op feeds a station
- **WHEN** an op feeds 4 iron ingots to a station on the rails while a rack holds 10 and a garage holds a shuttle
- **THEN** a shuttle brings 4 ingots into the buffer
