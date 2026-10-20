## Why

The family gets a fourth mod, Servus: Delicacies, an alcohol free Bavarian kitchen (PLAN.md, M14 Delicacies, `m14-delicacies-project`). Every later M14 change (crops, Tiled Stove, bakery, dishes, Teller) needs a project to land in, and PLAN.md and CLAUDE.md must describe the new mods and the dependency order before that work starts.

## What Changes

- New Gradle project `:delicacies` for Servus: Delicacies, mod id `servus_delicacies`, package `io.github.dirnei.mc.servus.delicacies`, wired like `alchemy` and `shuttles`: `settings.gradle`, its own `gradle.properties` and `build.gradle`, a `neoforge.mods.toml` template with a required dependency on `servus_foundation`, its own client, server, data and GameTest runs, and a place in the combined runs of the root project.
- Mod class, the "Servus: Delicacies" creative tab with the Pretzel as its first item and icon (the approved Pretzel sprite, drawn by `tools/delicacies-textures/gen.py`), an `en_us` lang file and the item model through datagen, and a smoke GameTest.
- CI uploads the Delicacies jar, the Release workflow accepts `delicacies-v*` tags with Foundation as a required dependency, and the play test pack script packs the jar.
- `PLAN.md`: Servus: Delicacies and Servus: Brewery in the mod table, a vision section for each, the real dependency order (Brewery depends on Foundation and Delicacies), and the open milestones M14 Delicacies and M15 Brewery with their planned changes, the Foundation parts (Mill, Hand Crank, Electric Motor, Heating Element) and the later points Obatzda, Radler and Biergarten furniture.
- `CLAUDE.md` and `openspec/config.yaml`: the Stack lists the new project and mod id, and the dependency rule describes the real order.

## Capabilities

### New Capabilities
- `delicacies/mod-setup`: Servus: Delicacies loads with Foundation, shows its own creative tab and names its content in English.

### Modified Capabilities
- `foundation/mod-family-loading`: the family that loads together, the content mods that each load with Foundation alone, and the independence rule now include Servus: Delicacies.

## Non-goals

- The Brewery project (`m15-brewery-project`) and any Brewery content.
- Any Foundation content: Mill, Hand Crank, Electric Motor (`m14-mill`) and Heating Element (`m14-heating-element`).
- Crops, the Tiled Stove, recipes and food values: the Pretzel has no recipe and cannot be eaten yet; `m14-bakery` makes it a food.
- A docs site page, a branding logo and the docs game data export for Delicacies.

## Impact

- New: `delicacies/` (build files, sources, generated resources, GameTest structure), `tools/delicacies-textures/gen.py`.
- Changed: `settings.gradle`, root `build.gradle` (combined runs), root `gradle.properties` (comment), `.github/workflows/build.yml`, `.github/workflows/release.yml`, `server/pack.ps1`, `README.md`, `PLAN.md`, `CLAUDE.md`, `openspec/config.yaml`.
- No Foundation, Alchemy or Shuttles source changes.
