# Proposal

## Why

A play check follow up to M10 field work (`m10-harvester`, `m10-spreader`, `m10-rail-corrosion`), not an item in PLAN.md. A field never got fertilized although the warehouse held 512 bone meal: all of it sat in a rack behind an elevator shaft. Spreaders and Harvesters are wide and never ride a lift car, so the Spreader found nothing it could reach, drove home empty, and the head called again every 30 seconds while its status said "waiting". Seeds for replanting have the same blind spot. The anti-corrosion slot is the only part of the head stocked over the data line, the one thing tying a rail block to the cable.

## What Changes

- The Field Head gets a small read-only **supply buffer**: one part for its crop's seeds, one for its fertilizer, beside the anti-corrosion slot. The GUI shows it; players, hoppers and pipes neither put in nor take out.
- A **supply run** keeps the buffer stocked: when a part runs low, the warehouse sends a shuttle without a tool, which may ride the lift. It picks seeds, fertilizer above the head's reserve and anti-corrosion items from the racks and unloads them at the head. Fill levels come from `storage/field.json`.
- Spreaders take fertilizer only from the head's buffer, and Harvesters take missing seeds from it; neither drives to racks for them any more. Leftover fertilizer and spare seeds go back into the buffer first, then into storage.
- The anti-corrosion slot is filled only by supply runs: no hand filling, no data line packets, no upkeep. The Field Head leaves the data line entirely (no packet inlet, no upkeep capability).
- Statuses for what keeps a field unsupplied: no shuttle without a tool, nothing in stock above the reserve, or an empty buffer waiting for a supply run, with a red marker where a player must act.
- Items left in the buffer after the crop or the fertilizer changes go back to storage with the next supply run. Breaking the head drops the buffer.

## Capabilities

### New Capabilities
- `storage/field-supply`: supply runs that keep a Field Head's buffer stocked by shuttle.

### Modified Capabilities
- `storage/field-head`: the GUI shows the buffer, loses the anti-corrosion hand slot, and the work and fertilize statuses name supply problems; a new requirement for the buffer itself.
- `storage/rail-corrosion`: the anti-corrosion slot is filled by supply runs only.
- `storage/spreader`: fertilize jobs and fertilizer come from the head's buffer; leftovers go back into it.
- `storage/harvester`: missing seeds come from the head's buffer.

## Impact

- `FieldHeadBlockEntity`, `FieldHeadMenu`, `FieldHeadScreen`, the field status enums and the field command.
- `ShuttleFleet`, `FieldRuns`, `WarehouseNetwork.tickFields`: a supply job and run, buffer use in the Harvester and Spreader runs.
- `ServusStorageEvents`: the head's packet inlet and upkeep registrations go.
- `storage/field.json`, lang, docs (`docs/.../storage.md`), GameTests, pure logic with JUnit.
- Fields in existing worlds start with an empty buffer; unreleased, no migration.

## Non-goals

- Limits on harvesting and fertilizing by stock (`m10-field-stock-levels`).
- Moving stock between racks, shuttle to shuttle handovers, a Stack Upgrade.
- Markers in the world for racks a wide shuttle cannot reach.
