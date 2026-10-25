## Why

A player reported a shuttle deadlock near a Rail Elevator, and it was hard to tell from the world what was going on: which way a shuttle faces, which shuttle is which, and why one stands still. Shuttles need to show their state on the rails so a jam can be read in game, by players and by us.

## What Changes

- Shuttles on the rails get **lights**: white headlights on the front and red tail lights on the back, so front and back read at a glance on every tier.
- The tail lights show **why a shuttle drives or stands**: dim red while driving, bright red while braking or held behind another shuttle, blinking amber while it waits for something that is not a shuttle (the lift car, a deck, power, the chunk ahead), off while it works at a stop, rides the lift car or waits at its garage door.
- Numbered shuttles get a **number plate** on both long sides that reads `#12`, drawn only in the world and only within about 16 blocks. The engraved name is not shown on the plate.
- The **Data Potato** used on a Warehouse Rail, Farm Rail or elevator segment reports every shuttle there with its label and what it does or waits for: "Following Shuttle #3", "Waiting for the lift car (floor 2)", "Waiting for power" and so on, following the chain of shuttles it waits behind and naming a cycle when the chain comes back to it.
- The server tells clients the standing reason of each shuttle and syncs a shuttle when the reason changes.

PLAN.md has no open point for this; the change comes from the player report. It adds the deadlock it was found with (a shuttle crossing a shaft held behind the car's own queue) as an open point under "Open: Shuttles".

Milestone: M10 Field work (shuttle looks).

## Non-goals

- Fixing the deadlock itself. A shuttle crossing a shaft on its deck keeps the car from folding that deck; when the shuttle it waits behind waits for that car, nothing moves. That fix is its own change with its own GameTest.
- Lights and plate on the shuttle item. Inventories, the garage GUI and the workshop keep today's look.
- Showing the engraved name on the shuttle; the potato report names it.
- A Data Potato overlay while held (lines over shuttles). The chat report comes first.
- Clicking the shuttle model itself; shuttles are not entities, the potato targets the block under the shuttle.

## Capabilities

### New Capabilities
- `shuttles/shuttle-signals`: the lights, the tail light states, the number plate, the standing reason clients learn and the Data Potato report on rails and shafts.

### Modified Capabilities
- `shuttles/shuttle-looks`: the item keeps its look without lights and plate; "The item looks like the shuttle" gets that exception.

## Impact

- `shuttles` only. Server: `shuttle/ShuttleFleet`, `shuttle/ShuttleLifts` (record why a gate refused), new pure `shuttle/StandReason` and `shuttle/TailLight`, `shuttle/ShuttleView` (one more field, sync codec), a new `shuttle/ShuttleReport` for the potato, `rail/RailBlock` and `rail/ElevatorBlock` implement Foundation's `PotatoInspectable`.
- Client: `client/ShuttleGarageRenderer` draws lamps and plates.
- Lang entries for the report lines; no new items, blocks, recipes or datapack values.
- Network: `ShuttleView` grows by one small field. Client and server ship together, no save data changes.
