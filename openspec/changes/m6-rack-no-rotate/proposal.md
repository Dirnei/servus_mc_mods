# Proposal

## Why

Turning a rack with the Configuration Carrot breaks it out of its row and its stack, and a rack's facing is a layout decision made when it is placed. The user asked on 2026-10-06 that the carrot must not turn racks. This polishes the PLAN.md M4 line "Heavy Duty Rack".

## What Changes

- The Configuration Carrot no longer rotates a Heavy Duty Rack. A plain carrot click on a rack does nothing; sneak clicking still dismantles it with its contents.
- The rail docking scenario that relied on turning a rack is rewritten: to face a rail, the player breaks the rack and places it again.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/heavy-duty-rack`: "Rack is a storage block" drops the rotation sentence; "Rack is obtainable and dismantled" states the carrot does not turn the rack.
- `storage/warehouse-rail`: the scenario "Rail behind a heavy duty rack" no longer turns the rack.

## Non-goals

- Changing rotation of crates, controller or terminal.

## Impact

- `servus_storage`: `HeavyRackBlock.onWrench` loses its rotate branch, `HeavyRackBlockEntity.facingChanged` goes, a GameTest uses the carrot on a rack and checks the facing.
