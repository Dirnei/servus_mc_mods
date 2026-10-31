# Spec Delta

## MODIFIED Requirements

### Requirement: Warehouse as supplier
The Warehouse Controller and every Packing Station SHALL expose the supplier capability. A request SHALL be shipped like any shipment to a data member: through the Packing Station with the shortest shuttle trip, as packets. The answer SHALL be the amount planned, at most what the warehouse holds, and zero when the warehouse cannot ship, together with the shipment as its ticket. The ticket SHALL be open while the shipment is open, also after a reload. Cancelling it SHALL end the shipment like a cancelled terminal request: its queued picks are dropped and what waits for it in the To network buffer goes back into storage.

#### Scenario: Upkeep from the warehouse
- **WHEN** a Machine Adapter on the line asks the controller's supplier for 16 coal and a rack served by a shuttle holds 40
- **THEN** the answer is 16 and 16 coal arrive at the adapter as packets

#### Scenario: Out of stock
- **WHEN** the warehouse holds no coal
- **THEN** the supplier answers 0

#### Scenario: Ticket of a delivered shipment
- **WHEN** all 16 coal of a supplier request arrived
- **THEN** the supplier reports its ticket as not open

#### Scenario: Cancel a refused shipment
- **WHEN** a supplier shipment of 8 coal waits in the To network buffer because its target refuses it, and its ticket is cancelled at the Packing Station's or the controller's supplier
- **THEN** the shipment ends, the 8 coal go back into storage, coal is no longer wanted in the buffer, and queued picks for it are dropped

### Requirement: Shipping to a data member by shuttle
A mod SHALL be able to ask a warehouse, on the server, to ship an amount of an item to a member of a data line. The warehouse SHALL plan and lock the picks like a terminal request, but shuttles SHALL bring picks into the To network buffer of the chosen Packing Station only as far as the member has granted room and the buffer has room. The station SHALL send what arrives at no more than its rate out of storage. A shipment its member refuses SHALL wait without holding up the station's other shipments and be offered again every push interval. A shipment to a machine target, any data member that is not a Storage Terminal, SHALL end once its target has refused it without a break for longer than the refusal timeout (example 1200 ticks). A terminal request SHALL wait for room in its Output slot with no timeout. A shipment whose target is cut from the line, because no route of data ducts reaches it any more, its route broke, or it no longer is a member of a data line reaching the station, SHALL wait and be judged like a refusal: it ends only once it has been cut or refused without a break for longer than the refusal timeout, for terminal requests too. A shipment whose target is removed, because no block at that loaded position takes items any more, SHALL end at once. A target in an unloaded chunk SHALL count as neither removed, cut nor refusing: its shipment waits. A shipment the station ends this way SHALL end short: what waits for it in the To network buffer goes back into storage, its queued picks are dropped, and the request reports it ended, so whoever asked may ask again. The request SHALL report how many items were planned, how many were delivered so far and whether it ended. It SHALL be refused when the warehouse is offline, has no Packing Station on a data line reaching the member, has none of the item, or has no shuttle that reaches the racks holding it.

#### Scenario: From a rack
- **WHEN** a warehouse with 32 planks in a rack served by a shuttle and a Packing Station on a data line to a Crafting Station ships 16 planks to the station
- **THEN** a shuttle brings the planks to the Packing Station first, then 16 planks arrive in the station's inputs and the request reports 16 delivered and ended

#### Scenario: No shuttle
- **WHEN** the planks sit in a rack and the warehouse has no garage
- **THEN** the request is refused because it needs a shuttle and nothing leaves the warehouse

#### Scenario: No Packing Station
- **WHEN** the warehouse has no Packing Station on a data line that reaches the member
- **THEN** the request is refused and nothing leaves the warehouse

#### Scenario: Full target does not block the station
- **WHEN** a request for 128 copper has one Output slot that the player keeps full, and a request for 16 iron at the same station follows
- **THEN** the 16 iron arrive while the copper waits

#### Scenario: Nothing parks for a full slot
- **WHEN** the copper request's slot is full with 64 copper and the other 64 are still in the rack
- **THEN** no shuttle brings copper to the Packing Station until the player takes copper out of the slot

#### Scenario: Picks stay locked
- **WHEN** the other 64 copper wait in the rack for room in the slot
- **THEN** the terminal's requestable count does not include them and no other request can take them

#### Scenario: Full Output slot outlasts the timeout
- **WHEN** a terminal request's Output slot stays full for longer than the refusal timeout of 1200 ticks
- **THEN** the request is still open, its picks stay locked, and its items arrive once the player takes items out of the slot

#### Scenario: Refused past the timeout
- **WHEN** a Coal Generator whose fuel slot holds Coal Blocks refuses a shipment of 8 coal waiting in the To network buffer, with a refusal timeout of 1200 ticks, for longer than 1200 ticks
- **THEN** the shipment ends short, the 8 coal go back into storage, no queued pick for it remains, and the request reports it ended

#### Scenario: Room in between restarts the timeout
- **WHEN** a machine target refuses a shipment for 1000 ticks, then takes some of its items, then refuses again
- **THEN** the shipment ends only after the target has refused it for longer than the timeout counted from the new refusal

#### Scenario: Target removed
- **WHEN** the Crafting Station a shipment of 16 planks goes to is broken while 8 planks wait in the To network buffer and 8 are still in the rack
- **THEN** at the next push interval the shipment ends short, the 8 planks go back into storage and the queued pick for the other 8 is dropped

#### Scenario: Duct rebuilt within the timeout
- **WHEN** the duct between the Packing Station and a shipment's Crafting Station is broken and placed again 200 ticks later, with a refusal timeout of 1200 ticks
- **THEN** the shipment is still open and its items arrive at the Crafting Station

#### Scenario: Target cut from the line past the timeout
- **WHEN** the duct between the Packing Station and a shipment's target is broken and stays broken for longer than the refusal timeout
- **THEN** the shipment ends short and its buffered items go back into storage

#### Scenario: Target not loaded
- **WHEN** a shipment's target sits in a chunk that is not loaded while the Packing Station runs
- **THEN** the shipment waits, its items stay in the To network buffer and it is offered again once the target is loaded
