# Proposal

## Why

A warehouse built from stacked Heavy Duty Racks has no way up except placing ladders or scaffolding next to it, although the rack already shows sturdy posts on its exposed sides. The user asked on 2026-10-06 that the side of a rack works like a ladder: climbable, but only from the side. This polishes the PLAN.md M4 item "Heavy Duty Rack: chest sized bay with a GUI, visual joins and a sample display".

## What Changes

- A player pushing against an exposed side of a Heavy Duty Rack (a face at a right angle to its facing, where the posts stand) climbs it like a vanilla ladder: pushing moves the player up, hanging without input slides down slowly, sneaking holds the position, and no fall damage is taken while against the side.
- The front and the back of a rack stay plain walls. Nothing changes for the GUI, placement, joins, display or network behavior.
- Only players climb. Mobs treat the rack as a full block as before.
- PLAN.md gets the climbable side posts added to the M4 rack item, since the roadmap does not mention them.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/heavy-duty-rack`: adds the requirement "Sides climb like a ladder" covering climbing, sliding, holding, fall protection and the non climbable front and back.

## Non-goals

- Climbing crates, the controller, the terminal or the rail elevator.
- Mobs climbing racks.
- A ladder like climb for the inner joins of a section. Those sides are covered by the neighbouring bay and cannot be reached.
- Changing the rack's shape, model or collision box.
- Tunable climb speed in `rack.json`. The feel matches a vanilla ladder and is not a balance number.

## Impact

- `servus_storage`: a pure `RackClimb` helper with JUnit tests, a `RackClimbing` player tick handler on the game bus, a GameTest that checks the handler against a rack side and a rack front, and one line in `PLAN.md`.
- No changes to `servus_lib`, datagen, textures or datapack JSON. No new dependencies.
