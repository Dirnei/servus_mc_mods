# Spec Delta

## ADDED Requirements

### Requirement: Warehouse as supplier
The Warehouse Controller and every Access Point SHALL expose the supplier capability. A request SHALL be shipped like any shipment to a data member: through the Access Point with the shortest shuttle trip, as packets. The answer SHALL be the amount planned, at most what the warehouse holds, and zero when the warehouse cannot ship.

#### Scenario: Upkeep from the warehouse
- **WHEN** a Machine Adapter on the line asks the controller's supplier for 16 coal and a crate holds 40
- **THEN** the answer is 16 and 16 coal arrive at the adapter as packets

#### Scenario: Out of stock
- **WHEN** the warehouse holds no coal
- **THEN** the supplier answers 0
