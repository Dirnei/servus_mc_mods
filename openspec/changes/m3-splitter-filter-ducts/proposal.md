# Proposal

## Why

Plain ducts only move what machines push into them, and they deliver to any neighbour that accepts. Factories need to pull from chests and to sort. The splitter_filter_ducts tech unlocks both. PLAN.md milestone M3, item "Splitter and Filter Ducts".

## What Changes

- **Splitter Duct**: a duct that pulls items from its adjacent inventories (output sides only) every few ticks and sends them into the duct network, which spreads them round robin over the accepting inventories. Pull size from the stats JSON.
- **Filter Duct**: a duct with a nine slot whitelist set through its GUI with ghost items. The network delivers to the filter duct's neighbours only items on the list; an empty list allows everything.
- Both are duct network members like plain ducts, connect to them and share one network.
- Recipes: assembling, gated by the splitter_filter_ducts tech. Textures, models, lang, JEI through the existing categories.

## Capabilities

### New Capabilities
- `manufacturing/splitter-duct`: pulling into the network.
- `manufacturing/filter-duct`: whitelisting deliveries.

### Modified Capabilities
None.

## Non-goals

- Blacklists, tag filters, per side filters (Logistics M5 brings the full Utility Duct with channels).
- Priorities between targets.

## Impact

- `ItemDuctNetwork` tracks which duct owns each endpoint and which ducts are splitters.
- `ItemDuctBlockEntity` gains a type parameterised constructor for subclasses.
- New blocks, block entity types, a menu and screen.
