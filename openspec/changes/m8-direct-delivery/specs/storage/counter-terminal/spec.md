# Spec Delta

## ADDED Requirements

### Requirement: Crafted items arrive directly
While a request's work order is open, the terminal SHALL NOT ship the request's item from stock; the crafted items arrive in the output slots from the producer and count as delivered, and the missing amount SHALL shrink as they land. After the order ends, whatever is still missing SHALL be shipped from stock as before. Output slots that are full SHALL make the packets wait, shown as waiting for room.

#### Scenario: No second shipment
- **WHEN** an order for a request delivers all 8 sticks straight to the terminal
- **THEN** the request is done and no sticks are shipped from storage for it

#### Scenario: Shortfall after the order
- **WHEN** an order ends having delivered 6 of 8 sticks and 2 sticks are in stock
- **THEN** the terminal ships the 2 sticks from stock
