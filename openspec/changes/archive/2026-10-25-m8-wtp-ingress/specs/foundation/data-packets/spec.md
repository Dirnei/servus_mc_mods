## ADDED Requirements

### Requirement: Packet endpoint handshake
Foundation SHALL offer a packet endpoint, a block capability through which a target answers an offer of an item and a total before a delivery starts: with a grant of an amount and a ticket, room reserved for it, or with a refusal and a reason. A delivery to a target with an endpoint SHALL offer first, send at most the granted amount, and insert only as its grant. A refused delivery SHALL fail at once and take nothing from its source.

#### Scenario: Granted in part
- **WHEN** a move of 200 cobblestone goes to a target whose endpoint grants 128
- **THEN** the delivery moves 128 cobblestone, ends as done with 128 and the other 72 stay at the source

#### Scenario: Refused
- **WHEN** a move of 64 logs goes to a target whose endpoint refuses because it has no room
- **THEN** the delivery fails as refused with that reason and the source keeps all 64 logs

#### Scenario: No endpoint
- **WHEN** a move of 8 planks goes to a Crafting Station, which has no endpoint
- **THEN** the planks are delivered as before, as far as the station accepts them

#### Scenario: Reserved room is the grant's
- **WHEN** a grant reserved one slot of a target for 64 cobblestone and a second delivery of cobblestone reaches the same target
- **THEN** the second delivery cannot put anything into that slot

### Requirement: Grants end with their delivery
A grant SHALL end when its delivery ends, done, failed or settled, and its endpoint SHALL be told how many items arrived for it. An endpoint SHALL also end grants whose delivery no longer runs, checked at least once per second. A sender that already holds a grant from an endpoint SHALL be able to start a delivery with it instead of offering again.

#### Scenario: Delivery ends short
- **WHEN** a delivery granted 64 logs ends after 40 because its source was broken
- **THEN** the endpoint learns that 40 arrived and frees the room left for the other 24

#### Scenario: Lost delivery
- **WHEN** a grant's delivery disappears without ending, for example when its network is gone
- **THEN** the endpoint ends the grant within a second

## MODIFIED Requirements

### Requirement: Delivery state
Every delivery SHALL have an id and a state: running, done or failed with a reason. Starting a delivery between positions that no route connects SHALL fail at once with the reason no route and SHALL take nothing from the source. Starting a delivery to a target whose endpoint refuses SHALL fail at once with the reason refused and the endpoint's reason, and SHALL take nothing from the source. Callers SHALL be able to read the state by id and be told when it ends.

#### Scenario: No route
- **WHEN** a mod asks to move items to a member of a different data network
- **THEN** the delivery fails with no route and the source keeps its items

#### Scenario: Refused with a reason
- **WHEN** a mod asks to move logs to a Packing Station whose warehouse has no rack that takes logs
- **THEN** the delivery fails as refused, the reason says that no storage takes them, and the source keeps its logs
