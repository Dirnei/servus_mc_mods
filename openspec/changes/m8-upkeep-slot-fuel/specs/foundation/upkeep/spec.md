# Spec Delta

## MODIFIED Requirements

### Requirement: Upkeep capability
Foundation SHALL define a sided block capability `servus_foundation:upkeep`. A holder SHALL name its upkeep item (none when empty), the level to keep, the batch to wait for and how many of that item it holds right now, SHALL keep its open upkeep request and SHALL be told when a refill of an amount was asked for. Any mod SHALL be able to register a holder for its blocks.

#### Scenario: Burner with coal
- **WHEN** a mod asks the upkeep capability of a machine keeping coal at 16 with a batch of 8 that holds 5 coal
- **THEN** it gets coal, level 16, batch 8 and count 5

#### Scenario: No upkeep
- **WHEN** a mod asks the upkeep capability of a stone block
- **THEN** it gets nothing

### Requirement: Upkeep stays stocked
Every data network SHALL check its upkeep members at a fixed interval (example every 40 ticks) without scanning blocks. When a member has no open request and is missing at least its batch, or holds none at all, the network SHALL ask the suppliers on the line, lowest position first, for everything missing up to its level and keep the answer as the member's open request. Upkeep SHALL never be part of an order.

#### Scenario: Refill
- **WHEN** a Coal Generator keeping coal at 16 with a batch of 8 holds 4 coal and a warehouse with coal and an Access Point is on its line
- **THEN** the network asks for 12 coal and the generator ends up with 16 coal

#### Scenario: Less than a batch missing
- **WHEN** a Coal Generator keeping coal at 16 with a batch of 8 holds 14 coal
- **THEN** nothing is asked for until it holds 8 coal or less

#### Scenario: Batch larger than the level
- **WHEN** a machine keeps its upkeep at 4 with a batch of 8 and holds 1
- **THEN** nothing is asked for until it holds none, then 4 are asked for

#### Scenario: One request at a time
- **WHEN** 12 coal were asked for and only 6 have arrived
- **THEN** the next checks ask for nothing until the rest arrives, the supplier no longer has the request open, or the request times out

#### Scenario: No supplier
- **WHEN** an upkeep member is on a line without any supplier
- **THEN** nothing is asked for and the member keeps what it holds

### Requirement: Upkeep bookkeeping is pure logic
The decision for one member (credit arrivals counted from the holder's count against the open request, end it when complete, when its supplier ticket is no longer open or after a timeout, say whether a timed out ticket must be cancelled, and how much to ask for given level and batch) SHALL be made by a pure Java class without Minecraft imports, covered by JUnit tests.

#### Scenario: Arrival completes a request
- **WHEN** 10 were asked at count 2 and a later check sees count 12
- **THEN** the request is complete and the next check may ask again

#### Scenario: Ticket ended at the supplier
- **WHEN** 10 were asked at count 2, a later check sees count 6 and the supplier reports the ticket as no longer open
- **THEN** the request ends without a cancel and the same check may ask for what is missing

#### Scenario: Timeout cancels
- **WHEN** a request is still open at the supplier after its timeout
- **THEN** the request ends and its ticket is to be cancelled

### Requirement: Burner machines keep fuel as upkeep
The Burner Pump, the Coal Generator and the Calciner SHALL expose the upkeep capability for their fuel slot on every side. The upkeep item SHALL be the item in the fuel slot when it burns, the machine stats `upkeep_item` when the slot is empty, and none while the slot holds something that does not burn. The count SHALL be the number of that item in the fuel slot, and the level SHALL be at most one stack of that item. Upkeep that arrives goes into the fuel slot.

#### Scenario: Generator fuel
- **WHEN** a Coal Generator with 5 coal in its fuel slot is asked for its upkeep
- **THEN** it answers coal, level 16 and count 5

#### Scenario: Coal Blocks in the fuel slot
- **WHEN** a Coal Generator keeping coal at 16 holds 11 Coal Blocks in its fuel slot and a warehouse with coal and Coal Blocks is on its line
- **THEN** it answers Coal Blocks with count 11, nothing is asked for while fewer than a batch are missing, and once asked the Coal Blocks it gets join the slot

#### Scenario: Empty fuel slot
- **WHEN** a Coal Generator with an empty fuel slot is asked for its upkeep
- **THEN** it answers coal, the stats upkeep item, with count 0

#### Scenario: Blocked fuel slot
- **WHEN** the fuel slot of a Burner Pump holds an empty bucket left by lava
- **THEN** it asks for nothing until the bucket is taken out

#### Scenario: Level capped at a stack
- **WHEN** a Coal Generator keeping its upkeep at 16 burns a Lava Bucket in its fuel slot
- **THEN** its level is 1 and nothing more is asked for while the bucket is there

### Requirement: Upkeep stats
Machine stats SHALL have `upkeep_item` (an item id, empty for none), `upkeep_level` (a count, 0 for none) and `upkeep_batch` (how many must be missing before the machine asks again, at least 1). The defaults SHALL be coal, 16 and a batch of 8 (example values) for the Burner Pump, the Coal Generator and the Calciner and no upkeep for the other machines. A machine with no upkeep item or a level of 0 SHALL expose no upkeep.

#### Scenario: Retuned level
- **WHEN** a datapack sets `upkeep_level` of `servus_foundation:coal_generator` to 32 and the server reloads
- **THEN** the Coal Generator's fuel slot is kept at 32 coal

#### Scenario: Retuned batch
- **WHEN** a datapack sets `upkeep_batch` of `servus_foundation:coal_generator` to 1 and the server reloads
- **THEN** the Coal Generator asks again as soon as one coal is missing

#### Scenario: Upkeep switched off
- **WHEN** a datapack sets `upkeep_level` of `servus_foundation:burner_pump` to 0
- **THEN** the Burner Pump exposes no upkeep and is no data member

## ADDED Requirements

### Requirement: Open upkeep request survives a restart
An upkeep holder SHALL save its open request (supplier, ticket, amount still missing, last seen count and timeout) with its block. After a restart or a chunk reload the network SHALL go on from the saved request instead of asking again, and items gained since the last seen count SHALL count against it.

#### Scenario: Restart with a request on its way
- **WHEN** a Coal Generator asked for 12 coal, 4 arrived, and the server restarts while the supplier still has the request open
- **THEN** after the restart the generator asks for nothing new and the request ends once the other 8 arrive

#### Scenario: Restart after the shipment ended
- **WHEN** the generator's saved request names a ticket the supplier no longer has open after the restart
- **THEN** the request ends and the network asks for what is missing in the same check, if at least a batch is

### Requirement: Given up upkeep is cancelled at the supplier
When an open upkeep request times out while its supplier still has the ticket open, the network SHALL cancel the ticket at that supplier before it may ask again, so no second request runs beside the first. While the supplier's block is not loaded the request SHALL stay open; when the block is loaded but no longer a supplier, the request SHALL end without a cancel.

#### Scenario: Refused shipment times out
- **WHEN** an upkeep shipment waits at the Packing Station because its machine refuses it, and the request times out
- **THEN** the shipment is cancelled, the picked items go back into storage, and only then is a new request made

### Requirement: Supplier tickets
The supplier capability SHALL answer a request with the amount it will send and a ticket for it, SHALL tell whether a ticket is still open, and SHALL cancel a ticket on request. A ticket it does not know SHALL count as not open, and cancelling it SHALL do nothing.

#### Scenario: Unknown ticket
- **WHEN** the network asks a supplier about a ticket it never handed out
- **THEN** the supplier answers that it is not open
