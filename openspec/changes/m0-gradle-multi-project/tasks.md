# Tasks

## 1. Toolchain

- [x] 1.1 Install Temurin JDK 21 with `winget install EclipseAdoptium.Temurin.21.JDK` and verify `java -version` reports 21 in a fresh shell.
- [x] 1.2 Bootstrap the Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties` for Gradle 9.2.1) from the MDK-1.21.1-ModDevGradle template and verify `gradlew --version` runs.

## 2. Build skeleton

- [x] 2.1 Write `settings.gradle` (foojay resolver 1.0.0, root project name `servus_mc_mods`, include `lib`, `manufacturing`, `logistics`, `storage`) and the root `gradle.properties` with shared versions (NeoForge 21.1.255, Parchment 1.21.1 / 2024.11.17, Minecraft 1.21.1, JUnit 5.11.4, mod version 0.1.0, license MIT, loader range) and Gradle JVM args. Verify `gradlew projects` lists the four subprojects.
- [x] 2.2 Write the root `build.gradle` with the `subprojects {}` block: ModDevGradle 2.0.148, Java 21 toolchain, UTF-8, Parchment, NeoForge version, `src/generated/resources`, JUnit 5 with `useJUnitPlatform()`, shared `generateModMetadata` task wired via `neoForge.ideSyncTask`. Verify `gradlew help` configures without errors.
- [x] 2.3 Write `lib/build.gradle` with mod id `servus_lib`, name `Servus Lib`, the `mods {}` binding and runs `client`, `server`, `data`, `gameTestServer` under `runs/`. Add `lib/src/main/templates/META-INF/neoforge.mods.toml`. Verify `gradlew :lib:generateModMetadata` produces a toml with the expanded values.
- [x] 2.4 Write `manufacturing/build.gradle`, `logistics/build.gradle`, `storage/build.gradle` with their mod ids, `implementation project(':lib')`, runs whose `loadedMods` include Lib, and templates declaring `servus_lib` as a required dependency. Verify `gradlew generateModMetadata` succeeds for all three and each toml contains the Lib dependency block.

## 3. Mod classes and tests

- [x] 3.1 Add `io.github.dirnei.mc.servus.lib.ServusLib` (`@Mod`, logger, constructor logging one line) plus the pure constants class `ServusLibIds` with `MOD_ID`. Add JUnit test `ServusLibIdsTest` asserting `MOD_ID` equals `servus_lib`. Verify `gradlew :lib:test` passes.
- [x] 3.2 Add `ServusManufacturing`, `ServusLogistics`, `ServusStorage` main classes and their `*Ids` constants classes in their packages, each with a JUnit test asserting the mod id. Verify `gradlew test` passes in all four projects and `gradlew build` produces `lib/build/libs/servus_lib-0.1.0.jar` and the three content jars.

## 4. Combined runs and repository files

- [x] 4.1 Add the root project ModDevGradle setup with `clientAll`, `serverAll`, `gameTestServerAll` runs loading all four mods, `evaluationDependsOnChildren()` and the root `jar` task disabled. If ModDevGradle rejects runs without a `mods {}` block, move the combined runs to `lib` as described in design.md D5 and update the design. Verify `gradlew tasks --group "mod development"` lists the combined runs.
- [x] 4.2 Add `LICENSE` (MIT, copyright 2026 Dirnei), `README.md` (prerequisites, build, run commands, run configs) and `.github/workflows/build.yml` (push and pull_request, Temurin 21, setup-gradle, `./gradlew build`, upload jars). Verify `gradlew build` still passes and the workflow YAML parses.
- [x] 4.3 Update `PLAN.md` section 1 repository layout to show `runs/` per subproject, `.github/` and the wrapper files. Verify the layout matches `git ls-files`.

## 5. Verification

- [ ] 5.1 Run `gradlew :lib:runServer`, `gradlew :manufacturing:runServer`, `gradlew :logistics:runServer`, `gradlew :storage:runServer` once each (accept the EULA in `runs/server/eula.txt` first). Verify the log shows exactly the expected mod ids loaded (Lib alone, then Lib plus one content mod) and the server reaches "Done". Stop each server.
- [ ] 5.2 Run `gradlew serverAll`. Verify the log lists all four Servus mod ids and the server reaches "Done".
- [ ] 5.3 Full check: `gradlew clean build` green including tests, then `gradlew clientAll` launches the client to the title screen and the Mods screen shows all four mods with version 0.1.0 and license MIT. Commit.
