# Tasks

## 1. Plan and rules

- [x] 1.1 `PLAN.md`: Delicacies and Brewery in the mod table and repository layout, vision sections 2.5 Delicacies and 2.6 Brewery, the dependency sentence describing the real order, "Open: M14 Delicacies" (Foundation parts `m14-mill` with Mill, Hand Crank and Electric Motor, `m14-heating-element`; Delicacies changes `m14-crops` to `m14-teller`; later Obatzda, Radler, Biergarten furniture) and "Open: M15 Brewery"; verify no em dash with a grep
- [x] 1.2 `CLAUDE.md` Stack and dependency rule, `openspec/config.yaml` context; verify with `openspec validate m14-delicacies-project`

## 2. Gradle project

- [x] 2.1 `settings.gradle`, `delicacies/gradle.properties`, `delicacies/build.gradle`, `delicacies/src/main/templates/META-INF/neoforge.mods.toml`, `allServusMods` in the root `build.gradle`, the comment in the root `gradle.properties`; mod class `ServusDelicacies` and `ServusDelicaciesIds` in `io.github.dirnei.mc.servus.delicacies`; verify `gradlew build`
- [x] 2.2 `.github/workflows/build.yml` (jar upload), `.github/workflows/release.yml` (`delicacies-v*` tag, CurseForge id and Foundation dependency), `server/pack.ps1`, `README.md`; verify `gradlew build`

## 3. Creative tab, Pretzel and datagen

- [x] 3.1 `tools/delicacies-textures/gen.py` writes `textures/item/pretzel.png` from the approved grid; verify the PNG matches `temp/brezn-smooth.png` pixel for pixel
- [x] 3.2 `registry/ServusDelicaciesItems` (Pretzel, tab order) and `registry/ServusDelicaciesCreativeTabs` (own tab after the Servus tab, Pretzel icon), registered from the mod class; verify `gradlew build`
- [x] 3.3 `datagen/ServusDelicaciesDataGen`, `ServusDelicaciesLanguageProvider` (tab title, Pretzel), `ServusDelicaciesItemModelProvider`; run `gradlew :delicacies:runData`, commit `src/generated/resources`; verify `gradlew build`

## 4. Smoke GameTest

- [x] 4.1 `data/servus_delicacies/structure/empty_12x6x12.nbt`, `gametest/DelicaciesGameTests` (class list) and `gametest/DelicaciesSetupGameTests` (Foundation loaded, tab lists the Pretzel), registered from the mod class; verify `gradlew :delicacies:runGameTestServer` and `gradlew build test`

## 5. Play check

- [ ] 5.1 Play check by the user: the client starts with Foundation, Alchemy, Shuttles and Delicacies, the Mods screen lists "Servus: Delicacies", and the creative inventory shows the "Servus: Delicacies" tab with the Pretzel
