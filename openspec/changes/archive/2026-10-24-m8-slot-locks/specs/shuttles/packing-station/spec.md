## MODIFIED Requirements

### Requirement: Shortest shuttle trip
When several Packing Stations of a warehouse reach the member, the warehouse SHALL ship through the one with the cheapest shuttle trip by travel cost (see `shuttles/travel-cost`) from the racks holding the item; on a tie, and when the items need no shuttle, any of them.

#### Scenario: Two Packing Stations
- **WHEN** a rack with planks is 2 rails away from Packing Station A and 8 rails away from Packing Station B, both on the data line to the station
- **THEN** the shipment goes through Packing Station A
