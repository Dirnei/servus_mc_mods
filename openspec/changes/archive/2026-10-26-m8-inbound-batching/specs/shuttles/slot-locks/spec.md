# Spec Delta

## MODIFIED Requirements

### Requirement: More of an item joins an open put-away
More of an item arriving at a station SHALL join a put-away of that item from the station that no shuttle has loaded yet, up to a stack per job and as far as its bay has room, instead of becoming a job of its own. At an Inbound Station the more SHALL arrive in the job's own slot when it has room, and the job's OUT lock in that slot SHALL grow by what joined.

#### Scenario: Buffer filling item by item
- **WHEN** 16 planks arrive at a Packing Station one by one while the first shuttle is on its way
- **THEN** they join one put-away of 16 planks instead of sixteen put-aways of one

#### Scenario: Inbound slot grows
- **WHEN** an Inbound Station slot holds 8 cobblestone OUT locked for a put-away no shuttle has loaded and 8 more cobblestone arrive
- **THEN** the slot holds 16 cobblestone, its OUT lock is for 16 and the bay's IN lock for that job grows by 8

#### Scenario: Job full
- **WHEN** the job already takes 64 cobblestone and more cobblestone arrives
- **THEN** the new cobblestone goes into another slot and becomes a put-away of its own

### Requirement: A locked slot is closed to everyone but its job
A locked slot SHALL refuse every insert and extract except those of its own job: clicks, drags, shift clicks and number keys in any GUI, hoppers, item pipes, the stations' own Pull and Push sides, wireless arrivals, packet inlets and the item handler capability as other mods use it. One exception: an Inbound Station slot that holds only OUT locks of put-aways no shuttle has loaded yet SHALL take inserts of its own item up to the stack size, from every inserter including the station GUI, and still refuse every extract. Unlocked slots of the same block SHALL work as before.

#### Scenario: Hand blocked
- **WHEN** a player opens the rack section GUI and clicks a slot OUT locked for a pick
- **THEN** nothing lands on the cursor and the slot keeps its items

#### Scenario: Shift click passes locked slots
- **WHEN** a player shift clicks cobblestone into a rack section whose only slot with cobblestone is IN locked
- **THEN** the cobblestone goes into an unlocked slot or stays in the inventory, never into the locked slot

#### Scenario: Pull side tops up a planned slot
- **WHEN** an Inbound Station slot with 20 bread is OUT locked for a put-away no shuttle has loaded yet and its Pull side offers more bread
- **THEN** the locked slot fills up to 64 bread and only the bread beyond that goes into a free unlocked slot

#### Scenario: Pull side cannot top up
- **WHEN** an Inbound Station slot with 20 bread is IN locked for a pipe grant and its Pull side offers more bread
- **THEN** the locked slot keeps 20 bread and the new bread goes into a free unlocked slot

#### Scenario: Other items stay out
- **WHEN** that slot holds 20 bread OUT locked and a hopper offers carrots
- **THEN** the carrots go into another slot

#### Scenario: Rest after loading
- **WHEN** the shuttle on the put-away has loaded at the station and the slot still holds bread it did not take
- **THEN** that bread is a new stack of its own, planned as a new put-away

#### Scenario: Pipe blocked
- **WHEN** an item pipe tries to pull from an Outbound Station whose only filled slot is locked
- **THEN** the pipe takes nothing from that slot
