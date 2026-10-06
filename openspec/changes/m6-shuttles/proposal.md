# Proposal

## Why

Rails and elevators link the warehouse, but nothing moves on them: every pull at the counter is instant. PLAN.md milestone M6 item "Simulated shuttles with client rendering" makes picks physical. Shuttles are data on the controller, travel the rail graph to the block that holds the item, bring it back, and are drawn moving on the client. Throughput now depends on shuttle count, rail layout and distance, which is the core of the storage mod's progression. The user asked on 2026-10-06 to continue with this item without a separate review pause, so the open decisions are settled in the design and recorded in PLAN.md.

## What Changes

- New item **Shuttle** in `servus_storage`. Right clicking a Warehouse Controller with it adds a shuttle to that warehouse, up to `max_shuttles` from `warehouse.json` (example 8). Sneak right clicking the controller with an empty hand takes one back out. Dismantling the controller drops its shuttles.
- Shuttles live as data on the controller's network. They park on the home rail, the rail touching the controller, and move along rails and elevator segments at `shuttle_speed` blocks per tick from `warehouse.json` (example 0.25).
- Pulling at the counter terminal becomes a pick job when the warehouse has shuttles and the item sits in a storage block that a rail reaches: a free shuttle drives to the rail beside that block, takes up to a stack, drives home and hands the stack to the player who asked, or drops it at the terminal when that player is gone. Pulls from blocks no rail reaches, and all pulls in a warehouse without shuttles, stay instant as today. Pushes stay instant.
- Jobs queue when every shuttle is busy; the nearest free shuttle takes the next job.
- Clients see shuttles move: the controller sends the fleet's paths and progress to players tracking its chunk while any shuttle moves, and a renderer on the controller draws each shuttle as a small cart on the rail with its cargo on top, interpolated between updates.
- The controller's summary line and the terminal's offline reasons stay; the summary gains the shuttle count and how many are busy.
- PLAN.md section 5 open decision "Does a warehouse without shuttles feel complete enough on its own?" is settled: yes, a warehouse without shuttles works instantly, shuttles add throughput limits and life.

## Capabilities

### New Capabilities
- `storage/shuttles`: the shuttle item, the fleet on the controller, movement on rails, pick jobs, delivery and client rendering.

### Modified Capabilities
- `storage/counter-terminal`: "Pull to cursor or inventory" states when a pull is served by a shuttle and when it is instant.
- `storage/warehouse-controller`: new requirements for the fleet, the home rail and the shuttle count in the summary.

## Non-goals

Deferred to later M6 and M7 changes:
- Put-away by shuttle, dispatch station, put-away rules and overflow zones.
- Pick rate statistics.
- Power use, shuttle tiers, collisions or traffic on shared rails. Shuttles pass through each other.
- Reserving stock for a job. If the item is gone when the shuttle arrives, it comes home empty.

## Impact

- `servus_storage`: new `shuttle` package with pure path finding and movement plus JUnit tests, the fleet and job logic in the warehouse network, a shuttle count on the controller block entity, the shuttle item, a sync payload, a client side shuttle store and a controller renderer, `warehouse.json` gains two numbers, datagen for the item model, recipe and lang, a shuttle texture, a GameTest.
- `storage/counter-terminal` pull path changes for warehouses with shuttles.
- `servus_lib`: none. The travel graph comes from the existing network graph neighbours.
- PLAN.md: section 5 loses the shuttle open decision, section 2.3 states how shuttles work.
