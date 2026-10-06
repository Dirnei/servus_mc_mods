# Proposal

## Why

Building a wall of racks means clicking racks with a rack in hand, and today every click opens the GUI instead of placing the next bay. The user asked on 2026-10-06 that only the front opens the rack. This polishes the PLAN.md M4 line "Heavy Duty Rack".

## What Changes

- The rack's GUI opens only when its front face is clicked. Clicking any other face behaves like clicking a plain block: a held block gets placed against it, other items do nothing special.
- Nothing else about the rack changes.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/heavy-duty-rack`: "Chest sized inventory with a GUI" opens from the front only.

## Non-goals

- Changing crates, controller or terminal interaction.

## Impact

- `servus_storage`: `HeavyRackBlock.useItemOn` and `useWithoutItem` check the hit face, a GameTest places a rack against a rack's side and checks the front still opens.
