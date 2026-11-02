# Tasks

## 1. Pure search syntax (Foundation)

- [x] 1.1 Add `SearchFacts` and `SearchQuery` (`parse`, `matches`, `isEmpty`) in the new package `foundation/src/main/java/.../foundation/search`, as in design decisions 1 and 2. No Minecraft imports. Add `SearchQueryTest` covering:
  - a plain word on names; `@` on mod id and mod name; `$` on short and full tag ids; `#`, `%` and `&`;
  - all terms, `|`, `-`, a quoted phrase, a prefix followed by a quote, an unclosed quote;
  - a lone `@` or `-` ignored; an empty and a blank search; case;
  - the tooltip method not called for `@minecraft iron` (a counting fake `SearchFacts`).

  Verify with `gradlew :foundation:test`.
- [x] 1.2 Split `TagSearch.PREFIX` into `QUERY = "$"` and `ID_PREFIX = "#"`, add `isTagId`, and make `search` and `isTagQuery` use `QUERY` (`foundation/.../picker/TagSearch.java`). Update `TagSearchTest` to `$` and add an `isTagId` test. Verify with `gradlew :foundation:test`.
- [x] 1.3 Change `PickerList.filter` to take a function from item id to `SearchFacts` and filter with `SearchQuery`, keeping the none entry (`foundation/.../picker/PickerList.java`). Update `PickerListTest`:
  - plain words still match name or id;
  - a `@` term filters by mod;
  - the none entry stays for any search.

  Verify with `gradlew :foundation:test`.

## 2. Client facts and the item picker (Foundation)

- [x] 2.1 Add `ItemSearchFacts` with its per-screen cache, and `FluidSearchFacts`, in `foundation/.../client` (design decisions 3 and 4):
  - mod names through `ModList`;
  - tags through `typeHolder().tags()`;
  - tooltip lines after the name through `getTooltipLines`;
  - creative tabs through a one-time `CreativeModeTabs.tryRebuildTabContents` and `contains`.

  Verify with `gradlew :foundation:build`.
- [x] 2.2 Add `SearchHint` and the lang lines `gui.servus_foundation.search_hint.*` in Foundation's lang datagen, then regenerate. Verify with `gradlew :foundation:runData` and build.
- [x] 2.3 Wire `ItemPicker`:
  - item, fluid or plain facts per entry, with the id in `names()`;
  - `PickerList.filter` with those facts;
  - tag mode on `TagSearch.QUERY`, and Tab writing `$` plus the hit;
  - tag entries keep `#` ids;
  - the search box shows `SearchHint`.

  Update the class Javadoc to `$`. Verify with `gradlew :foundation:build` and the Foundation GameTests.

## 3. Shuttles search boxes

- [x] 3.1 Storage Terminal Stock grid (`shuttles/.../client/TerminalCells.java`, `StorageTerminalScreen.java`):
  - parse the raw search once per refresh;
  - match cells with `SearchQuery` over the screen's `ItemSearchFacts` cache;
  - set `SearchHint` on the search box.

  Verify with `gradlew :shuttles:build`.
- [x] 3.2 Rules tab:
  - `RuleListing.shown` takes `List<? extends SearchFacts>` and the query, sorts by the first name and filters with `SearchQuery` (`shuttles/.../terminal/RuleListing.java`);
  - `RulesList` passes cached facts per rule.

  Update `RuleListingTest`: name order kept, plain word filter, and a `$` term with fake facts. Verify with `gradlew :shuttles:test`.
- [x] 3.3 Handheld (`shuttles/.../client/HandheldScreen.java`):
  - `stockRows` filters with `SearchQuery` over cached facts;
  - the tag entry check uses `TagSearch.isTagId`;
  - zone tags are drawn with `TagSearch.ID_PREFIX`;
  - the STOCK search line shows `SearchHint`;
  - the zone list search stays by name.

  Verify with `gradlew :shuttles:build` and the Shuttles GameTests (zone filter tests stay green).
- [x] 3.4 Export picker (`shuttles/.../client/ExportPicker.java`):
  - facts are the offer's item, with the station label in `names()` and `tooltip()`;
  - the search box shows `SearchHint`.

  Verify with `gradlew :shuttles:build`.

## 4. Changelog and play check

- [x] 4.1 Add lines under `## Unreleased`:
  - in `foundation/CHANGELOG.md`, `Added`: item search boxes understand JEI's search syntax (`@` mod, `$` tag, `#` tooltip, `%` tab, `&` id, `-`, `|`, quotes);
  - in `shuttles/CHANGELOG.md`, `Added`: the Storage Terminal, Rules tab, Handheld STOCK and Claim an export picker search with JEI's syntax;
  - in `shuttles/CHANGELOG.md`, `Changed`: tags in the handheld zone picker are now searched with `$` instead of `#`.

  Verify that both files list them.
- [ ] 4.2 Full `gradlew build` and `gradlew test` green, and the game launches with all mods. Play check in the client:
  - Storage Terminal:
    - `@servus`, `$ingots`, `#cobblestone` (on tanks with different floors), `%redstone`, `&minecraft:oak_`, `$ingots -@minecraft` and `$ingots | $nuggets` each filter the grid as the specs say;
    - the Rules tab filters with `$`;
    - with JEI two way, a typed syntax filters both lists the same way;
    - `#` on a large stock does not stutter noticeably.
  - Duct item and fluid filter pickers filter with `@`.
  - Handheld:
    - the zone picker lists tags on `$ores`, and Tab completes to `$c:ores`;
    - picked tags show as `#c:ores`, and an old zone keeps its tags;
    - the STOCK line filters with `$`.
  - Claim an export picker: `#1` finds station #1's offers.
  - Hovering each search box shows the prefix hint.

## Workflow follow-up

- Archive the change after the user's play check sign-off, in one commit with the CHANGELOG lines.
