# Proposal

## Why

The Heavy Duty Rack paints its sample flat onto the front face, which looks like a sticker on a frame that is open and has real shelves. The user asked on 2026-10-06 for the sample to stand on the shelves in 3D, and for the rack's connection lamp to go because it does not look good on the frame. This refines the PLAN.md M4 line "Heavy Duty Rack".

## What Changes

- The sample items stand on the shelf boards inside the frame as 3D item models, two per shelf level, slightly turned, instead of flat sprites on the front face. Blocks show as small cubes, flat items as upright sprites.
- The rack has no connection lamp any more. Its online state is still visible through the rail dock and the terminal; crates, the controller and the terminal keep their lamps.
- Everything else about the rack stays: sample selection, sync, GUI, joins, indexing.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/heavy-duty-rack`: "Sample display" says the sample stands on the shelves in 3D; "Rack is a storage block" drops the lamp.
- `storage/connection-indicator`: "Lamp on storage blocks" names crates of all sizes, controllers and terminals, and states that racks carry no lamp.

## Non-goals

- Changing the sample size or how it is picked.
- 3D display on crates. They are closed boxes and keep the painted front.

## Impact

- `servus_storage`: `HeavyRackRenderer` poses items on the shelves, `HeavyRackBlock` drops the `ONLINE` property, `HeavyRackModels` drops the lamp parts and models, the two lamp textures are deleted, the GameTest no longer asserts the rack lamp.
- Generated resources: the rack blockstate loses its lamp entries and `online` property.
