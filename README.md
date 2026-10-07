# Servus mod family

A family of NeoForge mods for Minecraft 1.21.1. See `PLAN.md` for the design and roadmap.

| Mod | Mod ID | Folder |
|---|---|---|
| Servus Lib | `servus_lib` | `lib/` |
| Servus: Manufacturing | `servus_manufacturing` | `manufacturing/` |
| Servus: Logistics | `servus_logistics` | `logistics/` |
| Servus: Storage | `servus_storage` | `storage/` |

Storage and Logistics depend only on Servus Lib. Manufacturing also depends on Servus: Logistics for its ducts.

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
3. Coal Generator plus Utility Ducts with a Power Cable (from Servus: Logistics) carry FE to an Assembler Mk1 and a Research Lab.
4. In the Assembler, lay out a recipe in the pattern grid and press plus, or press the plus button on any recipe in JEI. An Assembler Mk1 holds two recipes and alternates between them. Every machine GUI has a side panel on the left to set which sides take input and give output.
5. Utility Ducts with an Item Pipe route pushed items to the next inventory on the same channel.
6. Red packs go into the Research Lab; open the Tech Tree from its GUI and pick a tech.
7. Researched machines are assembled, not hand crafted: Electric Extractor (twice as fast, powered), Crusher (ore to two grit, grit smelts to ingots). Stone Processing adds stone, stone bricks and glass recipes to the assembler.
8. The Chunk Loader keeps its chunk running while powered. The per team limit and the FTB Chunks rule are in `config/servus_manufacturing-common.toml`.

## Storage playtest guide

1. Crate: planks around a chest. Right click with items to fill it (double right click empties your inventory of that item), left click to take a stack, sneak left click for one. It shows its item and count on the front.
2. Shelving Rack: planks and sticks. Four compartments on the front; right click a compartment to put the held stack there, right click with an empty hand to take it.
3. Warehouse Controller: iron, redstone, chest and planks. Touching crates, racks and terminals form a warehouse; chests and machines touching any of them are indexed too. Exactly one controller per warehouse. Right click it for a summary.
4. Counter Terminal: glass, iron, chest and planks. Opens the stock view: type to search, click to pull a stack to the cursor, shift click to pull into the inventory, click the grid with a carried stack or shift click an inventory slot to store.
5. Ops can inspect any warehouse with `/servus_warehouse info <x> <y> <z>` on a storage block.

## Logistics playtest guide

1. Item Pipe (iron, glass, chest) and Power Cable (copper, redstone, glass) place a Utility Duct carrying that line. Use a line item on an existing duct to add the other line. A duct with one line is thin, with both lines thicker, and each line shows its own color.
2. Use the Configuration Carrot on an arm that touches a chest or machine to open that side's configuration: per line a mode (in and out, pull in, push out, disabled), one of eight colored channels (items and energy only flow between sides on the same channel; the ring at the arm's end shows the color), and for the Item Pipe up to nine filter items (empty means everything) and a priority. The carrot on the centre, on an arm toward another duct or a bare hand opens nothing.
3. Facades: craft any full block with an iron ingot for six facades, use one on a duct and it looks like that block. Sneak wrench takes the facade off first, a second sneak wrench removes the duct and drops its channels.
4. Engineer Goggles (leather, iron, glass) go on your head. While wearing them, or holding the wrench, hidden ducts show through walls as an orange overlay.

The wrench rotates machines and sneak wrench dismantles them. Balance numbers live in `manufacturing/src/main/resources/data/servus_manufacturing/machine/` and the deposit, recipe and tech JSON files next to them.

## Development workflow

Work is planned and tracked with OpenSpec. See `CLAUDE.md` for the spec-driven workflow and `openspec/` for the specs and changes.

## License

MIT, see `LICENSE`.
