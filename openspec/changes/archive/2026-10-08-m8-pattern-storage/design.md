# Design

## Context

The Crafting CPU (`storage/cpu`) plans orders with `OrderBook` over the recipes that producers of its data line report through `servus_foundation:production`; the Crafting Station reports its own configured list (`StationRecipe`: recipe id plus a 3x3 template of concrete items). The Production Terminal (`storage/production`) is a slotless menu with an ordered tab list (Orders, Machines), synced by `ProductionPayload` every 10 ticks when its signature changes. The Crafting Station already has the recipe editor: ghost slots in its menu, `RecipeEditorButtons` ids handled in `clickMenuButton`, the Foundation `RecipeEditorPopup` on the screen, and JEI transfer through `OpenStationEditorPayload`. See proposal.md for why.

## Goals / Non-Goals

**Goals:**
- Patterns as items with a data component, kept in Pattern Storage blocks, listed and edited from the Production Terminal.
- The CPU's recipe list includes pattern recipes for producers that can execute them, so `m8-station-hands` only has to widen "can execute".

**Non-Goals:**
- Changing how a Crafting Station crafts (`m8-station-hands`).

## Decisions

### Items and component
- `ServusStorageComponents.PATTERN`: `EncodedPattern(ResourceLocation recipe, List<ItemStack> template, ItemStack result)` with a codec (persistent) and a stream codec (synced). The template always has 9 cells, empty cells as `ItemStack.EMPTY`, used cells with count 1.
- Items `blank_pattern` (stack 64) and `pattern` (stack 1, `PatternItem` with a tooltip from the component). A `pattern` without the component shows "empty" and is refused by Pattern Storage.
- Recipes (datagen): `blank_pattern` shapeless from paper and redstone, 1 result; `pattern` alone, shapeless, gives one `blank_pattern` (the shapeless ingredient ignores components, so any Pattern wipes).

### Pattern Storage
- `pattern/PatternStorageBlock` (horizontal facing like the Production Terminal, pickaxe, drops itself and its contents) and `PatternStorageBlockEntity` with an `ItemStackHandler` sized `patterns_per_block` from `data/servus_storage/storage/pattern_storage.json` through `StorageStatsManager` (default 9, clamped 1..27). A saved inventory larger than the current size keeps its extra patterns and drops them when the block is broken; a smaller one is padded.
- `isItemValid`: only `pattern` stacks with the component. No item handler capability, so ducts and hoppers cannot move patterns.
- `DataLink` capability registered in `ServusStorageEvents`, so it is a link member of the data line.
- `PatternStorageMenu` and `PatternStorageScreen`: rows of 9 slots above the player inventory, drawn with flat fills like other simple Servus screens (no new GUI texture).

### Patterns of a line
`pattern/PatternLibrary.of(ServerLevel, DataNetwork)` returns the entries (`storagePos`, `slot`, `EncodedPattern`) of every loaded Pattern Storage of the line (lowest position first, slots in order) and the capacity. It also offers `firstFree`, `find(storage, slot)` and `contains(recipe, template)`.

### Planning with patterns
`pattern/PatternRunners.canRun(ServerLevel, BlockPos producer, EncodedPattern)`: today true for a Crafting Station whose own list has a `StationRecipe` with the same id and the same per item counts (pure `TemplateCounts`). `CpuBlockEntity.Host.producers()` appends, per producer, a `ProductionRecipe` for every pattern it can run that it does not already report with the same id and inputs (id = the pattern's recipe id, inputs from the template counts, output the result, ticks the producer's slowest reported time or the station work time). With today's rule every runnable pattern equals a recipe the station already reports, so planning results do not change yet; `m8-station-hands` makes `canRun` true for any crafting pattern and these appended recipes become the station's work. Recorded here so the next change only touches `canRun` and the station.

### Production Terminal editor
- Menu: 9 ghost slots over a menu-local `ItemStackHandler` and the 36 player inventory slots, all inactive unless the editor is open (`editing` data slot: `EDIT_CLOSED`, `EDIT_NEW`, or 0 for an existing pattern). The server keeps which `(storage, slot)` is edited.
- Buttons through `clickMenuButton`: `OPEN_NEW`, `SAVE`, `DELETE`, `CANCEL` from `RecipeEditorButtons`. Opening an existing pattern uses a new `EditPatternPayload(storage, slot)`; the server checks the storage is on the terminal's line.
- Save: grid empty -> `empty`; no crafting match -> `no_match`; same recipe and items already on the line (other than the edited one) -> `duplicate`; new pattern: no Blank Pattern in the player's inventory -> `no_blank`, no free slot -> `full`, else take one blank, put the encoded Pattern into `firstFree`. Existing: rewrite in place. Messages in the action bar, editor stays open on refusal.
- Delete: remove the stack and give a Blank Pattern to the player (or drop it at their feet when full).
- JEI: `OpenStationEditorPayload` handling also opens the Production Terminal editor when that menu is open; a second transfer handler class registers for `ProductionTerminalMenu`.
- Payload: `ProductionPayload` gains `patterns` (`PatternRow(storage, slot, result, inputs)`), `patternsUsed`, `patternsTotal`; the signature includes them.

### Screen
- A third `Tab("patterns", ...)`. The tab shows the capacity line with a bar, then a plus line, then one line per pattern (result icon, "N Result from inputs", storage position in the tooltip); clicking a pattern sends `EditPatternPayload`, clicking plus presses `OPEN_NEW`.
- While editing: the list area is dimmed and inert, `RecipeEditorPopup` at (40, 22) of the 240x200 panel, the player inventory frames at (39, 112) and the hotbar at (39, 170). The popup's preview uses the client recipe manager like the Crafting Station screen.

### Testing
- JUnit: `TemplateCountsTest` (counts per item, empty cells ignored, equality).
- GameTests (`PatternGameTests`): Pattern Storage joins the line and accepts only encoded patterns; capacity is the JSON value times blocks; the menu's save path encodes a blank and stores it, refuses `no_blank`, `full` and `duplicate`; delete returns a blank; edit rewrites in place; the CPU makes an order for sticks on a line with a sticks pattern and a matching station; `ProductionView` lists patterns and capacity.

### PLAN.md open decisions
Settles "whether Pattern Storage gets tiers": no tiers, more blocks. Removed from section 5.

## Risks / Trade-offs

- [Pattern recipes do nothing visible until station-hands] -> documented; the Patterns tab and storage are fully usable, and orders already plan through the same path.
- [Inventory slots only while editing make the menu's slot list depend on state] -> the slots exist always and are inactive otherwise, like the Crafting Station's ghost slots.
- [Ghost grid needs items from the inventory] -> the inventory shows while the editor is open; JEI transfer fills it without items.
- [Changing `patterns_per_block` smaller hides patterns] -> they stay saved and drop when the block breaks.
