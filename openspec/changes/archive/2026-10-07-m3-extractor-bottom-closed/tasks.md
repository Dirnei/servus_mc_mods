# Tasks

## 1. Closed bottom

- [x] 1.1 Lock the bottom in `machine/extractor/BurnerExtractorBlockEntity.java` and `ElectricExtractorBlockEntity.java`, add a GameTest in `gametest/ServusManufacturingGameTests.java`: cycling the bottom keeps it Off and a hopper below receives nothing. Verify `gradlew build` and `gradlew :manufacturing:runGameTestServer`.

## 2. Verification

- [x] 2.1 Run `openspec validate m3-extractor-bottom-closed --strict`, full `gradlew build` and `gradlew test` green, `gradlew runClientAll` launches with all four mods. Commit.
- [x] 2.2 Play check: the extractor's bottom cell is greyed, the carrot does not change it, no marker on the bottom.
