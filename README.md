# Servus mod family

A family of NeoForge mods for Minecraft 1.21.1. See `PLAN.md` for the design and roadmap.

| Mod | Mod ID | Folder |
|---|---|---|
| Servus Lib | `servus_lib` | `lib/` |
| Servus: Manufacturing | `servus_manufacturing` | `manufacturing/` |
| Servus: Logistics | `servus_logistics` | `logistics/` |
| Servus: Storage | `servus_storage` | `storage/` |

Each content mod depends only on Servus Lib. No content mod depends on another.

## Prerequisites

- JDK 21 (Temurin recommended). Gradle also auto provisions a toolchain through the foojay resolver.
- Nothing else. The Gradle wrapper downloads Gradle, NeoForge and Minecraft on first use.

## Build and test

```
gradlew build          # compiles all four mods, runs unit tests, writes jars to <mod>/build/libs
gradlew test           # unit tests only
gradlew clean build    # from scratch
```

## Run

Each mod project has its own run configurations. Content mod runs always load Servus Lib as well.

```
gradlew :lib:runClient               # Lib alone
gradlew :manufacturing:runClient     # Manufacturing plus Lib
gradlew :logistics:runServer         # Logistics plus Lib, dedicated server
gradlew :storage:runData             # data generation for Storage
gradlew :storage:runGameTestServer   # game tests for Storage (exits with an error until tests exist)
```

Combined runs with all four mods are defined in the root project:

```
gradlew runClientAll
gradlew runServerAll
gradlew runGameTestServerAll
```

Game directories are `runs/client`, `runs/server`, `runs/data` and `runs/gametest` inside each project (or the repo root for the combined runs). They are gitignored. Dedicated server runs need `eula=true` in `runs/server/eula.txt`.

## Development workflow

Work is planned and tracked with OpenSpec. See `CLAUDE.md` for the spec-driven workflow and `openspec/` for the specs and changes.

## License

MIT, see `LICENSE`.
