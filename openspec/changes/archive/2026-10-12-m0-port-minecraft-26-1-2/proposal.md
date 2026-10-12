# Proposal: Port the Servus mods to Minecraft 26.1.2

## Why

The target pack moved on: ATM11 runs Minecraft 26.1.2 on NeoForge 26.1.2.x, and NeoForge itself names 26.1 the next long lived modding version after 1.21.1. The Servus mods are still built for 1.21.1 / NeoForge 21.1, so they cannot be installed in ATM11. Nothing is released yet, so the port is cheap now and only gets more expensive with every milestone built on 1.21.1.

This is a cross cutting M0 item (build tooling and platform), not a feature of a later milestone. It does not touch the M11 work in flight; the user merges both branches later.

## What Changes

- Build tooling: Gradle wrapper 9.2.1 (26.1 needs 9.1 or newer), ModDevGradle 2.0.148, NeoForge 26.1.2.116, Java toolchain 25 provisioned by Gradle through the foojay resolver, Parchment removed (26.1 ships unobfuscated with Mojang's own parameter names), the `data` run becomes a `clientData` run, JEI 29.43.0.107 for 26.1.2.
- Foundation is ported to the 26.1.2 APIs: registration with ids in properties, `ValueInput` and `ValueOutput` block entity persistence, the NeoForge transfer API (`ResourceHandler<ItemResource>`, `ResourceHandler<FluidResource>`, `EnergyHandler`) behind the capabilities, the GUI extractor, render state block entity renderers, the new block model API for the duct, facade and wood models, the registry based GameTests, datagen with item model definitions, and recipe display based JEI integration.
- Behaviour stays identical: this is a port, not a redesign. Wherever an old behaviour cannot be kept exactly, design.md records why and what changed.
- Manufacturing and Storage are ported in later runs of this same change. Until then they are left out of `settings.gradle` so Foundation builds alone.
- `CLAUDE.md` (Stack) and `PLAN.md` section 1 name the new platform.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `foundation/mod-family-loading`: the supported platform becomes Minecraft 26.1.2 and NeoForge 26.1.2.x, and Foundation exposes its inventories, tanks and energy buffers through the NeoForge transfer capabilities that replaced `IItemHandler`, `IFluidHandler` and `IEnergyStorage`.

## Non-goals

- No gameplay, balance or content changes. New 26.1 features (copper golems, new blocks) are not used.
- No porting of Manufacturing and Storage in the first run (tasks sections 4 and 5 stay open).
- No backport branch for 1.21.1; main stays on 1.21.1 until the user merges the port.
- No update of the docs site beyond the version line.

## Impact

- Every Java file in `foundation/src/main` and most of `foundation/src/test` that touches Minecraft types.
- `settings.gradle`, `build.gradle`, `gradle.properties`, the wrapper, `foundation/build.gradle`, the `neoforge.mods.toml` template.
- Generated resources of Foundation (`foundation/src/generated/resources`): item model definitions under `assets/<mod>/items/`, the new recipe and loot formats.
- GameTests move from annotated methods to registered test functions and test instances.
- Optional dependencies: JEI 26.1.2 API. Modonomicon, Jade and FTB mods only matter for Manufacturing and the combined runs, which are disabled until Manufacturing is ported.
