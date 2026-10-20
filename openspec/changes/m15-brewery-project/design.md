## Context

See proposal.md for the why. The repo builds four mod projects from one root `build.gradle` that holds the shared ModDevGradle setup (`subprojects { ... }`: runs `client`, `server`, `gameTestServer`, `data`, the `neoforge.mods.toml` expansion with `foundation_version_range`) and the combined runs (`clientAll`, `serverAll`, `gameTestServerAll`) over the list `allServusMods`. `m14-delicacies-project` added `:delicacies` as the leanest content project; `:brewery` copies it and adds Delicacies as a second dependency.

## Goals / Non-Goals

**Goals:** a `:brewery` project that builds, runs its datagen and GameTests green, shows a creative tab in game, refuses to start without Delicacies, and documentation (CLAUDE.md, openspec config, PLAN.md repository layout) that names it as an existing project.

**Non-Goals:** any brewing content or use of the Delicacies firebox; see proposal.md, Non-goals.

## Decisions

### Project wiring copies Delicacies, plus Delicacies as a dependency
`brewery/build.gradle` is the Delicacies one with a second project: `evaluationDependsOn(':foundation')` and `evaluationDependsOn(':delicacies')`, `compileOnly` and `testImplementation` on both projects, and the runs load `[ownMod, foundationMod, delicaciesMod]`. `compileOnly` and not `implementation` for the same reason as with Foundation: the runs load Delicacies from its source set through `loadedMods`, and an implementation jar on the run classpath would make FML see `servus_delicacies` twice. `brewery/gradle.properties` holds `mod_id`, `mod_name`, `mod_description`, `mod_version=0.1.0`. The root list `allServusMods` gains `brewery` (after `delicacies`), so the combined runs and their GameTest namespaces include it. `settings.gradle` includes `brewery` after `delicacies`.

### Delicacies version range
The mods.toml template is the Delicacies template plus a third Servus entry: `servus_delicacies`, `type="required"`, `versionRange="${delicacies_version_range}"`, `ordering="AFTER"`, `side="BOTH"`. The range is expanded by the shared `generateModMetadata` task of the root `build.gradle` from a new root property `delicacies_min_version=0.1.0-alpha` as `[${delicacies_min_version},)`, exactly like `foundation_version_range`. The property sits in the shared expansion map so every project sees it; only the Brewery template uses it. A missing required dependency makes FML refuse to load with a "missing dependency" error naming the mod, the same mechanism the spec already relies on for Foundation.

### Own creative tab, after the Delicacies tab
Like Delicacies, Brewery registers its own tab (`DeferredRegister<CreativeModeTab>` for `Registries.CREATIVE_MODE_TAB`, `CreativeModeTab.builder().title(...).icon(...).displayItems(...).withTabsBefore(ServusDelicaciesCreativeTabs.DELICACIES_KEY)`), so the drink chain sits in one place and right after the food. `withTabsBefore` is the NeoForge addition already used by Delicacies (verified there in `minecraft-patched-26.1.2.116-sources.jar`); a category tab without items is hidden by `CreativeModeTab#shouldDisplay()`, so the tab needs a first item. The item list is one ordered list `CREATIVE_TAB_ORDER` in the items registry class.

### The Beer Glass as the first item
Every beer of M15 is served in the half litre glass and drinking gives it back, so the empty glass is the natural first item and icon. It is registered now as a plain item (`DeferredRegister.Items#registerSimpleItem("beer_glass")`), stacking to 64 like a vanilla glass bottle, named "Beer Glass". It has no recipe and no use; `m15-beer` decides how it is made and filled. The Weissbier glass is a separate item of `m15-beer`.

### Texture generator
`tools/brewery-textures/gen.py` ports `mug(None, ...)` from `temp/delicacies-mockup-src/sprites.py` (the approved `halbe_glas` sprite: glass body `x 3..10, y 4..14` in translucent glass blue with a darker edge, a grid of white glints, and a handle ring on the right) into plain Python without Pillow, with the same PNG writer and `--preview` option as `tools/delicacies-textures/gen.py`. The empty glass uses no random noise, so the port is exact. It writes `brewery/src/main/resources/assets/servus_brewery/textures/item/beer_glass.png`. Later Brewery sprites (Helles, Dunkles, Weissbier, barley, hops, malt) are added to this script. The masks (`rect`, `capsule`, `boundary`) are copied in as small helpers, since the family has no shared Python module for the tools.

### Datagen
`ServusBreweryDataGen` subscribes to `GatherDataEvent.Client` (client dist, like Delicacies) and adds a `LanguageProvider` for `en_us` (tab title `itemGroup.servus_brewery`, item names) and an item model provider built on Foundation's `datagen.model.ItemModelProvider` (`basicItem`, which also writes the 26.1 item model definition under `assets/<mod>/items/`). No render type is set for the glass: in 26.1 `BakedQuad.MaterialInfo.of` picks `Sheets.translucentItemSheet()` when the sprite's `Transparency` has translucent pixels (verified in `minecraft-patched-26.1.2.116-sources.jar`), so the half transparent glass pixels render as glass.

### Smoke GameTests
Tests register through Foundation's `ServusGameTests.register(modBus, MOD_ID, classes)`, which needs the shared `empty_12x6x12` structure in the mod's own namespace, so `data/servus_brewery/structure/empty_12x6x12.nbt` is copied from Delicacies. `BrewerySetupGameTests` checks that Foundation and Delicacies are loaded (`ModList.get().isLoaded` with `ServusFoundationIds.MOD_ID` and `ServusDelicaciesIds.MOD_ID`) and that the Brewery tab, after `CreativeModeTabs.tryRebuildTabContents(enabledFeatures, false, registryAccess)`, lists the Beer Glass. The refusal without Delicacies is checked by FML at load and is not testable in a GameTest; it is covered by the mods.toml and the play check. There is no pure logic yet, so there are no JUnit tests.

### CI, release and pack
`build.yml` uploads `brewery/build/libs/*.jar`. `release.yml` adds the `brewery-v*` tag, the `CURSEFORGE_ID_BREWERY` variable and `servus_foundation(required){curseforge:$ID_FOUNDATION}, servus_delicacies(required){curseforge:$ID_DELICACIES}` as CurseForge dependencies (mc-publish takes a comma or newline separated list). `server/pack.ps1` packs the Brewery jar. The docs game data export stays as it is.

### Settles from PLAN.md
No open decision of PLAN.md section 5 is touched. PLAN.md changes only in the repository layout (a `brewery/` line) and the package sentence (`brewery` without "later").

## Assumptions

- The first item is named "Beer Glass" with the id `beer_glass`; it is the empty glass for Helles and Dunkles.
- The Brewery tab sits after the Delicacies tab rather than after the shared Servus tab.
- `delicacies_min_version` starts at `0.1.0-alpha`, the same floor as Foundation.
- The mod description is "Bavarian beer from the field to the glass: barley, hops, a brewhouse and casks."
- The `foundation/mod-family-loading` delta is written on top of the state `m14-delicacies-project` leaves (that change is not archived yet and modifies the same requirements); it repeats the Delicacies wording, so either archive order ends in the same spec.
- No docs site page, no `branding/` logo and no README icon for Brewery yet; the README gets a text row.

## Risks / Trade-offs

- [Two unarchived changes modify `foundation/mod-family-loading`] -> the Brewery delta contains the full text including Delicacies, so the later archive wins with a consistent result.
- [The Brewery runs load three mods from source sets] -> the same `loadedMods` mechanism the combined runs already use for all mods.

## Migration Plan

None. Nothing is released.
