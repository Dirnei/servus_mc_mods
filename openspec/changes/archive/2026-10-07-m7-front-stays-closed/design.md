# Design

## Context

`SideConfig` holds six modes, a supported list and a fallback; `set`, `cycle` and `fromBytes` change any side. `SideConfigPanel` draws every cell the same way and forwards clicks. Machines and the Crafting Station build `SideConfig.machine()`.

## Goals / Non-Goals

**Goals:**
- A fixed side is invisible to every writer and obvious in the panel.

**Non-Goals:**
- See proposal.

## Decisions

### Settles no PLAN.md open decision

### Lock in the config
`SideConfig.lock(RelativeSide, SideMode)` sets the mode and adds the side to a locked set; `set` and `cycle` return without change for locked sides, `fromBytes` goes through `set`, `isDefault` skips locked sides, `isLocked(side)` for the panel. Machines and the Crafting Station call `lock(RelativeSide.FRONT, SideMode.DISABLED)` in their constructor, before any load.

### Panel
`SideConfigPanel` takes a `Predicate<RelativeSide> locked`; a locked cell is filled with the Off colour dimmed, its letter grey, no hover highlight, no click, and its tooltip ends with "(fixed)" from `gui.servus_lib.side_fixed`.

### Minecraft side verification
`SideConfigTest.lockedSideStays`; GameTest `machineFrontIsClosed`: a Burner Smelter facing north, the front handler refuses coal and extracts nothing, `cycleFace(north)` stays Off.

## Risks / Trade-offs

- [Saved machines with a configured front] → the lock wins on load; nothing is released.

## Open Questions

None.
