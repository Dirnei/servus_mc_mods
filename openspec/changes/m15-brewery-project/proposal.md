## Why

The family gets a fifth mod, Servus: Brewery, Bavarian beer as a mod of its own so packs can leave alcohol out (PLAN.md, Open: M15 Brewery, `m15-brewery-project`). Every later M15 change (crops, Brewhouse, malting, beer, Rausch) needs a project to land in, wired so that it builds on Foundation and Delicacies, whose firebox the Brewhouse reuses.

## What Changes

- New Gradle project `:brewery` for Servus: Brewery, mod id `servus_brewery`, package `io.github.dirnei.mc.servus.brewery`, wired like `:delicacies`: `settings.gradle`, its own `gradle.properties` and `build.gradle`, a `neoforge.mods.toml` template with required dependencies on `servus_foundation` and `servus_delicacies`, a Gradle project dependency on `:delicacies`, its own client, server, data and GameTest runs that load Foundation and Delicacies, and a place in the combined runs of the root project.
- A shared lower bound for the Delicacies version that Brewery accepts (`delicacies_min_version` in the root `gradle.properties`, expanded into the mods.toml like the Foundation range).
- Mod class, the "Servus: Brewery" creative tab with the empty half litre Beer Glass as its first item and icon (the approved `halbe_glas` sprite of the mockup, drawn by the new `tools/brewery-textures/gen.py`), an `en_us` lang file and the item model through datagen, and smoke GameTests.
- CI uploads the Brewery jar, the Release workflow accepts `brewery-v*` tags with the `CURSEFORGE_ID_BREWERY` variable and Foundation and Delicacies as required dependencies, the play test pack script packs the jar, and the README lists the mod.
- `CLAUDE.md`, `openspec/config.yaml` and the repository layout in `PLAN.md` name `brewery` and `servus_brewery` as an existing project instead of a later one.

## Capabilities

### New Capabilities
- `brewery/mod-setup`: Servus: Brewery loads with Foundation and Delicacies, shows its own creative tab with the Beer Glass and names its content in English.

### Modified Capabilities
- `foundation/mod-family-loading`: the family that loads together now has five mods, Brewery requires Delicacies besides Foundation and refuses to start without it, and the independence rule names Brewery's place in the dependency order.

## Non-goals

- Barley, hops and the hop pole (`m15-crops`), the Brewhouse (`m15-brewhouse`), the Malting Floor and malt (`m15-malting`), Yeast, the Fermenting Vat, the Cask and the beers (`m15-beer`), the Rausch (`m15-rausch`).
- Any use of Delicacies code beyond its mod id: the firebox is reused by `m15-brewhouse`.
- A recipe for the Beer Glass and filling it: the glass is a plain creative only item until `m15-beer`.
- A docs site page, a branding logo and the docs game data export for Brewery.
- The rest of `PLAN.md` (milestones are groomed when the change is archived).

## Impact

- New: `brewery/` (build files, sources, generated resources, GameTest structure), `tools/brewery-textures/gen.py`.
- Changed: `settings.gradle`, root `build.gradle` (combined runs, Delicacies version range), root `gradle.properties`, `.github/workflows/build.yml`, `.github/workflows/release.yml`, `server/pack.ps1`, `README.md`, `CLAUDE.md`, `openspec/config.yaml`, `PLAN.md` (repository layout and package list only).
- No Foundation, Alchemy, Shuttles or Delicacies source changes.
