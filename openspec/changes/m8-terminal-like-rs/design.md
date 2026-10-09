# Design

## Context

The Counter Terminal screen (`client/CounterTerminalScreen`) draws from fixed rectangles in `terminal/TerminalLayout.Counter`, checked by `TerminalLayoutTest`. The menu (`menu/CounterTerminalMenu`) places output, inventory, hotbar, crafting grid and result slots from the same constants. Vanilla `Slot.x` and `Slot.y` are final and the project has no access transformer, so slot positions cannot follow a client side height setting. Content arrives instantly through `ClientStock` (m8-terminal-speed). See proposal.md for the why.

## Goals / Non-Goals

**Goals:** RS/AE2 like layout with a side column of view buttons, height settings, requests inside the grid, client side sorting, saved settings, JEI search sync, amount steps that fill the stack first.

**Non-Goals:** server changes, Production Terminal layout, per terminal settings.

## Decisions

### Height without moving slots
Everything at and below the message line has fixed offsets from the panel's base top (`topPos`), so the menu's slot positions never change. Extra grid rows grow the panel upwards: the grid, scrollbar, title, search box and side column move up by 18 pixels per extra row and the panel is drawn from `-extra * 18`. The screen centres the whole panel by setting `topPos = (height - totalHeight) / 2 + extra * 18` after `super.init()`. Alternative: an access transformer to make `Slot.x/y` mutable (what bigger mods do); rejected because it adds build setup for one screen.

### Layout numbers
Panel 190 wide. Base layout (small, 4 rows): title row at y 4 to 16 with the search box (x 96, 86 wide) on the right; grid at (8, 18), 9 x rows cells, scrollbar at x 174; message line at y 92; crafting grid (8, 104), arrow (66, 123), result (86, 122), clear button (89, 106), output 3x3 at (128, 104); inventory label y 162, inventory y 173, hotbar y 231; base height 256. Heights: small 4 rows, medium 6, tall 9, auto as many as fit; every setting is capped at what fits the screen (screen height minus 4) and never below 4. At 1080p: GUI scale 4 gives 270 pixels, so every setting shows 4 rows; scale 3 (360) fits 9 rows; scale 2 (540) fits 19. Side column at x -22, five 18 by 18 buttons, 20 apart, starting at the panel top. `TerminalLayout.Counter.layout(rows)` returns a `CounterLayout` record with all rectangles; the base slot positions stay static constants for the menu. Craft and output labels are dropped (tooltips on the clear button and a hover title cover them) to keep the base height under the scale 4 limit.

### Dialogs
The amount prompt and the craft dialog cover the grid, message line and crafting row (base 8, 18, 174 by 140, growing upwards with extra rows). They are drawn at z 400 so slot items under them stay hidden, slot clicks inside the dialog area are swallowed and slot tooltips under it are not drawn. Rows: header, amount field, six step buttons (-S, -10, -1, +1, +10, +S, 26 wide), preview columns (three columns 54 wide), status line, and the bottom buttons (remove rule, rule, confirm, cancel).

### Amount steps
Pure `terminal/AmountSteps`: `plusStack(v, s)` is the smallest multiple of `s` above `v` (1 -> 64, 64 -> 128, 70 -> 128, 0 -> 64); `minusStack(v, s)` is the largest multiple of `s` below `v`, at least 1 (70 -> 64, 128 -> 64, 64 -> 1, 10 -> 1); `add(v, n)` clamps to 1..99999. JUnit tested.

### Requests in the grid
The client builds the grid as requests first (in request order), then stock and craftables sorted by the setting. A request cell is tinted blue, shows `delivered/requested` in small text and a coloured mark in the top right corner: white dot picking, teal arrow sending, yellow pause waiting for room, green gear producing, red cross missing. Click cancels it immediately, the same gesture as the removed request row (it had no confirmation). The search applies to requests; the view setting does not hide them.

### Sorting
Pure `terminal/TerminalSort` with `Key(name, count, mod)` and `comparator(sort, direction)`; ties fall back to name, then mod. Craft only entries count as 0. Default amount descending, which matches the server order used before.

### Settings
Pure `terminal/TerminalSettings` (record with enums `Sort`, `Direction`, `View`, `SearchMode`, `Height`) with `fromProperties`/`toProperties` and safe defaults for unknown values. The client keeps one instance in `client/ClientTerminalSettings`, loaded once from `config/servus_storage-terminal.properties` (`FMLPaths.CONFIGDIR`) and written on every change. Alternative: a NeoForge client config spec; rejected because the values are UI state, not configuration, and change on every click.

### JEI search sync
`client/JeiSearchBridge` holds two optional functions (push text, read text), null when JEI is absent. `ServusStorageJeiPlugin.onRuntimeAvailable` fills them from `IJeiRuntime.getIngredientFilter()` (`setFilterText`, `getFilterText`, JEI 19.57 API) and `onRuntimeUnavailable` clears them. Synced mode pushes on every search change; two way also polls JEI's text each frame and copies a changed text into the search box, remembering the last synced text to avoid loops. Without the bridge the search mode button is fixed to normal. The plugin also registers an `IGuiContainerHandler` for `CounterTerminalScreen` whose extra areas are the side column and the upward extension, so JEI's overlays leave them free.

### Testing
JUnit: `AmountStepsTest`, `TerminalSortTest`, `TerminalSettingsTest`, `TerminalLayoutTest` for every height at 1080p scales 2, 3, 4 (no overlaps, inside the panel, panel inside the screen, side column left of the panel). GameTests: existing Storage suites stay green (server unchanged). In game: look, settings, JEI sync.

## Risks / Trade-offs

- [Panel grows upwards, so JEI or other mods that assume `topPos` is the panel top may draw over the top rows] -> the JEI handler reports the extension as an extra area.
- [Dropping the craft and output labels] -> tooltips on the clear button and over the areas name them.
- [Clicking a request cancels it without confirmation, now inside the grid where clicks are frequent] -> same behaviour as before; the tooltip says "Click to cancel"; a confirmation can follow if play testing asks for it.
