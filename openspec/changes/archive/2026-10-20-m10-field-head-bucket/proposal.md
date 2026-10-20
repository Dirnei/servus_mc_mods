# Proposal

## Why

A Field Head takes water only from pipes. A player with a fresh field and a water bucket but no pump yet cannot fill it: right clicking the head with a water bucket opens its GUI and the bucket stays full. Every other water block of the family takes buckets (the Fluid Tank, the Purifier's Vat), so this reads as a bug. Found in `/opsx:explore` on 2026-10-19.

## What Changes

This change is a follow-up to PLAN.md milestone **M10 Field work** (Farm Rail and Field Head), as a new item `m10-field-head-bucket`.

- Using a water bucket, or any item that holds water, on a Field Head pours the water into its buffer as far as it has room, like a Fluid Tank. A creative player keeps the full bucket.
- A fluid container that cannot fill the head (an empty bucket, a lava bucket, a full head) does nothing and does not open the GUI. The head never gives water back.
- Every other item and the empty hand keep opening the GUI as today.

## Capabilities

### New Capabilities
- none

### Modified Capabilities
- `shuttles/field-head`: water by bucket.

## Non-goals

- Taking water back out with an empty bucket (the head stays fill only).
- A message when the head is full.
- Buckets on the farm rail or other field blocks.

## Impact

- Code, Shuttles: `field/FieldHeadBlock.java` (`useItemOn`), a GameTest in `gametest/FieldGameTests.java`.
- No data, recipe or texture changes.
