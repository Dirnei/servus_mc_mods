# Servus: Foundation changelog

## 0.4.0

### Added
- The Data Potato reports on Servus storage blocks too, without opening them.
- A number stepper and a scrollable item list for the screens of every Servus mod.

### Fixed
- Utility Ducts no longer wash away in water or lava. They hold back flowing fluids, can be placed into water and are waterloggable with a bucket.

## 0.3.0

### Added
- Mill: grinds items while a Hand Crank or Electric Motor drives it. Ships Gravel into Sand, more milling recipes come from datapacks. Shown in JEI.
- Hand Crank: hold right click to turn it and drive the block it sits on. Costs a little hunger.
- Electric Motor: drives the block it faces from FE and draws power only while that block has work. Takes a Speed Upgrade.
- Heating Element: lets the Tiled Stove (Delicacies) and the Brewhouse (Brewery) heat with FE instead of wood.
- The Sink buffers what it gets: a row of 7 slots and an 8,000 mB tank, one more of each per Capacity Upgrade, voided in batches every second. New GUI.
- Chimneys work as decoration away from the Sink: the Configuration Carrot picks the smoke (off, thin, cosy, signal), sneak plus carrot picks a redstone mode (always, powered, unpowered).
- Coal Generator, Burner Pump and Electric Pump screens show why the machine is dark: waiting for the buffer to drop, out of fuel or no power. The bar marks the restart level.

### Changed
- The Sink and the Chimney are crafted from any stone and take its look. A Sink is built from one stone only.
- Burner machines bank their fire while they have no work instead of burning fuel away.
- Coal Generator, Burner Pump and Electric Pump stop when full and restart once the buffer is below half, so they no longer flicker under a small load.
- Item Pipe and Fluid Pipe recipes use more copper than iron.
- The Ender Anchor works next to FTB Chunks by default.

## 0.2.0

### Added
- Capacity Upgrade.

### Changed
- Gantries lay their own rail, and elevators can take the place of a rail.
