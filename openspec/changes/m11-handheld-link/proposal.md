# Proposal

## Why

The Handheld's STOCK screen lists the whole warehouse and holds a basket, but nothing can be ordered from it: the player still walks to a Storage Terminal. A warehouse scanner should order from where the player stands and hand the goods over at a pickup point, like a parcel station. This implements the PLAN.md open point "Handheld orders to a Parcel Locker" under Open: Shuttles (milestone M11, remote access), explored on 2026-10-29 and 2026-10-30.

## What Changes

- **REQUEST** on the Handheld's STOCK screen opens a locker picker: the warehouse's Parcel Lockers sorted by distance from the player, nearest first and preselected, each with its number and name, distance and free boxes; a locker that cannot take the order is greyed out. SEND places the order and empties the STOCK basket.
- **Range**: the Handheld HH-1 orders only while the player stands within the warehouse's site area plus a margin from data; outside, REQUEST says the warehouse is out of range.
- **Parcel Locker**: a new block group placed by one item, one fixed piece 5 wide, 2 high and 1 deep: a column of 4 S boxes, a column of 2 S boxes over 1 M box, the head (screen over the shuttle dock) in the middle, a column of 2 M boxes and 1 L box. 6 S, 3 M and 1 L boxes; S holds 3 stacks, M 6, L 9 (datapack JSON). The dock faces the rail by the stations' facing rule. Breaking any part removes the whole locker and drops its contents and the one item.
- **Orders**: an order reserves as many free boxes as it needs, smallest first, all marked with the order and the player; a locker without enough room refuses it before anything is picked. Shuttles pick the items from the racks like a terminal request and deliver them at the dock into the order's boxes. The Handheld panel and a chime say when the parcel is ready.
- **Collecting**: scanning the locker's screen with the Handheld greets the player on the screen and swings their doors open in the world; right clicking an open box moves its items into the inventory and frees it. An empty hand on the screen opens the locker GUI with the name pencil and the player's boxes.
- **Look** from `temp/parcel-locker-look.html`: yellow body with a thin roof, doors with small ids and lamps, animated doors and items drawn by a renderer, a screen with QR code, blinking SCAN and short messages, a dock lamp that blinks while loading.
- **Returns**: a parcel not collected within one in-game day (configurable) goes back into the racks by shuttle, like an Inbound Station's put-away, and its boxes are freed.
- Data Potato line for the locker, machine number and name like the stations, recipe, lang, PLAN.md and CHANGELOG.

## Capabilities

### New Capabilities

- `shuttles/parcel-locker`: the Parcel Locker block group, its boxes, deliveries, collecting, returns and inspect.

### Modified Capabilities

- `shuttles/handheld`: REQUEST on the STOCK screen with the locker picker and the HH-1 range; the STOCK basket is emptied by a sent order.

## Non-goals

- Handheld HH-2 and HH-3 with dimension wide and cross dimension range, and orders to a sleeping site (`m11-handheld-tiers`).
- The Direct Delivery upgrade (`m11-direct-delivery`).
- Saved or repeated baskets.
- Ordering from the Storage Terminal to a Parcel Locker.
- Autocrafting missing items for an order: only what is in stock is ordered.

## Impact

- New Shuttles package `locker` (block, block entity, box logic, menu, screen, renderer), registrations, datagen (models, lang, loot, recipe), `warehouse.json` keys.
- `network/WarehouseNetwork` (order planning to a locker, returns as put-away), `handheld` (REQUEST payload, picker data), `client/HandheldScreen`, `client/HandheldHud`.
- Pure logic with JUnit: box assignment, return timing, range check.
- GameTests for placement, orders, collecting, returns and refusals.
