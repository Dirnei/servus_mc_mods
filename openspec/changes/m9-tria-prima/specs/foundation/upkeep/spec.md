# Spec Delta

## ADDED Requirements

### Requirement: Burner machines keep fuel as upkeep
The Burner Pump and the Coal Generator SHALL expose the upkeep capability for their fuel slot on every side: the upkeep item and level come from their machine stats, and the count is the number of that item in the fuel slot. Upkeep that arrives goes into the fuel slot.

#### Scenario: Generator fuel
- **WHEN** a Coal Generator with 5 coal in its fuel slot is asked for its upkeep
- **THEN** it answers coal, level 16 and count 5

### Requirement: Upkeep stats
Machine stats SHALL have `upkeep_item` (an item id, empty for none) and `upkeep_level` (a count, 0 for none). The defaults SHALL be coal and 16 for the Burner Pump and the Coal Generator and none for the other machines. A machine with no upkeep item or a level of 0 SHALL expose no upkeep.

#### Scenario: Retuned level
- **WHEN** a datapack sets `upkeep_level` of `servus_foundation:coal_generator` to 32 and the server reloads
- **THEN** the Coal Generator's fuel slot is kept at 32 coal

#### Scenario: Upkeep switched off
- **WHEN** a datapack sets `upkeep_level` of `servus_foundation:burner_pump` to 0
- **THEN** the Burner Pump exposes no upkeep and is no data member

## MODIFIED Requirements

### Requirement: Upkeep stays stocked
Every data network SHALL check its upkeep members at a fixed interval (example every 40 ticks) without scanning blocks. When a member holds less than its level and no earlier request for it is still open, the network SHALL ask the suppliers on the line, lowest position first, for the difference and remember the answer as the open request. Items of the upkeep item that the member gains count against the open request; the request SHALL end when it is complete or after a timeout (example 1200 ticks), so the next check asks again. Upkeep SHALL never be part of an order.

#### Scenario: Refill
- **WHEN** a Coal Generator keeping coal at 16 holds 4 coal and a warehouse with coal and an Access Point is on its line
- **THEN** the network asks for 12 coal and the generator ends up with 16 coal

#### Scenario: One request at a time
- **WHEN** 12 coal were asked for and only 6 have arrived
- **THEN** the next checks ask for nothing until the rest arrives or the request times out

#### Scenario: No supplier
- **WHEN** an upkeep member is on a line without any supplier
- **THEN** nothing is asked for and the member keeps what it holds
