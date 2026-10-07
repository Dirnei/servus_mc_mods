# Proposal

## Why

Play test (2026-10-07): the station fronts should be closed like the machine fronts, and a fixed side should draw no marker at all while the carrot is held. The Outbound Station pushed through its front until now; with the front closed, the machine it serves sits behind it, which matches the garage taking its rail at the back.

## What Changes

- **Inbound Station**: the front is fixed to Off; the other five sides stay Input by default.
- **Outbound Station**: the front is fixed to Off; the back is Push by default and the machine the station serves is the block behind it. The producer report and work orders follow.
- **Markers**: fixed sides draw neither frame nor glyph.

## Capabilities

### Modified Capabilities
- `lib/side-config`: fixed sides draw no marker.
- `storage/inbound-station`: closed front.
- `storage/outbound-station`: closed front, machine behind, back pushes by default.

## Non-goals

- Changing the station textures. The lamps stay on the front.

## Impact

- `lib`: `SideMarkerRenderer` skips locked sides.
- `storage`: both station block entities lock the front; the Outbound Station reads its machine at `machinePos()` behind it; station screens grey the front cell; GameTests turn their outbound stations around.
- `PLAN.md` 2.4: the Outbound Station stands with its back to the machine.
