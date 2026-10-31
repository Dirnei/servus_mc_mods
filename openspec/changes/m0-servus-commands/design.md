# Design

## Context

All Shuttles commands sit in one class, `shuttles/.../command/ServusShuttlesCommands`, under `/servus_warehouse` (op level 2), and every one takes `BlockPosArgument`s resolved in `source.getLevel()`, so RCON (Overworld source) cannot reach other dimensions. Foundation has `/servus_foundation data move <from> <to> <item> <count>` (`ServusFoundationCommands`), Alchemy `/servus_alchemy journal [finish <chapter>]` (`JournalCommand`, both need a player via `getPlayerOrException`).

Named machines (`foundation/.../naming/NamedMachine`, kinds in `MachineKinds`, ids like `servus_shuttles:field_head`) get a number per kind from their warehouse (`MachineCounters` on the controller). Warehouses have a `warehouseId` UUID on `WarehouseControllerBlockEntity` that travels with a carrot dismantle (`MACHINE_COUNTERS` and the id components) and a `SiteRegistry` (`SavedData`, server wide) with one `SiteRecord` per controller (dimension, pos, name, team, span, upgrades, stock).

The user decided in explore: verb first so every command fits Edict; warehouse number first, then the machine; `here` for the looked at warehouse or machine; the warehouse number visible only in the controller GUI and the list command; one `/servus` root with a branch per mod; numbered `move` under Shuttles, Foundation keeps positions; a general `machine` report; the docs page rendered from the catalogs at the release tags.

## Goals / Non-Goals

**Goals:** usable op commands by number in every dimension and over RCON, one root, a catalog per mod that a test keeps honest, a generated Commands page.

**Non-Goals:** aliases for the old roots (op debug tools, no saved data depends on them); numbers in Potato reports, the Handheld or messages; numbering data line members without a warehouse; publishing the catalogs to the Edict registry (possible later, not needed here); commands for Delicacies and Brewery.

## Decisions

### Warehouse numbers in the SiteRegistry
`SiteRegistry` gains `Map<UUID, Integer> numbers` and `int lastNumber`, saved with it. `update(controller)` and `remember(...)` look up the controller's `warehouseId` and assign `++lastNumber` when it has none; `SiteRecord` gains `number`. Entries are never removed, so a carrot moved controller (same id) finds its number again and a pickaxe broken one (new id) gets a new one. On load, records without a number are numbered sorted by dimension id, then x, y, z. A lookup `byNumber(int)` returns the record. The registry already lives on the server, so numbers are unique in the world.

Alternative: number on the controller like machine counters. Rejected: uniqueness needs one counter for the world, and the registry is that place already.

### Controller GUI
The menu gets the number as menu data (one more data slot), and the site rows already sent to the screen carry it from `SiteRecord`. `WarehouseControllerScreen` draws "W<n> " before the title name and before each site row; the name editor keeps only the name.

### Shared root
Each mod calls `dispatcher.register(Commands.literal("servus").then(Commands.literal(<mod>).requires(...)...))`. Brigadier's `CommandNode.addChild` merges a child with an existing name, so the three registrations build one tree in any load order; the root itself carries no `requires`. Verify the merge in the brigadier sources shipped with 26.1 before relying on it. Foundation, Shuttles and Alchemy each keep their own `RegisterCommandsEvent` subscriber.

### Arguments without new argument types
Custom `ArgumentType`s must be registered and synced to clients, so the commands use vanilla types: `<warehouse>` and machine numbers are `StringArgumentType.word()` with suggestion providers (numbers with names, `here`), kinds are `word()` suggesting the path of the kinds the warehouse has (`field_head`) and accepting the full id. Items stay `ItemArgument`, counts `IntegerArgumentType`, players `EntityArgument.player()`, the Foundation dimension `DimensionArgument`.

### Resolving a warehouse and a machine
A new `shuttles/.../command/CommandTargets`:
- `warehouse(source, token)`: a number via `SiteRegistry.byNumber`, then `server.getLevel(record.dimension())`; `here` via the player's block hit (`player.pick(blockInteractionRange)`), then the warehouse of that block (storage node network, data line member's line, or a Field Head's serving warehouse). Fails with the messages from the spec; `requireLoaded` checks `level.isLoaded(pos)` and the controller block entity and fails pointing at `wake <n>`.
- `machine(warehouse, kind, token)`: walks the warehouse's known members, never the world: the network's nodes (stations, garages, elevators, lockers), the members of its data line (CPUs, Crafting Stations, Adapters, generators, pumps, Alchemy machines), and the Field Heads its fields report. Matches `NamedMachine.machineKind()` and `number()`. `here` takes the looked at block's `NamedMachine` and checks the kind; for `here here` it returns the looked at machine without needing a warehouse.

Reports that only read a warehouse use the loaded controller; commands that act (order, feed, ship, move, take, field age) also require the target machine's chunk to be loaded.

### `take` without a position
It now gives the stack to the running player, or drops it at the controller when the source is not a player (RCON).

### Command layout
`ServusShuttlesCommands` is split by group so no class carries everything: `WarehouseCommands` (list, machines, info, zones, fluids, producers, wake, machine), `FieldCommands`, `StockCommands` (take, feed, ship, move), `OrderCommands` (orders, order, rules, rule, cap, uncap, unrule), sharing `CommandTargets` and the existing report helpers. The handlers keep today's report text where the spec does not change it.

### Risk levels and categories in the catalogs
`safe`: list, machines, info, zones, fluids, producers, machine, field, orders, rules, journal. `moderate`: wake, field recheck, field age, take, feed, ship, move, order, rule, cap, uncap, unrule, data move, journal finish. Nothing is `dangerous` or `destructive`. Categories per mod: Shuttles "Warehouses", "Machines", "Fields", "Stock", "Orders and rules"; Foundation "Data line"; Alchemy "Journal". `remoteExecutable` is true for all; parameters that accept `here` say "not over RCON".

### Catalog files
`<mod>/src/main/resources/edict/<mod_id>.edict.yaml`, so the catalog ships in the jar. `edict: 1.0.0`, `slug: dirnei/servus-<mod>`, `name: "Servus: <Mod>"`, `prefix: "/servus "`, `license` from the mod, `repository` the GitHub repo. Each command key is the full word path after the prefix (`shuttles field age`), which the spec allows ("primary invocation name"); no `subcommands` nesting is needed. The catalog `version` starts at 1.0.0 and is raised (minor for new commands, major for removed or changed ones) in the change that touches commands.

### Catalog check
A GameTest per mod (`CommandCatalogGameTests`) walks `server.getCommands().getDispatcher()` from `servus <mod>` and collects every executable path as fixed words plus argument names, marking arguments after the last executable node as optional. It compares them with the catalog: same command keys, same params in order with the same `required`. YAML is read with SnakeYAML if it is on the dev runtime classpath (check the NeoForge/FML runtime first); otherwise add it only to the GameTest run's classpath, never jar-in-jar it into the release jars. A shared helper in Foundation's gametest package does the walk and the compare so each mod adds only one test method.

### Docs page
- `docs/src/commands.mjs` reads, per mod with a released version (`versions.mjs` already finds it from the CHANGELOG), `git show <mod>-v<version>:<mod>/src/main/resources/edict/<mod_id>.edict.yaml`; a missing file means no section. It parses with the `yaml` npm package and validates with `ajv` (draft 2020-12) against a vendored copy of the Edict schema in `docs/src/edict/catalog.schema.json`; an invalid catalog fails the build.
- `docs/src/components/CommandCatalog.astro` renders per mod and category: the signature built by the Edict rules (words, then `<required>`, `[optional]`), description, a parameter table (name, type, required, description, default, range), risk level, RCON, examples.
- `docs/src/content/docs/reference/commands.mdx` holds only an intro and the component; the sidebar gets it under a Reference group next to the datapack reference.
- `.github/workflows/docs.yml`: checkout with `fetch-depth: 0` so tags are there, and a `workflow_run` trigger on the `Release` workflow's completion, so a pushed release tag rebuilds the page (a `push: tags` trigger would be filtered by the existing `paths`).

## Risks / Trade-offs

- Scripts using `/servus_warehouse` break. Accepted: op debug tools, the user chose no aliases.
- Positions are gone from Shuttles commands; a machine in a warehouse you cannot see is reached through `machines` then its number. `here here` covers unnumbered Field Heads.
- `here` needs the player to look at the block; at long range it fails like vanilla block picking.
- The release tags before this change have no catalogs, so the Commands page stays empty until the first release that ships them.
- Overlaps `m10-rail-corrosion-per-rail` (field command, `ServusShuttlesCommands`); apply after it is archived.
