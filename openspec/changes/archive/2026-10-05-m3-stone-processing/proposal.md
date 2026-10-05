# Proposal

## Why

The stone deposit yields cobblestone and nothing uses it. Stone Processing turns the fourth deposit into a factory input for building materials. PLAN.md milestone M3, item "Stone Processing".

## What Changes

- Assembling recipes gated by the stone_processing tech: cobblestone to stone, stone to stone bricks, sand to glass, all without a furnace step. Example times and counts live in the recipe JSON.
- The tech's icon and unlock list point at these recipes.

## Capabilities

### New Capabilities
- `manufacturing/stone-processing`: what the stone processing tech unlocks.

### Modified Capabilities
None.

## Non-goals

- New blocks or items; vanilla results only.
- A dedicated stone machine; the assembler and crusher cover it.

## Impact

- Recipe provider and tech JSON only.
