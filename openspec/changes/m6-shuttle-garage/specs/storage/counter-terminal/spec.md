# Spec Delta

## REMOVED Requirements

### Requirement: Pull to cursor or inventory
**Reason**: The counter becomes a pickup desk: items are requested and collected from output slots, never pulled onto the cursor or into the inventory directly.
**Migration**: Click an item and confirm the amount, or shift click for one stack; take the delivery out of the output slots.

## ADDED Requirements

### Requirement: Request items
Clicking an entry SHALL open an amount prompt defaulting to one stack, with buttons for +1, +10 and +stack and a text field, and confirming SHALL create a request for that amount. Shift clicking an entry SHALL request one stack at once. A request SHALL be served by pick jobs: instantly for stock no shuttle can reach in crates and foreign inventories, by shuttle for stock a shuttle can reach, never instantly from a rack. A request whose stock sits only in racks no shuttle can reach, or that needs a shuttle while the terminal touches no rail, SHALL be refused with that reason.

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

### Requirement: Output slots
The terminal SHALL have `terminal_output_slots` output slots (example 9, from `warehouse.json`) shown in its GUI. Deliveries SHALL land there and the player SHALL take them out by hand or shift click. The output slots SHALL NOT be indexed as warehouse stock and SHALL NOT be reachable by hoppers or ducts. Breaking the terminal SHALL drop their contents.

#### Scenario: Collect
- **WHEN** a shuttle has delivered 64 cobblestone into the output slots
- **THEN** the player shift clicks the slot and the cobblestone is in the inventory

#### Scenario: Not stock
- **WHEN** 64 cobblestone sit in the output slots
- **THEN** the stock view does not count them

### Requirement: Open requests
The GUI SHALL list the terminal's open requests with the item, the delivered and requested amounts and the state: waiting for a shuttle, picking, waiting for room in the output slots, or missing stock. Clicking a request SHALL cancel it: jobs not yet started are dropped and loads already on a shuttle are still delivered. Requests are cleared when the warehouse reloads.

#### Scenario: Progress
- **WHEN** a request for 128 cobblestone has one load of 64 delivered and the second on its way
- **THEN** the list shows 64 of 128 and the state picking

#### Scenario: Cancel
- **WHEN** the player clicks a request that is waiting for a shuttle
- **THEN** it leaves the list and no shuttle is sent for it

## MODIFIED Requirements

### Requirement: Push into the warehouse
Clicking the grid with a carried stack SHALL store it in the warehouse; shift clicking an inventory slot SHALL store that slot. Pushes SHALL follow the zone routing but SHALL only go into crates and foreign inventories; racks are filled through a Inbound Station. Items that fit nowhere stay where they were.

#### Scenario: Shift click to store
- **WHEN** the player shift clicks 20 cobblestone in the inventory while an empty crate is on the network
- **THEN** the crate holds 20 cobblestone and the slot is empty

#### Scenario: Only racks have room
- **WHEN** the player pushes bread while every crate is full and a rack has room
- **THEN** the bread stays with the player

### Requirement: Offline shown
When the network is offline the GUI SHALL show the reason instead of the stock and SHALL refuse requests and pushes. Output slots SHALL stay usable so deliveries already made can be collected.

#### Scenario: No controller
- **WHEN** the terminal touches crates but no controller
- **THEN** the GUI says the warehouse has no controller

#### Scenario: Collect while offline
- **WHEN** the controller is broken after a delivery landed in the output slots
- **THEN** the player can still take the delivery out
