## ADDED Requirements

### Requirement: Stalled deliveries end
A delivery to a target without a packet endpoint that has moved nothing for `stall_ticks` (example 600, datapack JSON next to `retry_interval`) SHALL end as failed with the reason stalled. Items it has not delivered SHALL stay at their source, and it SHALL report what it delivered. A delivery with a grant SHALL never stall, because it only sends into reserved room.

#### Scenario: Machine stays full
- **WHEN** a move of 64 planks reaches a foreign machine through a Machine Adapter and the machine takes 10, then nothing for 600 ticks
- **THEN** the delivery ends as stalled, reports 10 and the other 54 planks are still at the source

#### Scenario: Room frees in time
- **WHEN** the machine takes nothing for 300 ticks and then takes the rest
- **THEN** the delivery goes on and ends as done

### Requirement: Held grants expire
A sender SHALL be able to hold a grant before its delivery starts, for example while shuttles bring the items. A grant that no delivery has used for `grant_hold_ticks` (example 1200, datapack JSON next to `retry_interval`) SHALL end, its endpoint freeing the room, and the sender SHALL offer again when it is ready.

#### Scenario: Shuttle never comes
- **WHEN** a Packing Station holds a grant of 64 copper at a terminal and no delivery uses it for 1200 ticks
- **THEN** the grant ends, the terminal's room is free again, and the station offers again once the copper has arrived
