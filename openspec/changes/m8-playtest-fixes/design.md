# Design

## Context

`m8-terminal-speed` keeps terminal content per terminal on the client (`ClientStock`, `ClientProduction`). Both screens call `open(pos)` in their constructor and `close()` in `removed()`. Minecraft calls `removed()` whenever another screen replaces this one (JEI's recipe view, any child screen) and `init()` again when the same instance is shown again, but never the constructor. After a round trip through another screen the store therefore has no open terminal: every payload is cached but none is shown, so the screen stays on "Loading..." until it is reopened. Creating a pattern with the JEI transfer button is exactly that round trip, which is what the user saw.

## Goals / Non-Goals

**Goals:** the screens always show their content after a round trip; small play test polish (search clear, CPU light margin, lamps and animation).

**Non-Goals:** direct delivery (`m8-direct-delivery`), docs icon renders.

## Decisions

### Screen registration in `init()`
Both screens call `open(pos)` in `init()` (which runs on first show, on every return and on resize) and keep `close()` in `removed()`. The open/close/update/shown/version bookkeeping moves into a pure generic `client/TerminalStore<K, P>` (no Minecraft types) used by `ClientStock` and `ClientProduction`; JUnit `TerminalStoreTest` covers: open shows the cached payload, update while closed is cached and shown on the next open, a round trip close then open shows the newest payload, another terminal's payload is not shown, forget clears. Alternative considered: drop `close()` in `removed()`. Rejected: a closed screen must not keep a key that later payloads update.

### Search clear
`CounterTerminalScreen.mouseClicked`: button 1 over the search box sets its value to "" and resets the scroll. The Zone Label screen's name and filter boxes are not search boxes and stay as they are.

### CPU light margin
In `gen_block_style.py` each tile keeps the global 5 px light grid (so lights line up across tiles) but drops a light unless it lies fully inside the glass with one pixel of glass around it (`x >= x0 + 1`, `x + 1 <= x1 - 1`, same for y). On edge tiles that removes the column or row next to the frame; centre tiles are unchanged.

### Lamps
Crafting Station and Pattern Storage get `StorageNodeBlocks.ONLINE`, set every 20 ticks from `ProductionView.online(level, pos)` exactly like the Production Terminal (cheap: the data network lookup is cached). Crafting Station already has `LIT` for working; its front textures become four: dark or online lamp, still or animated working face (`crafting_station_front_lit*.png` get an `.mcmeta` with 4 frames of the 3x3 grid lighting in turn). Pattern Storage fronts become 20 textures (lights 0 to 9, lamp off or on); datagen builds 20 models instead of 10.

### Testing
JUnit `TerminalStoreTest`; GameTests: station and Pattern Storage lamps light on a line with a formed CPU and are dark alone. The LED margin is checked by a small assertion in the generator (it prints any light that touches the glass edge) and by the user in game.

### PLAN.md open decisions
None.

## Risks / Trade-offs

- [More models for Pattern Storage (20 instead of 10)] -> negligible.
- [Lamp refresh once a second] -> same as the other terminals.
