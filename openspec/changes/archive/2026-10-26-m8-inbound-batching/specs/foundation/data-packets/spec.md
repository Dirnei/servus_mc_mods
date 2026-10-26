# Spec Delta

## MODIFIED Requirements

### Requirement: Held grants expire
A sender SHALL be able to hold a grant before its delivery starts, for example while shuttles bring the items, and a sender that is not a data line delivery, such as an Item Pipe, SHALL be able to fill a grant itself and end it when done. A grant that no delivery has used and that took no items for `grant_hold_ticks` (example 1200, datapack JSON next to `retry_interval`) SHALL end, its endpoint freeing the room, and the sender SHALL offer again when it is ready.

#### Scenario: Shuttle never comes
- **WHEN** a Packing Station holds a grant of 64 copper at a terminal and no delivery uses it for 1200 ticks
- **THEN** the grant ends, the terminal's room is free again, and the station offers again once the copper has arrived

#### Scenario: Pipe keeps its grant while items arrive
- **WHEN** an Item Pipe fills a grant of 64 cobblestone at an Inbound Station with 8 items every 8 ticks
- **THEN** the grant stays open until the pipe has sent all 64 or ends it

#### Scenario: Pipe lost without ending its grant
- **WHEN** an Item Pipe's grant takes no items for 1200 ticks, for example because its network is gone without ending it
- **THEN** the grant ends and the station frees its slot
