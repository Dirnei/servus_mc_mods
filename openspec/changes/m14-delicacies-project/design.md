## Context

See proposal.md for the why. The repo builds three mod projects (`foundation`, `alchemy`, `shuttles`) from one root `build.gradle` that holds the shared ModDevGradle setup (`subprojects { ... }`: runs `client`, `server`, `gameTestServer`, `data`, the `neoforge.mods.toml` expansion with `foundation_version_range`) and the combined runs (`clientAll`, `serverAll`, `gameTestServerAll`) over the list `allServusMods`. Each content project adds only its dependencies and `loadedMods = [ownMod, foundationMod]`. Shuttles is the leanest example and the template for `:delicacies`.

## Goals / Non-Goals

**Goals:** a `:delicacies` project that builds, runs its datagen and GameTests green, shows a creative tab in game, and documentation (PLAN.md, CLAUDE.md, openspec config) that names both new mods and the real dependency order.

**Non-Goals:** any shared code in Foundation for the kitchen; see proposal.md, Non-goals.

## Decisions

### Project wiring copies Shuttles
`delicacies/build.gradle` is the Shuttles one without JEI: `evaluationDependsOn(':foundation')`, `compileOnly project(':foundation')` and `testImplementation project(':foundation')`, and the runs load `[ownMod, foundationMod]`. `delicacies/gradle.properties` holds `mod_id`, `mod_name`, `mod_description`, `mod_version=0.1.0`. The mods.toml template is the Shuttles template (Foundation `required`, `ordering="AFTER"`). The root list `allServusMods` gains `delicacies`, so the combined runs and their GameTest namespaces include it. No dev only mods (JEI, Jade, FTB) are added to the Delicacies runs; the combined runs already carry them.

### Own creative tab, after the shared Servus tab
The other content mods fill the shared `servus_foundation:servus` tab through `BuildCreativeModeTabContentsEvent`. Delicacies registers its own tab instead (`DeferredRegister<CreativeModeTab>` for `Registries.CREATIVE_MODE_TAB`, `CreativeModeTab.builder().title(...).icon(...).displayItems(...).withTabsBefore(ServusFoundationCreativeTabs.SERVUS_KEY)`), because a kitchen brings dozens of foods and crops that would bury the machines and tools of the Servus tab, and because a pack that installs only Foundation and Delicacies should find the food in one place. Verified in `minecraft-patched-26.1.2.116-sources.jar`: `CreativeModeTab.Builder#withTabsBefore(ResourceKey<CreativeModeTab>...)` is a NeoForge addition, and `CreativeModeTab#shouldDisplay()` hides a category tab without items, so the tab needs a first item.

### The Pretzel as the first item
The tab needs an item to show and an icon. The Pretzel is the mod's emblem and its sprite is approved (`temp/brezn-smooth.png`), so it is registered now as a plain item (`DeferredRegister.Items#registerSimpleItem`). It gets no food component and no recipe: food values belong in datapack JSON and the bakery (`m14-bakery`) decides how the Pretzel is made and how its values are loaded. The item list of the tab is one ordered list in the items registry class, like `CREATIVE_TAB_ORDER` in the other mods.

### Texture generator
`tools/delicacies-textures/gen.py` holds the Pretzel grid from `temp/brezn-smooth.py` (left half mirrored, seven browns plus the salt white) and writes `delicacies/src/main/resources/assets/servus_delicacies/textures/item/pretzel.png` with the family's own PNG writer (no Pillow), like `tools/potato-texture/gen.py`. Later Delicacies sprites are added to this script.

### Datagen
`ServusDelicaciesDataGen` subscribes to `GatherDataEvent.Client` (client dist, like Foundation) and adds a `LanguageProvider` for `en_us` (tab title `itemGroup.servus_delicacies`, item names) and an item model provider built on Foundation's `datagen.model.ItemModelProvider` (`basicItem`, which also writes the 26.1 item model definition under `assets/<mod>/items/`). Block states, tags, recipes and loot tables join when the first block or recipe exists.

### Smoke GameTest
Tests register through Foundation's `ServusGameTests.register(modBus, MOD_ID, classes)`, which needs the shared `empty_12x6x12` structure in the mod's own namespace, so `data/servus_delicacies/structure/empty_12x6x12.nbt` is copied from Shuttles. `DelicaciesSetupGameTests` checks that Foundation is loaded (`ModList.get().isLoaded`) and that the Delicacies tab, after `CreativeModeTabs.tryRebuildTabContents(enabledFeatures, false, registryAccess)`, lists the Pretzel. There is no pure logic yet, so there are no JUnit tests; the project has no test sources and its `test` task is skipped.

### CI, release and pack
`build.yml` uploads `delicacies/build/libs/*.jar`. `release.yml` adds the `delicacies-v*` tag, the `CURSEFORGE_ID_DELICACIES` variable and Foundation as the required CurseForge dependency, exactly like Shuttles. `server/pack.ps1` packs the Delicacies jar.

### Settles from PLAN.md
No open decision of PLAN.md section 5 is touched. This change writes the M14 and M15 plan from the explore of 2026-10-20 into PLAN.md.

## Assumptions

- Own "Servus: Delicacies" tab rather than the shared Servus tab (see above).
- The Pretzel exists from this change on, as a non edible creative only item; `m14-bakery` makes it food.
- No docs site page, no `branding/` logo and no README icon for Delicacies yet; the README gets a text row.
- The mod description is "An alcohol free Bavarian kitchen: Pretzels, dumplings, roast pork and a tiled stove."
- The Brewery's milestone is written into PLAN.md now, but its project waits for `m15-brewery-project`.

## Risks / Trade-offs

- [The tab rebuild in a GameTest runs on a dedicated server where creative tabs are rarely built] -> `tryRebuildTabContents` is common code with no client types; if it misbehaves the test falls back to checking that the tab and the item are registered.
- [A second tab breaks the "one Servus tab" habit of the family] -> the tab sits right after the Servus tab and carries the family name.

## Migration Plan

None. Nothing is released.
