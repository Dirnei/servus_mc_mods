# Spec Delta

## MODIFIED Requirements

### Requirement: Machine like sides
The station SHALL expose its inventory through the item handler capability on every side: inputs accept insertion only, and only of items used by a configured recipe; outputs allow extraction only. Items in the outputs SHALL be pushed into adjacent inventories that accept them, checking regularly, so an Inbound Station beside the station collects them.

#### Scenario: Dock feeds and collects
- **WHEN** an Outbound Station faces the station with planks in its buffer, an Inbound Station touches the station and the sticks recipe is configured
- **THEN** the planks move into the station and the sticks end up in the Inbound Station's buffer

#### Scenario: Unused item refused
- **WHEN** a hopper offers cobblestone and no configured recipe uses it
- **THEN** the cobblestone is refused
