# Design

## Context

Greenfield. The directory holds only `PLAN.md`, `CLAUDE.md`, the OpenSpec setup and a fresh git repository. No JDK is on PATH; the only Java installations are the JetBrains runtimes bundled with Rider (JBR 25) and GoLand (JBR 21.0.9). No `gh` CLI and no GitHub remote yet. See proposal.md for motivation.

Versions verified on 2026-10-05 against the NeoForged Maven, the Gradle plugin portal and the NeoForgeMDKs/MDK-1.21.1-ModDevGradle template:
- NeoForge 21.1.255 (latest 21.1.x)
- ModDevGradle 2.0.148
- Parchment 1.21.1 / 2024.11.17
- Gradle 9.2.1 (wrapper used by the MDK template)
- `org.gradle.toolchains.foojay-resolver-convention` 1.0.0

## Goals / Non-Goals

**Goals:**
- One `gradlew build` compiles and tests all four mods and produces four jars.
- One command launches any mod with Lib, or all four together.
- The layout stays flat and boring: no buildSrc, no convention plugins, no Kotlin DSL.

**Non-Goals:**
- Supporting Fabric, Forge or other Minecraft versions.
- Optimizing Gradle configuration time.
- Publishing artifacts.

## Decisions

### D1: JDK 21 via Temurin installed with winget, foojay for everyone else
Install `EclipseAdoptium.Temurin.21.JDK` with winget on the dev machine. Keep the foojay toolchain resolver in `settings.gradle` so Gradle can auto-provision a JDK 21 toolchain on CI and on other machines.
Alternative: point `JAVA_HOME` at the GoLand JBR 21. Rejected because that runtime disappears on a GoLand update and is not a full JDK.

### D2: ModDevGradle applied per subproject, shared config in the root `subprojects` block
Each of `lib`, `manufacturing`, `logistics`, `storage` sets `mod_id`, `mod_name` and `mod_description` in its own `gradle.properties` (loaded before evaluation, so the root can read them) and has a small `build.gradle` with only what differs: the content mods add `implementation project(':lib')` and set `loadedMods` to themselves plus Lib. The root `build.gradle` holds the `subprojects {}` block with everything shared: the ModDevGradle plugin, Java 21 toolchain, UTF-8, Parchment, NeoForge version from `gradle.properties`, the `mods {}` binding, the four run configs, JUnit 5, the `generateModMetadata` task and `src/generated/resources` as a resource dir. Applied during implementation: the run configs moved from the subprojects into the shared block because all four were identical.
Alternative: a buildSrc convention plugin. Rejected as too much machinery for four nearly identical projects.

### D3: Per mod metadata from a template per subproject
Each subproject has `src/main/templates/META-INF/neoforge.mods.toml`. The shared `generateModMetadata` ProcessResources task expands `mod_id`, `mod_name`, `mod_version`, `mod_license`, `neo_version`, `minecraft_version_range`, `loader_version_range` and `lib_version_range`. Content mod templates add a `[[dependencies.<mod_id>]]` block for `servus_lib`, type `required`, version range `[${mod_version},)`. All four mods share one `mod_version` for now, so the range is simply the current version.
Reference: NeoForge docs, Mod Files (`neoforge.mods.toml`): https://docs.neoforged.net/docs/gettingstarted/modfiles

### D4: Content mods depend on Lib with `implementation project(':lib')`
This puts Lib on the compile and runtime classpath. Because ModDevGradle only loads mods listed in `mods {}` or `loadedMods`, each content mod's runs set `loadedMods = [mods.<self>, project(':lib').neoForge.mods.servus_lib]`. Reference: ModDevGradle README, "Include Mods from Subprojects".

### D5: Combined runs live in the root project
The root project also applies ModDevGradle (with `neoForge.version` set, no sources) and defines `clientAll`, `serverAll` and `gameTestServerAll` runs whose `loadedMods` lists all four mods. The root uses `evaluationDependsOnChildren()` so the subproject `mods` are available. The root `jar` task is disabled because the root has no sources.
Alternative: put the combined runs in `lib`. Rejected because Lib would then reference the content projects in its build script, which inverts the dependency direction we want to keep clean.
Risk noted below in case ModDevGradle rejects a run in a project without a `mods {}` block.

### D6: Run config shape per subproject
`client`, `server` (with `--nogui`), `data` and `gameTestServer`, same as the MDK template. `neoforge.enabledGameTestNamespaces` is set to the mod's id. The `data` run passes `--mod <id> --all --output src/generated/resources --existing src/main/resources`. Game directories: `runs/client`, `runs/server`, `runs/data`, `runs/gametest` under each subproject, all gitignored.

### D7: Empty mod main classes
One class per mod, annotated `@net.neoforged.fml.common.Mod("<mod_id>")`, with a `LOGGER` and a constructor taking `IEventBus modEventBus` that logs one line. Nothing else. Reference: NeoForge docs, Getting Started, "Mod entrypoint": https://docs.neoforged.net/docs/gettingstarted/modfiles#mod-entrypoint
Packages: `io.github.dirnei.mc.servus.lib`, `io.github.dirnei.mc.servus.manufacturing`, `io.github.dirnei.mc.servus.logistics`, `io.github.dirnei.mc.servus.storage`.

### D8: JUnit 5 in every subproject with a smoke test
`testImplementation platform("org.junit:junit-bom:5.11.4")`, `junit-jupiter`, `junit-platform-launcher`, `useJUnitPlatform()`. Each subproject gets one test asserting the mod id constant, so a broken test setup is caught immediately. These tests are pure Java (the constant lives in a plain `*Ids` class without Minecraft imports) and run without a game.

### D9: GitHub Actions
`.github/workflows/build.yml`: triggers on push and pull_request, runs on ubuntu-latest, `actions/checkout@v4`, `actions/setup-java@v4` with Temurin 21, `gradle/actions/setup-gradle@v4`, then `./gradlew build`. `build` already includes `test`. Jars uploaded as a workflow artifact for convenience.

### D10: Gradle wrapper bootstrapped from the MDK template
No Gradle is installed locally, so the wrapper jar, scripts and properties are copied from the MDK-1.21.1-ModDevGradle template (Gradle 9.2.1). After the first successful build, `gradlew wrapper` regenerates them from the configured version.

## Pure Java vs Minecraft

- Pure Java, JUnit tested: the `*Ids` constants classes and their smoke tests.
- Touches Minecraft, verified manually: mod main classes, mod metadata, run configurations. Verified by launching the server runs and reading the mod list in the log, and by launching the client once and opening the Mods screen. No GameTests in M0.

## PLAN.md decisions settled

- Repository, group id, package and license were settled before this change and are recorded in PLAN.md section 1. This change implements them.
- The repository layout in PLAN.md section 1 gains `runs/` directories per subproject and a `.github/` directory. PLAN.md is updated in this change.

## Risks / Trade-offs

- [ModDevGradle may require a `mods {}` entry in the root project for the combined runs] -> Fallback: define the combined runs in `lib` with `evaluationDependsOn` on the content projects. Decide at apply time, record in this design.
- [gameTestServer runs exit with an error when no tests are registered] -> Accepted for M0. The runs exist so M2 can add the first GameTest without build changes.
- [Gradle 9 and ModDevGradle 2.0.148 are newer than most online examples] -> Stick to the MDK template shapes, which are tested against exactly these versions.
- [Single shared `mod_version` means the Lib dependency range is strict] -> Fine while all mods release together. Revisit when mods get independent versions.
- [CI cannot be verified without a GitHub remote] -> The workflow file is written and validated locally with `gradlew build`. The first real run happens when the user pushes.

## Open Questions

None that affect the tasks. The GitHub remote creation is a user action, not a question.
