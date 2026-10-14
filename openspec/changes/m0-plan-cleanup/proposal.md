# Proposal

## Why

PLAN.md has grown into a build log: the M10 and M11 lists repeat what the archived changes and the main specs already hold, the design summaries describe built blocks in detail, and several lines contradict what was built later (the name plate on the model, tools only at the front, side panels on the alchemy builds, the working agreement versus CLAUDE.md). PLAN.md is meant as the list of what we still want to build.

## What Changes

- PLAN.md keeps the mod family, the vision per mod, the architecture principles, a short Done table and, above all, the open points per mod and the open decisions. Descriptions of built behaviour, change names and decision history go; `openspec/specs/` and the archive keep them.
- The contradictions above are resolved by removing the stale text.
- Decided but unbuilt points that only lived in session notes join the open points (journal `build` and `make` events).
- CLAUDE.md says PLAN.md is groomed after every archived change, mostly by removing what was built, so it stays a list of open points.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
None. No behaviour changes (`skip_specs: true`).

## Impact

- `PLAN.md`, `CLAUDE.md`. No code, data or specs.

## Non-goals

- New decisions or new milestones.
- Changing the main specs.
