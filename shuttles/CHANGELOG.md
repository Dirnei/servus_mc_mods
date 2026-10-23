# Servus: Shuttles changelog

## Unreleased

### Added
- Shuttles get a number per world, shown in their tooltip and in the garage.
- Slots a shuttle job fills or empties are locked and shown with a colour wash, a ghost item and the shuttle's label.
- Shuttles follow each other with a gap, take turns at merges and leave a garage one after another.

### Changed
- Put-aways pick the rack by zone, then bays already holding the item, then the most room, weighed by travel time.
- A shuttle whose cargo has nowhere to go parks in its garage with a red lamp instead of dropping it.

### Fixed
- Shuttle jobs, their cargo and station claims survive a reload or an unloaded warehouse; nothing drops mid-trip.

## 0.3.0

Needs Servus: Foundation 0.3.0 or later.

### Changed
- A mined or dismantled Warehouse Controller keeps its site name and stored FE, shown in its tooltip.
- Farm rail corrodes 50% slower: one step per 9,000 mB of water.
- Warehouse blocks use Foundation's new material system. They look and craft as before.

## 0.2.0

### Added
- Fluid Gantry: stores, imports and exports fluids on the warehouse rail.
- Tanker shuttles import fluids into Storage gantries and restock Export gantries.
- The Field Head can be filled with a water bucket.

### Changed
- The Shuttle Garage turns its doorway to the rail.
- Shuttles slow down in corners.
- Gantries lay their own rail, and elevators can take the place of a rail.
- Rack and elevator items show turned like their blocks in the GUI.
- The Heavy Duty Rack recipe makes three racks.
