# Proposal

## Why

Nothing can be built until the four mods compile, run and load together. PLAN.md milestone M0 (Repository and build) is the foundation every later milestone depends on, and it implements the project coordinates (repo, group id, package, license) that are painful to change afterwards.

## What Changes

- Multi project Gradle build with ModDevGradle 2 for NeoForge 21.1.x, Java 21 and Parchment mappings. Subprojects `lib`, `manufacturing`, `logistics`, `storage`.
- Shared versions (Minecraft, NeoForge, Parchment, JUnit, mod version) in the root `gradle.properties`.
- One empty mod main class per mod under `io.github.dirnei.mc.servus.<mod>`, each with its own `neoforge.mods.toml` generated from a template.
- Content mods declare a required dependency on `servus_lib` in their mod metadata and a Gradle project dependency on `:lib`. No content mod depends on another.
- Run configurations per subproject (client, server, data, gameTestServer) that load that mod plus Lib, and combined runs that load all four mods.
- JUnit 5 wired into every subproject with one smoke test so `gradlew test` is exercised from day one.
- GitHub Actions workflow that builds and runs unit tests on every push and pull request.
- MIT `LICENSE` file at the repo root, license recorded in every mod's metadata.
- Repository bootstrap: Gradle wrapper and a `README.md` with build and run instructions.

## Capabilities

### New Capabilities
- `lib/mod-family-loading`: how the Servus mods load in NeoForge: Lib standalone, each content mod together with Lib, all four together, and the dependency rules between them.

### Modified Capabilities
None. There are no existing specs.

## Non-goals

Deferred to later changes:
- Any gameplay content, blocks, items, registries or capabilities (M1 onward).
- The network graph, mailbox and `NetworkManager` (M1).
- Real GameTests. The gameTestServer run config exists, but the first GameTest ships with M2. Without registered tests that run exits with an error, which is expected.
- Data generation providers. The data run config exists, but no providers are registered yet.
- Publishing to Maven, CurseForge or Modrinth.
- Release versioning, changelog and tagging.
- Art, textures, lang files.

## Impact

- New files only. No existing code is touched because there is none.
- Implements the PLAN.md section 1 decisions: repository `github.com/Dirnei/servus_mc_mods`, group id and package `io.github.dirnei.mc.servus`, MIT license.
- The developer machine needs a JDK 21. None is on PATH today, so M0 includes setting one up.
- The GitHub repository does not exist yet. Pushing and verifying the CI run needs the user's GitHub account.
