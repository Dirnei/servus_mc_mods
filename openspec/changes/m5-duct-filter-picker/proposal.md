# Proposal

## Why

A duct side's item filter can only be set from an item the player holds: the ghost slots copy the cursor or a shift clicked inventory item. To filter for something the player does not carry (a modded ore, an item stored far away) they have to fetch one first. JEI already lists every item, and the Field Head already has a search picker, so the filter can be set without owning the item.

This is a follow-up to the done milestone M5 Logistics (filters). It is the first of three duct changes explored on 2026-10-15; `m5-named-channels` and `m5-data-potato` follow and build on it. `PLAN.md` lists the three under "Open: Foundation".

## What Changes

- Dragging an item from JEI onto a filter slot of the endpoint GUI sets that slot. Slots highlight while dragging.
- Clicking a filter slot with an empty cursor opens a search popup over the GUI that lists every registered item; picking one sets the slot. The popup also has a "none" entry that clears it.
- Right clicking a filter slot clears it. Clicking with an item on the cursor and shift clicking an inventory item still set a slot as before.
- Setting a slot by item id goes through a new client to server payload; the server checks that the player has this duct's endpoint GUI open and the duct carries the Item Pipe.
- The search picker (`ItemPicker`) and its pure list and stock helpers move from Storage to Foundation so the duct GUI can use them. The Field Head keeps using them, now from Foundation.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `foundation/utility-duct`: the per side item filter can be set by JEI drag and by a search popup, cleared by right click; the duct GUI requirement describes the new slot clicks.

## Non-goals

- Filtering by tag, by mod or by item components (the filter keeps matching by item only).
- More than nine filter slots, or a blacklist mode.
- Named channels and the new channel picker (`m5-named-channels`).
- The Data Potato and duct inspection (`m5-data-potato`).
- JEI drag onto other Servus ghost slots (Field Head, terminals).

## Impact

- `foundation`: `logistics/menu/UtilityDuctMenu`, `logistics/client/UtilityDuctScreen`, a new payload in `data/ServusFoundationPayloads` (or a logistics payload class), `integration/jei/ServusFoundationJeiPlugin` (ghost ingredient handler), new `client/ItemPicker` and pure `picker/PickerList`, `picker/PickerStock` with their JUnit tests, lang.
- `storage`: `client/ItemPicker`, `picker/PickerList`, `picker/PickerStock` and their tests move out; `FieldHeadScreen`, `FieldHeadMenu`, `CropPickerEntries`, `TagPickerEntries` import them from Foundation.
- JEI stays an optional compile only dependency; without JEI the search popup still works.
- `PLAN.md`: "Open: Foundation" lists this change and its two followers.
