# Spec Delta

## MODIFIED Requirements

### Requirement: Deliveries on request
The warehouse SHALL deliver a requested amount of an item into the station's buffer when asked by `/servus_warehouse feed <station> <item> <count>`, planned like a terminal request and answering with the refusal reason when it cannot. Such deliveries SHALL come on top of the keep amounts. Work orders SHALL NOT deliver to Outbound Stations.

#### Scenario: Op feeds a station
- **WHEN** an op feeds 4 iron ingots to a station while a crate touching the controller holds 10
- **THEN** 4 ingots are in the buffer at once

## REMOVED Requirements

### Requirement: Station reports its producer
**Reason**: Producers are found on the data line by the Crafting CPU; an Outbound Station is a plain keep slot buffer.
**Migration**: `/servus_warehouse producers <pos>` lists the producers of the data line (see `storage/crafting-cpu`).
