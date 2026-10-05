# Proposal

## Why

The wrench is the shared tool every Servus mod uses to configure blocks, and the config scaffolding is where balance knobs that are not datapack content will live. PLAN.md milestone M1, items "Wrench item" and "Config scaffolding". This change also sets up the registry and datagen plumbing that every later content change copies.

## What Changes

- Lib mod entrypoint takes the `ModContainer`, registers its deferred registers and its common config.
- `ServusLibItems` with the **Wrench** item, `ServusLibCreativeTabs` with a "Servus" creative tab that other Servus mods can add to, plus the wrench texture.
- `WrenchItem`: right click rotates blocks that have a facing property (vanilla or modded), unless sneaking. Blocks that implement `Wrenchable` handle the click themselves.
- `ServusLibConfig`: common config (`servus_lib-common.toml`) with the first option, whether the wrench rotates blocks that do not implement `Wrenchable`.
- Datagen for Lib: language file, item model and the wrench crafting recipe generated into `src/generated/resources`.
- Lang keys for the wrench, the creative tab and the config option.

## Capabilities

### New Capabilities
- `lib/wrench`: what the wrench does when used on blocks, how blocks opt in, how it is obtained.
- `lib/config`: the common config file of Servus Lib and how its options behave.

### Modified Capabilities
None.

## Non-goals

Deferred to later changes:
- Engineer goggles (M5, with the duct overlay).
- Wrench dismantling of Servus machines (each machine's change defines its `Wrenchable` behavior).
- Per mod configs for Manufacturing, Logistics and Storage (added by their first change that needs a knob).
- Final art. The wrench texture is a placeholder until the art direction is decided.

## Impact

- `ServusLib` constructor signature changes to `(IEventBus, ModContainer)`.
- New `src/generated/resources` content in Lib, produced by `gradlew :lib:runData`.
- A new config file appears in `config/servus_lib-common.toml` when the game runs.
