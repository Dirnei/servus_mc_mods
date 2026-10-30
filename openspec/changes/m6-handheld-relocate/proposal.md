# Proposal

## Why

The handheld's FIND mode shows where an item lies, but moving stock between shelves still means walking to each rack and carrying it by hand, or emptying and refilling through a station. Players asked to tidy a warehouse from the handheld: pick what to move in FIND, point at the shelf it belongs on, and let the shuttles carry it.

PLAN.md has no item for this yet; it extends the Warehouse Handheld of milestone M6 (`m6-warehouse-handheld`, play checked but not archived) and was decided in the play check of 2026-10-30. This change adds the line under "Open: Shuttles" and grooms it at archive.

## What Changes

- **Basket**: the FIND screen gets a basket of up to 9 entries (example), each an item with an amount. Clicking an item in the stock list adds it with an amount chosen on the Foundation number stepper (shift click adds a stack); an entry's amount can be changed, an entry removed, and the whole basket cleared. The basket lives on the handheld, survives closing the GUI and relocating, and is a reusable list that later actions (a Request to a station) build on.
- **Relocate**: a Relocate button on the FIND screen closes the GUI and puts the handheld into **pick target** mode. The panel says to scan the target shelf and shows the basket's size; the scan line and range work as for any scan.
- **Scanning the target**: right clicking any bay of a Heavy Duty Rack shelf in pick target mode relocates the basket onto that shelf:
  - items the target shelf's zone filter does not accept are refused and named in the action bar; the rest moves;
  - when the target shelf has no room for everything that moves, the whole scan is refused with a red flash, a buzz and an action bar note, and the handheld stays in pick target mode;
  - amounts are an upper bound: what the warehouse holds outside the target shelf moves, a shortfall is reported;
  - on success the shelf flashes green, a beep plays, the handheld leaves pick target mode and shuttles carry the items from the other shelves to the target shelf, one stack per trip, like terminal picks.
- Switching the handheld's mode (sneak scroll or a softkey) leaves pick target mode and keeps the basket. A Fluid Rack, a bay of another warehouse or a block that is no rack is refused with a buzz.

- **Play check revision**: two baskets. The warehouse basket (whole warehouse view, no Relocate) is kept for a later Request. The shelf basket (scanned shelf view) is emptied when the list leaves the shelf; Relocate stashes it and empties it, and the items move only from that shelf.

- **Modes revised in the play check**: FIND and INFO merge into INFO (scan a shelf, see and basket its contents, Relocate), and a new STOCK mode lists the whole warehouse with its own basket. Finding is a right click on an item in either list; a scan in STOCK jumps to INFO.

## Non-goals

- The **Request** action that sends the basket to a terminal or station; the basket is built so a later change can add it.
- Relocating fluids or onto Fluid Racks.
- Moving stock between warehouses or sites.
- Saved baskets with names, or more than one basket per handheld.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `shuttles/handheld`: the FIND basket, the Relocate action, pick target mode and the target scan with its refusals. The capability is introduced by `m6-warehouse-handheld`; this change applies after that change is archived.

## Impact

- Apply after `m6-warehouse-handheld` is archived: it changes `HandheldScreen`, `HandheldActions`, the handheld components and the handheld spec that change still owns.
- Shuttles code: `handheld` (basket component, relocate payloads, pick target state, target scan), `client` (FIND screen basket row, panel line), `network/WarehouseNetwork` (plan and start rack to rack picks, excluding the target shelf as source), `rack/HeavyRackBlockEntity` (takes shuttle deliveries), lang through datagen, GameTests and JUnit tests.
- No new blocks, items, recipes or datapack files; the basket size is a constant of the GUI.
- `shuttles/CHANGELOG.md` gets an Added line at archive; PLAN.md gets the open point now and loses it at archive.
