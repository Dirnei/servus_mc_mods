# Servus: Shuttles changelog

## 0.4.0

### Added
- Shuttles get a number per world, shown in their tooltip and in the garage.
- Slots a shuttle job fills or empties are locked and shown with a colour wash, a ghost item and the shuttle's label.
- Shuttles follow each other with a gap, take turns at merges and leave a garage one after another.
- A one-off order grid in the Storage Terminal, opened with the hammer button right of the panel: ghost items by click or drag, Clear, and Save as pattern without ordering.
- One amount panel for the order grid and craftable items: an amount stepper, scrolling From stock, To craft and Missing lists, and Start greyed with the reason.
- The Shuttle Garage has a real doorway with a roll-up shutter that opens for its shuttles.
- The Warehouse Controller and a new Storage tab in the Production Terminal show how full the storage is, per warehouse and zone.
- The Data Potato reports a rack section's used and free slots and a Fluid Gantry's tanks in chat.

### Changed
- Put-aways pick the rack by zone, then bays already holding the item, then the most room, weighed by travel time.
- A shuttle whose cargo has nowhere to go parks in its garage with a red lamp instead of dropping it.
- The Field Head buffer and field runs reserve their slots; with a full warehouse a Harvester parks in its garage instead of dropping crops on the field.
- The Storage Terminal counts only what can still be requested and asks only for what is there unless a CPU crafts the rest.
- Stations and the terminal say "no shuttle available" when no shuttle can take a job.
- Storage Terminal requests sit on their own Output row slot with progress; a click requests one, shift a stack, right click asks for an amount, middle or Ctrl click cancels.

### Fixed
- Shuttle jobs, their cargo and station claims survive a reload or an unloaded warehouse; nothing drops mid-trip.
- A Warehouse Controller placed against the side of an elevator shaft joins the warehouse.
- Shuttles roll out of and into their garage smoothly instead of appearing and vanishing on the rail in front of it.
- Rails, farm rails and elevator shafts no longer wash away in water or lava. They hold back flowing fluids and cannot be placed into a fluid source.
- JEI's plus fills the Storage Terminal order grid when a CPU is on the line, and a carried stack shows above the terminal's dialogs.

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
