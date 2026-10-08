# foundation/upkeep Specification

## Purpose
Upkeep keeps a machine's consumable slot, such as its fuel, stocked at a level from suppliers on the data line, independent of orders, so recipes stay pure and any block of any mod can ask for its upkeep the same way.

## Requirements

### Requirement: Upkeep capability
Foundation SHALL define a sided block capability `servus_foundation:upkeep`. A holder SHALL name its upkeep item (none when empty), the level to keep and how many of that item it holds right now, and SHALL be told when a refill of an amount was asked for. Any mod SHALL be able to register a holder for its blocks.

#### Scenario: Burner with coal
- **WHEN** a mod asks the upkeep capability of a machine keeping coal at 16 that holds 5 coal
- **THEN** it gets coal, level 16 and count 5

#### Scenario: No upkeep
- **WHEN** a mod asks the upkeep capability of a stone block
- **THEN** it gets nothing

### Requirement: Upkeep role
A block that exposes the upkeep capability on a side touching a duct's data line SHALL be a member of that data network with the role upkeep, besides any other role it has, and the duct SHALL show a data arm toward it.

#### Scenario: Coal Generator
- **WHEN** a duct with the data line touches a Coal Generator
- **THEN** the duct shows a data arm toward it and the generator is an upkeep member

### Requirement: Upkeep stays stocked
Every data network SHALL check its upkeep members at a fixed interval (example every 40 ticks) without scanning blocks. When a member holds less than its level and no earlier request for it is still open, the network SHALL ask the suppliers on the line, lowest position first, for the difference and remember the answer as the open request. Items of the upkeep item that the member gains count against the open request; the request SHALL end when it is complete or after a timeout (example 1200 ticks), so the next check asks again. Upkeep SHALL never be part of an order.

#### Scenario: Refill
- **WHEN** a Burner Smelter keeping coal at 16 holds 4 coal and a warehouse with coal and an Access Point is on its line
- **THEN** the network asks for 12 coal and the smelter ends up with 16 coal

#### Scenario: One request at a time
- **WHEN** 12 coal were asked for and only 6 have arrived
- **THEN** the next checks ask for nothing until the rest arrives or the request times out

#### Scenario: No supplier
- **WHEN** an upkeep member is on a line without any supplier
- **THEN** nothing is asked for and the member keeps what it holds

### Requirement: Upkeep bookkeeping is pure logic
The open requests per member (asked amount, arrivals counted from the holder's count, timeout) SHALL be kept by a pure Java class without Minecraft imports, covered by JUnit tests.

#### Scenario: Arrival completes a request
- **WHEN** 10 were asked at count 2 and a later check sees count 12
- **THEN** the request is complete and the next check may ask again
