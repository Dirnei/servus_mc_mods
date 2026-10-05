# Tasks

## 1. Machine

- [ ] 1.1 Add `DepositExtraction`, `ElectricExtractorBlock`, `ElectricExtractorBlockEntity`, `ElectricExtractorMenu`, the `ELECTRIC` layout, registry entries, capabilities, screen registration, stats JSON, textures and GUI, blockstate, loot, tag, lang, `AssemblingRecipeBuilder.tech()` and the gated assembling recipe, and update the tech unlock id. Verify `gradlew build` and `gradlew :manufacturing:runData` pass and the recipe JSON carries the `tech` field.

## 2. Verification

- [ ] 2.1 Server check over RCON: electric extractor on an iron deposit beside a generator produces raw iron every 2 seconds; one on dirt keeps a full buffer. `gradlew :manufacturing:runGameTestServer` passes, `gradlew clean build` green, `gradlew runClientAll` reaches the title screen. Commit.
