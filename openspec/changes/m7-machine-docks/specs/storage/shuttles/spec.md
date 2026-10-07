# Spec Delta

## MODIFIED Requirements

### Requirement: Delivery
A shuttle SHALL deliver its cargo into the delivery buffer of the block the job names: the output slots of a Counter Terminal for a request, the feed buffer of a Machine Dock for a feed. It drives to the rail touching that block. When the buffer has no room the shuttle SHALL wait beside it with its cargo until there is room. When the block is gone the cargo SHALL drop where it was. A destination touching no rail SHALL NOT take shuttle deliveries, and requests for it SHALL be served instantly or refused.

#### Scenario: Player left
- **WHEN** the requesting player logs out before the shuttle arrives
- **THEN** the stack still lands in the terminal's output slots and waits there

#### Scenario: Output full
- **WHEN** the output slots are full when the shuttle arrives
- **THEN** the cart waits beside the terminal and unloads as soon as the player takes something out

#### Scenario: Terminal broken
- **WHEN** the terminal is broken while a shuttle carries its order
- **THEN** the stack drops where the terminal stood

#### Scenario: Terminal off the rails
- **WHEN** a terminal touches no rail and the player requests an item that only sits in a rack
- **THEN** the request is refused with the reason that the terminal has no rail

#### Scenario: Feed to a dock
- **WHEN** a feed of 4 ingots is made for a dock on the rail
- **THEN** the shuttle unloads the ingots into the dock's feed buffer
