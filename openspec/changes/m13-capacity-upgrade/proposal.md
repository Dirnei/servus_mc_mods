# Proposal

## Why

The Fluid Gantry (`m13-fluid-gantry`) grows its built-in tanks with upgrades. Foundation's upgrades only speed things up (`foundation/upgrades`: slots multiply a rate), so there is no item that says "more room". A Capacity Upgrade in Foundation can serve the gantry first and other buffers of the family later (the Field Head buffer, station buffers). Decided in `/opsx:explore` on 2026-10-20.

## What Changes

This change starts the new PLAN.md milestone **M13 Fluid logistics** (Shuttles carry fluids), item `m13-capacity-upgrade`.

- **Capacity Upgrade item** in Foundation (`servus_foundation:capacity_upgrade`), crafted from vanilla materials (example: iron ingots, glass and a bucket), stacks to 16.
- **Capacity upgrade slots**: a slot kind that takes only Capacity Upgrades, one per slot, and reports how many it holds. What one upgrade adds is up to the block that has the slots and lives in that block's data.
- A Capacity Upgrade is not a speed upgrade: slots that take speed upgrades do not take it, and capacity slots take nothing else.
- `PLAN.md` gets the M13 milestone with its open points.

## Capabilities

### New Capabilities
- none

### Modified Capabilities
- `foundation/upgrades`: the Capacity Upgrade item and capacity upgrade slots.

## Non-goals

- Any block using the slots (the gantry comes in `m13-fluid-gantry`; other buffers later).
- Capacity upgrades from other mods through a tag or JSON entry (can follow when a second mod asks).
- Taking the upgrade out while content would no longer fit: that rule belongs to each block.

## Impact

- Code, Foundation: new `upgrade/CapacityUpgradeItem.java`, `upgrade/CapacityUpgradeSlots.java`, registration in `registry/ServusFoundationItems.java` and the creative tab, datagen (item model, lang with tooltip, recipe), item texture `textures/item/capacity_upgrade.png`.
- Tests: JUnit for the slot rule if it is pure; a Foundation GameTest for the slot and the recipe.
- Docs: `PLAN.md` M13 section.
