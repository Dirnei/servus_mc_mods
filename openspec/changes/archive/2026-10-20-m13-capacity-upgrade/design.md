# Design

## Context

Foundation upgrades today: `upgrade/UpgradeItem` (a multiplier), `upgrade/Upgrades` (`TAG` `servus_foundation:upgrades`, `isUpgrade`, `multiplier`), `upgrade/UpgradeSlots extends ItemSlots` (takes `isUpgrade` items, one per slot), and kind restricted slots such as `upgrade/AnchorUpgradeSlots` (takes only `MachineContent.ANCHOR_UPGRADE`, has `count()`) with `upgrade/AnchorUpgradeItem` as a plain `Item`.

## Decisions

### D1. A plain item, like the Anchor Upgrade

`CapacityUpgradeItem` is a plain `Item` with a tooltip, not an `UpgradeItem`, and it is not in the `servus_foundation:upgrades` tag. So `Upgrades.isUpgrade` stays false for it and no speed slot takes it, without touching `UpgradeSlots`.

### D2. Slots copy `AnchorUpgradeSlots`

`CapacityUpgradeSlots(int slots, Runnable onChanged)` follows `AnchorUpgradeSlots`: `isItemValid` accepts only the Capacity Upgrade, limit one per slot, `count()` returns the number of filled slots, `takeAll()` empties them for drops. The owner decides the room per upgrade (the gantry reads it from `gantry.json`). The two slot classes are the same apart from the item, so a shared base `SingleItemUpgradeSlots(int slots, Runnable onChanged, Supplier<? extends Item> item)` holds `isItemValid` and `count()`, and both extend it (a supplier, since the items are deferred registrations).

### D3. Recipe and look

Recipe in Foundation datagen: `IGI / GBG / IGI` with iron ingots, glass and a bucket (example in the spec; it lives in recipe JSON). Item texture 16x16 by a job script: a steel frame around a blue fluid square with a white plus, in the style of `speed_upgrade.png` and `anchor_upgrade.png`.

### D4. PLAN.md milestone

`PLAN.md` gets "M13 Fluid logistics" under the open milestones with: Fluid Gantry with built-in tanks, Tanker import and export runs, fluids in the Storage Terminal, water runs to Field Heads, fluids in requests, stock rules and work orders, a redstone tank sensor. Each later change removes its own line when it is archived.

## NeoForge APIs

- Vanilla `Item`, `ShapedRecipeBuilder`, `ItemModelGenerators` style datagen as used by `AnchorUpgradeItem` (26.1.2 in this repo).

## Testing

- GameTest (Foundation): `capacitySlotTakesOnlyCapacityUpgrades` (refuses Speed, takes one Capacity from a stack, `count()`), `speedSlotRefusesCapacityUpgrade`, `capacityUpgradeRecipe` (recipe manager crafts one).
- No pure logic beyond the slot rule, which lives in a Minecraft slot class.
