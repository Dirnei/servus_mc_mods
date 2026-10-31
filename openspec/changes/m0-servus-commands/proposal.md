# Proposal

## Why

The op commands are hard to use: every Shuttles command takes block positions, so a player has to stand at a machine or type coordinates, RCON reaches only the Overworld, and machines are found by position although every named machine already has a number. The commands live under three unrelated roots (`/servus_warehouse`, `/servus_foundation`, `/servus_alchemy`) and are documented nowhere. Warehouses themselves have no number, so a command cannot name one.

## What Changes

- **Warehouse numbers:** every warehouse gets a number unique in the world (W1, W2, ...), never reused, kept when the controller is moved with the Configuration Carrot and new when it is broken any other way. It shows in the Warehouse Controller GUI (title and site list) and in the list command, nowhere else.
- **One root, one branch per mod (BREAKING for scripts):** `/servus shuttles ...`, `/servus foundation ...`, `/servus alchemy ...` replace `/servus_warehouse`, `/servus_foundation` and `/servus_alchemy`. The old roots are removed without aliases.
- **Verb first, numbers instead of positions (BREAKING for scripts):** Shuttles commands name a warehouse by number and a machine by kind and number, for example `/servus shuttles field age 1 2 9000`. `here` stands for the warehouse or machine the player looks at (also an unnumbered one); positions are no longer taken by Shuttles commands.
- **New Shuttles commands:** `list` (warehouses), `machines <warehouse> [kind]`, a general `machine` report for every numbered kind, and `move` (the data line move by machine numbers).
- **Any dimension over RCON:** a warehouse number knows its dimension. A command on a warehouse whose chunks are not loaded says so and points at `wake`.
- **Foundation and Alchemy:** `/servus foundation data move` keeps block positions (Foundation has no warehouse numbers) and gains an optional dimension. `/servus alchemy journal` and `journal finish` take an optional player, so they work over RCON.
- **Edict catalogs:** each mod ships a catalog of its commands in the Edict format (`edict.leberkas.org/spec`). A GameTest fails when a registered command is missing from its catalog or a catalog lists a command that does not exist.
- **Docs Commands page:** rendered from the catalogs read at each mod's release tag, never written by hand. The docs workflow also runs on release tags.

## Capabilities

### New Capabilities
- `foundation/commands`: the shared `/servus` root with one branch per mod, permissions per branch, Edict catalogs per mod and their check, the docs Commands page rendered from the catalogs at the release tags.
- `shuttles/commands`: every Shuttles command, how a warehouse and a machine are named (number or `here`), dimensions, unloaded warehouses and RCON.

### Modified Capabilities
- `shuttles/machine-numbers`: warehouses get a number unique in the world.
- `shuttles/warehouse-controller`: the GUI shows the warehouse number; the command requirements move to `shuttles/commands`.
- `shuttles/crafting-cpu`, `shuttles/field-head`, `shuttles/fluid-rack`, `shuttles/packing-station`, `shuttles/site-wake`, `shuttles/stock-rules`: their command requirements move to `shuttles/commands`.
- `shuttles/outbound-station`, `shuttles/work-orders`: requirements that name a command point at the new command.
- `foundation/data-packets`: the test command moves under `/servus foundation` and takes a dimension.
- `alchemy/journal`: both journal commands move under `/servus alchemy` and take a player.

## Impact

- Code: `shuttles/.../command/ServusShuttlesCommands` rewritten (likely split per command group), `foundation/.../command/ServusFoundationCommands`, `alchemy/.../journal/JournalCommand`, `SiteRegistry` (warehouse numbers), `WarehouseControllerScreen`/menu (number in the title and site rows).
- New resources: `<mod>/src/main/resources/edict/<mod_id>.edict.yaml` for Foundation, Shuttles and Alchemy; a catalog GameTest.
- Docs: a `CommandCatalog` component, a Reference > Commands page, the `yaml` package, a vendored Edict schema, the docs workflow fetching tags and running on release tags.
- Order: apply after `m10-rail-corrosion-per-rail` is archived, which also edits the field command and `ServusShuttlesCommands`. The commands are op tools, so old saves need no migration; warehouses in existing worlds get numbers when they load.
- PLAN.md: no open point yet; the archive adds none unless the change finds one.
