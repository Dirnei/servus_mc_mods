# Spec Delta

## MODIFIED Requirements

### Requirement: Breaking unforms and cancels
Removing any block of a formed CPU SHALL turn the other 26 back into unformed blocks and SHALL cancel the CPU's open orders: their reservations are released and items already delivered stay where they are. When the CPU's chunk unloads its orders SHALL pause instead and go on when it loads again. A master that loads unformed SHALL cancel the orders it saved.

#### Scenario: Break one block
- **WHEN** a formed CPU runs an order and a player breaks one of its blocks
- **THEN** the other blocks show as unformed and the order is cancelled with its reservations released

#### Scenario: Chunk unloads
- **WHEN** a formed CPU runs an order and its chunk unloads and loads again
- **THEN** the order is still open and finishes

## ADDED Requirements

### Requirement: Orders survive reloads
A CPU SHALL save its work orders with its master block: item, amount, the request or rule that made it, state and reason, reservations, and every step with its producer, recipe, batches, state, the inputs fed and the outputs collected. Order ids SHALL not repeat on that CPU after a reload. Shipments of inputs and outputs travelling back that were under way SHALL be booked by their saved ids once they end; an input shipment that ended short SHALL be shipped again as before.

#### Scenario: Restart mid step
- **WHEN** an order for 8 sticks has delivered the planks to a station and the server restarts before the sticks are collected
- **THEN** after the restart the order is still open, the sticks are collected and the order finishes
