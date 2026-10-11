## Context

Since `m10-field-head-tabs` the Field tab of the Field Head GUI holds a ghost slot on `FieldHeadBlockEntity.cropHandler()` and a harvest only toggle (menu button 0). The head stores the crop as a one item `ItemStackHandler` plus a `harvestOnly` flag; `setCrop` turns harvest only off, `setHarvestOnly(true)` clears the crop, and both travel to clients in the block entity update tag, which `FieldHeadRenderer` reads for the crop icon on the top plate. The head finds its warehouse through the first rail of its field (`network()`), and the harvester checks for seeds with `WarehouseNetwork.available(ItemKey)`.

Foundation has a popup pattern for picking things over a machine GUI (`RecipePickerPopup`: dimmed GUI, centred grey panel with a title, scroll bar, selected entry marked). Menus that push server state to the open GUI do it from `broadcastChanges` on a sync interval and send a payload only when its signature changes (`PackingStationMenu`, `InboundStationMenu`).

This change archives after `m10-field-head-tabs`: its delta modifies the "Field Head GUI" requirement in the version that change writes.

## Goals / Non-Goals

**Goals:**
- A crop picker in place of the ghost slot and the toggle, with search and warehouse stock per entry.
- A reusable client widget, so the Fertilizer tab of `m10-spreader` can pick from another list with the same look.
- Pure list logic (sort, search, stock signature, count text) with JUnit tests.

**Non-Goals:**
- No change to how the head stores, saves or syncs the crop and the mode; the top plate renderer stays as it is.
- No new GUI texture; the popup is drawn with fills like `RecipePickerPopup`.

## Decisions

- **Crop items from the registry.** `FieldHeadBlockEntity.cropItems()` lists every item in `BuiltInRegistries.ITEM` that passes `isCropItem` (a `BlockItem` whose block is a `CropBlock`), computed once and cached; registries are frozen before any world loads. The client builds its entries from the same rule, so modded crops appear without data. The server uses the list for the stock counts.
- **Choice by payload.** `ChooseFieldCropPayload(Optional<ResourceLocation> crop)`, client to server; empty means harvest only. The handler calls `FieldHeadMenu.choose(Player, Optional<ResourceLocation>)`, which acts only when the player's open menu is a Field Head menu that is still valid (`stillValid`, the check vanilla runs for container clicks), then either sets harvest only or looks the id up in the item registry and calls `setCrop` with a fresh stack, refusing unknown ids and items that fail `isCropItem`. A payload is used rather than a menu button id: button ids are small ints and item registry ids are not part of that contract. The GameTest calls the same `choose` method with a mock player.
- **Stock counts.** `WarehouseNetwork.stockCount(ItemKey)` sums the index snapshot of the warehouse's sources holding the key, the same numbers `available` adds up, but without the per source reachability path search `stockOf` runs. `FieldHeadBlockEntity.cropStock()` returns, while the head serves a field whose rails are on a warehouse, a map of crop item id to count with only non zero counts, else empty.
- **Stock sync.** `FieldHeadMenu.broadcastChanges` builds a `FieldCropStockPayload(int containerId, boolean networked, Map<ResourceLocation, Long> counts)` every 20 ticks, the first on the first broadcast after opening, and sends it when `PickerStock.signature` of the networked flag and counts differs from the last one sent. The client handler hands it to the open menu with that container id, which keeps it as client state. With a handful of crop items this is a few map lookups per second per open GUI.
- **Menu slots.** The crop ghost slot goes away, so the head slots are Range Upgrade (0) and anti-corrosion (1), inventory from 2. `pageOf`, the tab logic and shift click stay. Shift clicking a seed no longer sets the crop: the picker is the one way to choose. The harvest only toggle button id is dropped; the threshold buttons keep ids 1 and 2.
- **Widget.** `storage/client/ItemPicker` owns a button rectangle and a popup. API: `ItemPicker(Component title, Consumer<String> onPick)`, `setEntries(List<Entry>)` with `Entry(String id, ItemStack icon, Component name)` and `Entry.NONE_ID` (empty string) for a "none" entry that stays first, `setSelected(String id)`, `setStock(Map<String, Long> counts)` (null: no counts), `setZeroHint(Component)`, `setButton(x, y, width)`, `renderButton`, `renderPopup` (dims a given area, z 400), `renderTooltip`, and the input forwards `mouseClicked`, `mouseScrolled`, `mouseDragged`, `mouseReleased`, `keyPressed`, `charTyped` that return whether the picker used the event. The popup has a title, an `EditBox` for the search, 7 rows of 18 pixels (icon, name, count right aligned) and a scroll bar; Escape closes the popup only, and while it is open every key goes to the search. Zero stock rows draw the name grey and show the zero hint as tooltip.
- **Pure logic.** `storage/picker/PickerList`: `Item(String id, String name)`, `sorted` (case insensitive by name, then id, the none id first), `filter(items, query)` (blank query keeps all, else name or id contains the lower case query; the none entry always stays). `storage/picker/PickerStock`: `signature(boolean networked, Map<String, Long>)` independent of map order, and `shortCount(long)` (1234 as 1.2k, 1200000 as 1.2M).
- **Layout.** The picker button replaces the slot and the toggle on the Field tab at 8,17, 160 wide and 18 tall: icon at the left, the name beside it, the stock count right aligned. The popup is centred over the 176 by 178 panel.
- **Lang.** `gui.servus_storage.field_head.crop_picker` (popup title), `gui.servus_storage.field_head.crop_none` ("None, harvest only"), `gui.servus_storage.field_head.crop_no_stock` (zero hint), `gui.servus_storage.picker.search` (search hint). The `harvest_only_on` and `harvest_only_off` keys go away.

## Risks / Trade-offs

- [`stockCount` counts index entries of sources the fleet cannot reach right now] The same is true of the counts the terminal shows; the work status still says when seeds cannot be fetched.
- [Crops with components (a seed with custom data) are chosen as plain items] A crop chosen in the picker is the plain item, which is how seeds are stored in the warehouse in practice.
- [Items whose block is a crop but not a seed, such as potatoes or carrots, appear by their own name] That is the vanilla rule the ghost slot used too.
