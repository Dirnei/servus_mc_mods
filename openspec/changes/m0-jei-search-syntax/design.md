# Design

## Context

Five item search boxes each filter with their own loop over the lower case name:

| Box | Code today | Matches |
|-----|-----------|---------|
| Storage Terminal, Stock | `TerminalCells.matches` | name |
| Storage Terminal, Rules | `RuleListing.shown(names, query)` (pure) | name |
| Foundation item picker | `PickerList.filter` (pure), plus `TagSearch` when `setTagSearch(true)` | name or id; `#` lists tags |
| Handheld STOCK | `HandheldScreen.stockRows` | name |
| Claim an export picker | `ExportPicker.lines` | name or station label |

`TagSearch.PREFIX = "#"` serves two jobs today:
- it is the typed key that switches the picker to tags;
- it is the prefix of tag entry ids and labels (`#c:ores`). `HandheldScreen` uses it to tell a tag entry from an item entry and to draw zone filter tags.

The item picker serves:
- the duct item and fluid filters (`UtilityDuctScreen`);
- the Field Head crop, sapling and tag pickers;
- the Fluid Outbound picker;
- the duct channel form;
- the handheld zone filter, the only one with tag search on.

Lists that hold no items (handheld zone list, `ChannelPopup`) are out of scope; see proposal.md, Non-goals.

## Goals / Non-Goals

**Goals:**
- One parser and matcher, pure and unit tested, used by every item search box.
- Item facts are computed only when a term needs them. A tooltip is built only for a `#` term.

**Non-Goals:**
- No index. Every box still filters by scanning its list on a change of the text or the list, as today.

## Decisions

### 1. Pure query in `foundation.search`

New package `io.github.dirnei.mc.servus.foundation.search`, no Minecraft imports:

- `SearchFacts`: an interface a list entry implements or is wrapped in.
  - Methods: `names()`, `ids()`, `mods()`, `tags()`, `tooltip()`, `tabs()`, each a list of strings; the matcher lower cases.
  - Callers return empty lists for what they do not know (a fluid has no tabs).
  - `names()` is what a plain word matches: the name, plus extra text such as the export picker's station label or a picker entry's id.
- `SearchQuery.parse(String)` turns the text into alternatives (split on `|` outside quotes), each a list of `Term(kind, text, negated)`.
  - `kind` is NAME, MOD, TAG, TOOLTIP, TAB or ID.
  - Lexing:
    - whitespace separates terms;
    - `"` toggles quoting, and an unclosed quote runs to the end;
    - a leading `-` negates;
    - one leading prefix character picks the kind, and the prefix may be followed by a quoted text (`@"applied energistics"`).
  - A term whose text is empty after the prefix and `-` is dropped. An alternative with no terms is dropped. A query with no alternatives matches everything.
- `SearchQuery.matches(SearchFacts)` is true when any alternative has all its terms true. A term is true when any string of its kind contains its text, ignoring case. A negated term inverts that.
  - Each term asks only its own kind's method, so the matcher touches `tooltip()` only for TOOLTIP terms.
  - Caching is the caller's job (decision 3).
- `SearchQuery.isEmpty()` lets a caller skip building facts.

*Alternatives:*
- Use JEI's `IIngredientFilter`. Rejected: it needs JEI, matches JEI's ingredient list rather than our stacks with components, and the Handheld and pickers must work without JEI.
- Regex per box. Rejected: copied logic, which is what this change removes.

### 2. JEI's semantics, to the extent the specs need them

- The prefixes follow JEI's defaults: `@` mod name, `#` tooltip, `$` tag, `%` creative tab, `&` resource id. `^` is skipped.
- `@` matches both the namespace and the mod display name (`ModList.get().getModContainerById(ns)` → `getModInfo().getDisplayName()`), as JEI matches mod id and name.
- `$` matches the full tag id `namespace:path`, so both `$ingots` and `$c:ingots` work.
- `#` matches tooltip lines after the first (the name). Each line's `getString()` is used.
- `%` matches the display name of each `CreativeModeTab` of type CATEGORY whose `contains(stack)` is true. `contains` checks the search-tab set, which compares components, so a stack with components matches only if the tab lists it so. For entries that are plain items (pickers), the plain stack is used.
- A plain word matches `names()`. The old pickers also matched the id on a plain word (`PickerList.filter`). To keep "typing `oak_log` finds Oak Log" working in pickers, the picker puts the id into `names()`. Everywhere else `&` is needed for ids, as in JEI.

### 3. Client facts in `foundation/client/ItemSearchFacts`

`ItemSearchFacts` wraps one `ItemStack`, with each fact a memoised supplier:
- `mods`: namespace plus mod name;
- `tags`: `stack.typeHolder().tags()` mapped to `location().toString()`;
- `tooltip`: `stack.getTooltipLines(Item.TooltipContext.of(level), player, TooltipFlag.NORMAL)`, dropping line 0;
- `tabs`: from decision 4.

`ItemSearchFacts.cache()` returns a small cache keyed by item plus components (`ItemStack.hashItemAndComponents` and `isSameItemSameComponents`). A screen holds one cache while it is open, so retyping does not rebuild tooltips.

`FluidSearchFacts` does the same for a `FluidStack`: name, id, namespace and mod name, and fluid tags through `fluid.builtInRegistryHolder().tags()`.

Picker entries that are neither item nor fluid (the channel form) get name and id only.

`SearchFacts` gives empty defaults for every fact but `names()`, plus `of(names, ids)` for name and id only facts and `withNames` / `withTooltip` decorators that add text to a base without building its other facts. A name or tooltip that throws in a mod's code counts as empty instead of breaking the screen.

### 4. Creative tab membership

- Before a stack's tabs are looked up, `CreativeModeTabs.tryRebuildTabContents(featureFlags, hasPermissions, registryAccess)` runs, from the local player's connection, the same call the creative screen makes. Vanilla caches the parameters, so the call rebuilds only on the first use per world or when features or permissions changed; otherwise it is a cheap no-op.
- When that call did rebuild, the creative screen's search trees are refreshed the way the creative screen does it (`SessionSearchTrees.updateCreativeTooltips` and `updateCreativeTags` per tab with a search bar, keys from `CreativeModeTabSearchRegistry`). The creative screen refreshes them only when its own call rebuilds, so skipping this would leave creative search empty after our rebuild.
- After that, a stack's tabs are found with `contains` over `CreativeModeTabs.allTabs()` of type CATEGORY.
- The result is memoised per stack in the facts, so the scan over tabs runs once per stack per screen.

### 5. Tag mode moves to `$`; ids keep `#`

- `TagSearch` gets two constants:
  - `QUERY = "$"`, the typed key;
  - `ID_PREFIX = "#"`, the entry id and label prefix.
- Methods change with them:
  - `isTagQuery` checks `QUERY`;
  - a new `isTagId` checks `ID_PREFIX`;
  - `search` strips `QUERY`.
- In `ItemPicker`:
  - Tab completion writes `QUERY + hit`;
  - tag entries keep `ID_PREFIX + id`.
- `HandheldScreen` switches its entry check to `isTagId` and keeps drawing zone tags with `ID_PREFIX`.
- With tag search off, a `$` term filters items by tag through `SearchQuery`.
- Saved zone filters store bare tag ids (`c:ores`), so nothing saved changes.

### 6. Wiring per box

- **Terminal Stock:**
  - `TerminalCells.matches` uses a `SearchQuery` parsed once per refresh;
  - facts come from the screen's `ItemSearchFacts` cache;
  - the query is passed raw, no longer lower cased in `StorageTerminalScreen`, because quotes and case are handled by the parser.
- **Terminal Rules:**
  - `RuleListing.shown(List<String> names, String query)` becomes `shown(List<? extends SearchFacts> rows, String query)`, still pure;
  - it sorts by the first of `names()` and filters with `SearchQuery`;
  - `RulesList` wraps each rule's stack in the cached facts.
- **Item picker:**
  - `PickerList.filter(items, query)` becomes `filter(items, query, facts)`, with `facts` a function from item id to `SearchFacts`, still pure;
  - the none entry always stays;
  - `ItemPicker` supplies item, fluid or plain facts per entry from its icon and fluid fields: item facts only when the icon's item id is the entry id, fluid facts only when the fluid's id is the entry id, so a decorative icon (the channel form) does not answer `@` or `$`. Each gets the entry's name and id added to `names()`.
- **Handheld STOCK:** `stockRows` filters with `SearchQuery` over cached facts.
- **Export picker:** facts are the offer's item, with the station label added to `names()` and to `tooltip()`, so `#1` finds station #1 (spec "Search by station number").

### 7. Prefix hint

- A shared `SearchHint.tooltip()` in `foundation/client` returns the translated lines.
- Each item search box sets it through `SearchHint.apply(box)`, which also sets a short hover delay (500 ms) so the hint does not flash while the mouse passes.
- The handheld's search line shows the hint only in STOCK; in the zone list it searches by name and has no hint.
- The lang keys are `gui.servus_foundation.search_hint.*` in Foundation's datagen lang.

### 8. Settled open points

- `PLAN.md` has no open decision for this; it is an M0 polish item.
- The question whether empty search boxes need a hint had no answer. This design takes the smallest option, a hover tooltip (decision 7). It is easy to drop at review.

## Risks / Trade-offs

- [`#` tooltip search over a big terminal stock is slow on first use] → Tooltips are built lazily, only for `#` terms, and cached per stack while the screen is open. GameTests cannot measure client tooltips, so the play check notes the feel with a large stock.
- [`tryRebuildTabContents` before the creative screen was ever opened costs a few ms once] → It runs once per session, only for `%`, and the creative screen and JEI do the same.
- [Players used to `#` for tags in the handheld picker] → A CHANGELOG `Changed` line, the hover hint, and JEI's own convention.
- [Mod tooltips that read client state could change between keystrokes] → The cache lives only while the screen is open.

## Migration Plan

No data changes. Zone filters store bare tag ids. Rollback is a plain revert.
