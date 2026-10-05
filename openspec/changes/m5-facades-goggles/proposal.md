# Proposal

## Why

Ducts along a wall are ugly. Facades make a duct look like any full block, and goggles or the wrench reveal what is hidden. PLAN.md milestone M5, items "Facades and goggles overlay" and "Dynamic baked model" (the facade rendering needs the dynamic model, so both land here).

## What Changes

- **Facade** item: crafted from any full block plus an iron ingot (the plan says iron plate; no plate item exists yet, so iron ingot until Manufacturing adds plates). Six facades per craft, through a special crafting recipe. Using a facade on a duct makes the duct render as that block with a full cube shape; sneak wrench takes the facade off first, then the duct.
- **Dynamic baked model**: the duct's models are wrapped at bake time. With a facade the wrapper serves the facade block's quads, render types and particle texture; without one it serves the normal centre and arms.
- **Engineer Goggles** in Lib: head slot item. Wearing them, or holding the wrench, shows hidden ducts as a translucent overlay drawn through walls.
- Logistics GameTest for the facade recipe and application.
- The duct now answers `getAppearance` with its facade for connected texture mods.

## Capabilities

### New Capabilities
- `logistics/facades`: crafting, applying, rendering, removal, overlay.
- `lib/goggles`: the item and the reveal rule.

### Modified Capabilities
None.

## Non-goals

- Per part selection shapes (PLAN.md "per part shapes"): channel removal lives in the duct GUI since the previous change, so targeting a single channel part is not needed. Flagged as a plan deviation.
- Hollow facade variants, facade rendering of the goggles on the player model, light blocking by facades.

## Impact

- Lib gains an item; Logistics gains a data component, a recipe serializer, client model and overlay code.
- The duct block entity gets a facade state in its saved and synced data.
