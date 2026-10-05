# Tasks

## 1. Integration

- [x] 1.1 Add the mavens and versions to the root build and `gradle.properties`, the JEI, Jade, FTB Ultimine, FTB Library and Architectury dependencies to `manufacturing/build.gradle`, the JEI plugin and category, and the lang key. Verify `gradlew build` passes and `gradlew :manufacturing:runData` writes the lang entry.
- [x] 1.2 Launch `gradlew runClientAll`. Verify the log shows JEI loading the Servus plugin without errors and Jade, FTB Ultimine, FTB Library and Architectury among the loaded mods. Commit.
