# Spec Delta

## MODIFIED Requirements

### Requirement: Request items
Clicking an entry SHALL open an amount prompt defaulting to one stack, with buttons for +1, +10 and +stack and a text field, and confirming SHALL create a request for that amount. Shift clicking an entry SHALL request one stack at once. A request SHALL be served by pick jobs: instantly for stock no shuttle can reach in crates and foreign inventories, by shuttle for stock a shuttle can reach, never instantly from a rack. A request whose stock sits only in racks no shuttle can reach, or that needs a shuttle while the terminal touches no rail, SHALL be refused with that reason. When stock falls short and an Outbound Station's producer can make the item, the shortfall SHALL become a work order linked to the request.

#### Scenario: Request a stack
- **WHEN** the player shift clicks cobblestone, the warehouse holds 50 in a crate touching the controller and has no garage
- **THEN** 50 cobblestone appear in the output slots at once

#### Scenario: Request an amount
- **WHEN** the player clicks iron ingots, presses +10 three times and confirms, and the ingots sit in a crate beside a rail with a shuttle available
- **THEN** a request for 94 is listed, a shuttle fetches two loads, and 94 ingots land in the output slots over time

#### Scenario: Rack needs a shuttle
- **WHEN** the player requests bread that only sits in a rack and the warehouse has no garage
- **THEN** the request is refused and the GUI says the item needs a shuttle

#### Scenario: Partly in stock
- **WHEN** the player requests 100 bread and the warehouse holds 70
- **THEN** 70 are delivered and the request shows 30 missing until more arrives or it is cancelled

#### Scenario: Shortage ordered
- **WHEN** the player requests 8 sticks, none are in stock, and an Outbound Station faces a Crafting Station with the sticks recipe
- **THEN** the request is listed as producing and the sticks arrive in the output slots once crafted
