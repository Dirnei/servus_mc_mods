## Why

Players asked to search the Storage Terminal like JEI: by mod, by tag and the other JEI prefixes. Today every item search box in the family only matches the item name, and each one does it with its own copy of the loop. The terminal can also copy its search into JEI, so the same text means different things on the two sides of the screen. This change is an M0 (shared code and polish) item and is not tracked in `PLAN.md`.

## What Changes

- One shared search syntax in Foundation, the same as JEI's:
  - `@` mod (id or name), `$` tag, `#` tooltip, `%` creative tab, `&` item id;
  - `-` negates a term, a space between terms means all of them, `|` means either side, and `"..."` keeps spaces in one term.
  - A plain word matches the name as before.
- A pure parser and matcher in Foundation (no Minecraft imports, JUnit tested), plus a client-side adapter that supplies an item's or fluid's mod, tags, tooltip, creative tabs and id.
- Every search box that lists items uses it:
  - the Storage Terminal's Stock grid and Rules tab;
  - Foundation's item picker, and so the duct item and fluid filters, the Field Head pickers and the handheld zone filter picker;
  - the Handheld STOCK list;
  - the Outbound Station's Claim an export picker, where plain words still match the station label as well.
- **BREAKING** (players only, no data): in a picker with tag search, listing tags now starts with `$` instead of `#`, as in JEI; `#` now searches tooltips there like everywhere else. Picked tags keep their `#namespace:path` id and label, so saved zone filters and the zone form are unchanged.
- Hovering an item search box shows a short tooltip that lists the prefixes. This is the smallest hint; the user may drop it at review.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `foundation/gui-widgets`:
  - new requirements for the search syntax and for its pure matcher;
  - the tag search in the item picker moves to `$`, and its ranking scenario is updated with it.
- `shuttles/storage-terminal`: "Stock view with search" filters with the syntax on both tabs. "JEI search sync" now notes that the copied text means the same in JEI.
- `shuttles/handheld`: the STOCK search line uses the syntax.
- `shuttles/zones`: "Zone properties" lists tags with `$` in the picker.
- `shuttles/outbound-station`: "Picker search" uses the syntax.

## Impact

- Foundation:
  - new package `io.github.dirnei.mc.servus.foundation.search` (pure);
  - new client adapter in `foundation/client`;
  - `picker/TagSearch` split into the typed key `$` and the id prefix `#`;
  - `client/ItemPicker`, `picker/PickerList`.
- Shuttles:
  - `client/TerminalCells`, `client/StorageTerminalScreen`, `client/RulesList` with `terminal/RuleListing`;
  - `client/HandheldScreen`, `client/ExportPicker`.
- No network, save data or datapack changes, so no migration is needed.
- The docs site describes released versions only. Its search notes are updated at the next release, not here.
- Overlap check:
  - `m5-floor-block-ingredient` changes a different storage-terminal requirement ("JEI fills the order grid");
  - `m0-servus-commands` changes a different outbound-station requirement ("Deliveries on request").

## Non-goals

- JEI's `^` colour search.
- Changing the search of lists that hold no items: the handheld zone list and the duct channel popup keep searching by name.
- Using JEI's own filter or index. The syntax must work without JEI and on stacks with components that JEI does not list.
- Search history, autocompletion of mod names, or a settings screen to turn single prefixes off.
