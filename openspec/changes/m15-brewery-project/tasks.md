# Tasks

## 1. Rules and layout

- [x] 1.1 `CLAUDE.md` Stack lines and `openspec/config.yaml` context name `brewery` and `servus_brewery` without "later"; `PLAN.md` repository layout gains `brewery/` and the package sentence drops "later"; verify no em dash with a grep and `openspec validate m15-brewery-project`

## 2. Gradle project

- [x] 2.1 `settings.gradle`, `brewery/gradle.properties`, `brewery/build.gradle` (Foundation and Delicacies), `brewery/src/main/templates/META-INF/neoforge.mods.toml` (Foundation and Delicacies required), `delicacies_min_version` in the root `gradle.properties`, `delicacies_version_range` and `allServusMods` in the root `build.gradle`; mod class `ServusBrewery` and `ServusBreweryIds` in `io.github.dirnei.mc.servus.brewery`; verify `gradlew build`
- [x] 2.2 `.github/workflows/build.yml` (jar upload), `.github/workflows/release.yml` (`brewery-v*` tag, `CURSEFORGE_ID_BREWERY`, Foundation and Delicacies dependencies), `server/pack.ps1`, `README.md`; verify `gradlew build`

## 3. Creative tab, Beer Glass and datagen

- [x] 3.1 `tools/brewery-textures/gen.py` writes `textures/item/beer_glass.png` from the ported `mug(None, ...)`; verify the PNG matches the mockup's `halbe_glas` sprite pixel for pixel
- [x] 3.2 `registry/ServusBreweryItems` (Beer Glass, tab order) and `registry/ServusBreweryCreativeTabs` (own tab after the Delicacies tab, Beer Glass icon), registered from the mod class; verify `gradlew build`
- [x] 3.3 `datagen/ServusBreweryDataGen`, `ServusBreweryLanguageProvider` (tab title, Beer Glass), `ServusBreweryItemModelProvider`; run `gradlew :brewery:runData`, commit `src/generated/resources`; verify `gradlew build`

## 4. Smoke GameTests

- [x] 4.1 `data/servus_brewery/structure/empty_12x6x12.nbt`, `gametest/BreweryGameTests` (class list) and `gametest/BrewerySetupGameTests` (Foundation and Delicacies loaded, tab lists the Beer Glass), registered from the mod class; verify `gradlew :brewery:runGameTestServer` and `gradlew build test`

## 5. Play check

- [ ] 5.1 Play check by the user: the client starts with all five Servus mods, the Mods screen lists "Servus: Brewery", and the creative inventory shows the "Servus: Brewery" tab after the Delicacies tab with the Beer Glass
