# Servus: Shuttles changelog

## Unreleased

### Added
- Inbound, Outbound and Packing Stations get a number per warehouse ("Outbound #3", "Packing #1") and an optional name typed into their title row ("Compost Dock (#3)"). The Data Potato on a station names it, its site and whether it is online.
- Shuttles light up on the rails: white headlights in front, tail lights that show whether a shuttle drives, brakes, is held behind another or waits for the lift or power, and a number plate on both sides. The Data Potato on a rail or shaft names every shuttle there and what it waits for, following the queue and naming a deadlock.
- Tree fields: a Field Head set to Trees plants saplings on every other plot (2x2 trees in squares with a Range Upgrade), needs no water, and a Lumber shuttle fells grown trees with their leaves, brings the wood home and replants.
- Lumber shuttles fell trees with an arm and a saw, and the tree topples away from the rail, lands and breaks apart into the shuttle.
- Patterns can be Exact or Any. An Any pattern, such as sticks or a chest, crafts from whichever planks or logs the warehouse holds most of; the recipe editor has the Exact / Any button and the Patterns tab marks Any patterns.
- The Storage Terminal has Stock and Rules tabs. Rules lists every stock rule with a search, and a carried item or a JEI drag on its add slot starts a rule for any item. A right click on an item opens one popup with Request, Craft and Rule tabs in place of the amount prompt.

### Changed
- The Lumber shuttle carries a closed hold; at a tree its doors and walls fold open and a harvester crane rises out of it, grips the trunk with rollers and knives, cuts it with a chainsaw you can hear, and folds away again before it drives on.
- Harvesters and Spreaders unload into a To storage buffer on the Field Head, and plain shuttles put it away into the racks.
- A data line belongs to one warehouse. A line that reaches two warehouses is in conflict and pauses: its terminals show no stock and refuse requests, and its Packing Stations send nothing. Terminals no longer add up the stock of several warehouses.
- Packing Stations reserve whole To storage slots before items arrive and put a slot away only once it is complete, so one shuttle takes the whole stack, and the warehouse picks the station closest to the rack. A Packing Station moves 10 items per second.
- A Storage Terminal request takes as many Output slots as its amount needs, and Packing Stations send only what a target has room for, so a full slot no longer holds up other deliveries; the station's status names a target that is full.

### Fixed
- The Storage Terminal's scrollbar can be dragged with the mouse.
- A click anywhere in the Storage Terminal takes the focus from the search box, and JEI's recipe and usage keys work on the items in its grid.
- Recipes that put a material into their result, such as a Heavy Duty Rack from oak planks, can be ordered and saved from the order grid, and JEI fills the grid with stocked items and their materials.

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
