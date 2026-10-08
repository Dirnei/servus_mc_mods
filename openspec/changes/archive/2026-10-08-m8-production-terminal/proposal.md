# Proposal

## Why

The Crafting CPU change removed the Order Board, so orders and machines are only visible through `/servus_warehouse orders` and `producers`. PLAN.md M8 item 6 (`m8-production-terminal`) adds their replacement on the data line: a Production Terminal with a Machines tab and an Orders tab, built on the orders the CPUs of the line hold. The Patterns tab follows in `m8-pattern-storage`.

## What Changes

- New Storage block **Production Terminal**: a horizontal facing block that joins the data line as a link member, holds nothing, and shows the online lamp while a Crafting CPU or a warehouse is reachable on its line. Right click opens its screen; the carrot turns it, sneak carrot dismantles it.
- The screen has a tab bar (Machines, Orders) built to take a third tab later.
- **Machines tab**: every producer on the data line with its block name, its state (working on an item, idle, no recipes) and the recipes it reports (outputs from inputs and time).
- **Orders tab**: a line per Crafting CPU with its slot use (in use of total, waiting), and under it every order of that CPU with item, amount, state, reason and source (request, stock rule, manual), and every step with producer, output, batches, state and reason. The list scrolls; clicking an open order cancels it like the Order Board did.
- Live view: while the screen is open the terminal sends the current data every half second, only when something changed.
- Recipe, block state, loot, tag, lang, textures; the Storage docs page describes it.

## Capabilities

### New Capabilities
- `storage/production-terminal`: the Production Terminal block, its data line membership and lamp, the Machines and Orders tabs, cancel and the live view.

### Modified Capabilities
None. The Crafting CPU, work orders and the data line keep their behaviour; the terminal only reads them and cancels orders through the existing cancel path.

## Non-goals

- The Patterns tab, pattern encoding and the capacity bar: `m8-pattern-storage`.
- An upkeep column in the Machines tab: comes with `m8-native-machines` when machines report upkeep.
- Making orders or editing producer recipes from the terminal.
- Restoring the Order Board block.

## Impact

- `storage`: new package `storage/production` (block, block entity, rows, payloads), a menu and a client screen, registries, payload registration, capability registration (`DataLink`), datagen (block state, item model, loot, tag, recipe, lang), textures, GameTests.
- Docs: Storage page; `gamedata.json` regenerated for the new recipe.
