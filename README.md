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

## Playtest (Tier 1)

Launch with `gradlew runClientAll`, create a new world, and take the Servus creative tab or craft from vanilla materials:

1. Find or place deposits (iron, copper, coal, stone patches on the surface).
2. Burner Extractor on a deposit, Burner Smelter next to it, coal in both. Machines push output into adjacent inventories.
3. Coal Generator plus Power Cables carry FE to an Assembler Mk1 and a Research Lab.
4. In the Assembler, lay out a recipe in the pattern grid and press plus, or press the plus button on any recipe in JEI. An Assembler Mk1 holds two recipes and alternates between them. Every machine GUI has a side panel on the left to set which sides take input and give output.
5. Item Ducts route pushed items to the next inventory on the duct network.
6. Red packs go into the Research Lab; open the Tech Tree from its GUI and pick a tech.
7. Researched machines are assembled, not hand crafted: Electric Extractor (twice as fast, powered), Crusher (ore to two grit, grit smelts to ingots), Splitter Duct (pulls from chests into the duct network) and Filter Duct (whitelist in its GUI). Stone Processing adds stone, stone bricks and glass recipes to the assembler.
8. The Chunk Loader keeps its chunk running while powered. The per team limit and the FTB Chunks rule are in `config/servus_manufacturing-common.toml`.

The wrench rotates machines and sneak wrench dismantles them. Balance numbers live in `manufacturing/src/main/resources/data/servus_manufacturing/machine/` and the deposit, recipe and tech JSON files next to them.

## Development workflow

Work is planned and tracked with OpenSpec. See `CLAUDE.md` for the spec-driven workflow and `openspec/` for the specs and changes.

## License

MIT, see `LICENSE`.
