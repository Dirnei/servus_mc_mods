## Why

Choosing the crop of a field needs the seed in the player's hand, because the Field tab uses a ghost slot, and harvest only is a separate toggle beside it. The seeds the field plants come from the warehouse anyway, so the player should pick from a list of what can be planted and see what the warehouse holds, without fetching an item first. A later change (`m10-spreader`) needs the same kind of picker for fertilizer, so the picker is built once as a reusable widget.

## What Changes

- The Field tab's crop ghost slot and the harvest only toggle are replaced by a crop picker: a button that shows the current choice (the crop's icon and name, or "None, harvest only") and opens a scrollable, searchable list over the GUI.
- The list holds every item that places a crop block, built from the item registry on the client so modded crops appear on their own, sorted by name. The first entry "None, harvest only" sets harvest only mode.
- Each entry shows the warehouse stock of that item. The server sends the counts when the GUI opens and again when they change while it is open. Entries with no stock stay selectable but greyed, with a hint that the first planting needs seeds in the warehouse. A head without a warehouse shows no counts.
- Choosing an entry sends the item id (or harvest only) to the server, which checks that the item plants a crop and that the player can still use the head, then sets the crop. The player never needs the item.
- Shift clicking a seed in the inventory no longer sets the crop; the picker is the one way to choose it.
- The head's crop and harvest only state, its saving and the crop icon on the top plate stay as they are.
- A reusable item picker widget in the Storage client (button plus popup with search, rows, counts and a zero stock hint) and pure list logic (sort, search, stock signature) with JUnit tests.
- Lang keys via datagen; docs (`docs/src/content/docs/mods/storage.md`) and the `PLAN.md` M10 list describe the picker.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `storage/field-head`: the Field Head GUI requirement (as modified by `m10-field-head-tabs`) chooses the crop through the picker with stock counts instead of a ghost slot and a toggle.

## Impact

- `storage/.../menu/FieldHeadMenu.java`: crop ghost slot and harvest only button removed, `chooseCrop`, stock sync on the existing broadcast tick.
- `storage/.../client/FieldHeadScreen.java`: picker button and popup on the Field tab.
- New `storage/.../client/ItemPicker.java` (widget), `storage/.../client/CropPickerEntries.java` (registry list), `storage/.../picker/PickerList.java` and `PickerStock.java` (pure logic).
- New payloads `ChooseFieldCropPayload` (client to server) and `FieldCropStockPayload` (server to client), registered in `ServusStoragePayloads`.
- `storage/.../field/FieldHeadBlockEntity.java`: crop items from the registry, warehouse stock lookup.
- `storage/.../network/WarehouseNetwork.java`: `stockCount(ItemKey)` read from the index.
- `ServusStorageLanguageProvider` and generated `en_us.json`; `FieldGameTests`; docs and `PLAN.md`.
