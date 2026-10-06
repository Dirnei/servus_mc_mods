# Proposal

## Why

The wrench is a placeholder tool with a placeholder texture, and every tech mod has a wrench. Servus gets its own signature tool instead: the Configuration Carrot, a carrot with a metal probe where the greens used to be. It does exactly what the wrench does today, so the whole mod family keeps working, but the item players craft, hold and see in the creative tab is now unmistakably Servus. This revisits the PLAN.md milestone M1 item "Wrench item".

## What Changes

- The item `servus_lib:wrench` is removed and replaced by `servus_lib:configuration_carrot`, display name "Configuration Carrot". Nothing has been released, so there is nothing to migrate.
- New 16 by 16 texture: a carrot body without its green leaves. In place of the greens a short grey metal bar sticks out of the top, ending in a red tip. The old wrench texture is deleted.
- New recipe: one carrot, one iron ingot and one redstone dust give one Configuration Carrot. The wrench recipe is removed.
- The Configuration Carrot keeps every wrench behavior: it rotates blocks with a facing, hands the click to `Wrenchable` blocks, sneak dismantles Servus blocks, and counts as a reveal trigger for hidden ducts like the goggles.
- The Servus creative tab uses the Configuration Carrot as its icon and lists it in place of the wrench.
- Lang entries and config option texts that say "wrench" now say "Configuration Carrot". The config keys stay the same, the role is still called wrench in code and config.
- Content mods swap their `WrenchItem` checks for the renamed item class. The `Wrenchable` block interface and its `onWrench` method keep their names: "wrench" stays the name of the role a block reacts to, the carrot is the item filling it.
- PLAN.md mentions of the wrench item are updated to name the Configuration Carrot.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `lib/wrench`: the obtainable item changes. The requirement "Wrench is obtainable and listed" is replaced by one that describes the Configuration Carrot, its recipe, its look and its place in the creative tab. The purpose text names the carrot as the item that fills the wrench role. Rotation and `Wrenchable` behavior are unchanged.
- `lib/goggles`: the reveal rule names the Configuration Carrot as the hand held reveal trigger instead of the wrench.

## Non-goals

Deferred or deliberately left out:
- Renaming `Wrenchable` and `onWrench`, and the "wrench" wording in the machine, duct and storage specs. Those describe the role, not the item, and the lib/wrench spec says so.
- Making the carrot edible, enchantable or usable as food for animals. It is a tool with a stack size of one.
- Renaming the `wrench` section and `wrenchRotatesBlocks` key in `servus_lib-common.toml`. The role keeps its name.

## Impact

- `servus_lib`: item registration, item class rename, texture, datagen for model, recipe, recipe advancement and lang, creative tab, goggles reveal helper, config comment text.
- `servus_manufacturing`, `servus_logistics`, `servus_storage`: one import and one `instanceof` check per block class that recognizes the tool (MachineBlock, UtilityDuctBlock, WarehouseControllerBlock, CrateBlock, RackBlock, CounterTerminalBlock) plus ConnectableBlock in lib.
- Generated resources under `lib/src/generated/resources` are regenerated: the wrench model, recipe and recipe advancement go away, the carrot ones appear.
- JEI shows the new recipe automatically through the recipe viewer integration.
- PLAN.md: the Lib table row, section 2.2 bullets and the M1 item mention the Configuration Carrot.
