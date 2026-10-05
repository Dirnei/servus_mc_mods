# Tasks

## 1. Registries and datagen plumbing

- [x] 1.1 Change `ServusLib` to `(IEventBus, ModContainer)`, add `ServusLibItems` (wrench), `ServusLibCreativeTabs` (Servus tab with public key), the placeholder texture `lib/src/main/resources/assets/servus_lib/textures/item/wrench.png`, and `ServusLibDataGen` with `ServusLibLanguageProvider` and `ServusLibItemModelProvider`. Verify `gradlew :lib:runData` writes `lib/src/generated/resources/assets/servus_lib/lang/en_us.json` and `models/item/wrench.json`.

## 2. Wrench and config

- [x] 2.1 Add `Wrenchable`, `WrenchItem` (Wrenchable first, then facing cycle, sneak passes) and `ServusLibConfig` with `wrenchRotatesBlocks`, registered as common config in `ServusLib`. Verify `gradlew build` passes.
- [x] 2.2 Add `ServusLibRecipeProvider` with the wrench recipe. Verify `gradlew :lib:runData` writes `data/servus_lib/recipe/wrench.json` and the advancement, and `gradlew build` passes.

## 3. Verification

- [x] 3.1 Full check: `gradlew clean build` green, `gradlew runClientAll` reaches the title screen with no missing texture or model errors for `servus_lib:wrench` in the log, and a server run creates `runs/server/config/servus_lib-common.toml` containing `wrenchRotatesBlocks = true`. Commit.
