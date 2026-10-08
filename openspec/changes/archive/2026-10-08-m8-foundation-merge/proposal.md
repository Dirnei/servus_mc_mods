# Proposal

## Why

PLAN.md M8 needs the data cable to live inside the Utility Duct, but Storage was not allowed to depend on Logistics and the cable therefore sat in Lib as its own block. Manufacturing already requires Logistics, so the split only protected a "Storage without ducts" setup. Merging Servus Lib and Servus: Logistics into one base mod, **Servus: Foundation**, removes that conflict and gives every content mod the ducts. Nothing is released yet, so ids can change for free now and never again as cheaply.

This change is a precondition for the M8 item `m8-data-cable`, which is reworked afterwards into a Data line of the Utility Duct. It adds the item `m8-foundation-merge` at the head of the M8 list in PLAN.md.

## What Changes

- `servus_lib` and `servus_logistics` become one mod: display name "Servus: Foundation", mod id `servus_foundation`, Gradle project `foundation`, Java packages under `io.github.dirnei.mc.servus.foundation`. The `logistics` Gradle project is removed.
- Every registry id, capability id, network type id, lang key, asset and data path of both mods moves to the `servus_foundation` namespace, for example `servus_logistics:utility_duct` becomes `servus_foundation:utility_duct` and `servus_lib:production` becomes `servus_foundation:production`. The common config file becomes `servus_foundation-common.toml`. No aliases or data fixers (unreleased).
- Manufacturing and Storage depend only on Servus: Foundation. The rule "Storage and Logistics depend only on Lib" is replaced by "Manufacturing and Storage depend only on Foundation; they do not depend on each other".
- Main specs under `openspec/specs/lib/` and `openspec/specs/logistics/` move to `openspec/specs/foundation/`, keeping their capability names; the open change `m8-data-cable` moves its delta to `foundation/data-cable`.
- PLAN.md, CLAUDE.md, `openspec/config.yaml`, README, the CI workflow, the docs site pages and its tests are updated to the three mod family.
- Behavior of every block, item, GUI and network stays the same.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `foundation/mod-family-loading` (moved from `lib/mod-family-loading`): three mods instead of four, Foundation is the only shared dependency; the independence rule is replaced by "Manufacturing and Storage are independent".
- `foundation/config` (moved from `lib/config`): the config file name.
- `foundation/production-api` (moved from `lib/production-api`): capability ids in the `servus_foundation` namespace.
- `foundation/wrench` (moved from `lib/wrench`): the creative tab exists with Foundation; old item ids.
- `foundation/utility-duct` (moved from `logistics/utility-duct`): item ids of the lines.
- `docs/balance-lab`: one overview page per mod for Foundation, Manufacturing and Storage.

## Non-goals

- The Data line in the Utility Duct and removing the standalone Data Cable: done by reworking `m8-data-cable` after this change.
- Any behavior, balance or texture change.
- Splitting a separate API only jar.
- Migrating existing dev worlds: blocks and items of the old namespaces disappear from saved worlds.

## Impact

- All Java sources of `lib` and `logistics`, plus imports in `manufacturing` and `storage`.
- Gradle: `settings.gradle`, root `build.gradle` (combined runs, metadata expansion), subproject build files, `gradle.properties` per project, mod metadata templates.
- Resources: `assets/servus_lib`, `assets/servus_logistics`, `data/servus_logistics` merge into `servus_foundation`; generated resources are regenerated.
- GameTests: Logistics GameTests run under the Foundation namespace.
- Docs site (`docs/`): mod pages, game data export paths and tests.
- Dev run directories (`runs/`) of the removed projects; the user's dev worlds lose ducts, tanks, carrots, goggles and cables.
