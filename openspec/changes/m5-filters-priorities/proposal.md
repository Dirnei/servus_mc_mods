# Proposal

## Why

Modes alone cannot sort or prefer. Factories need "only ores through here" and "fill the furnace before the overflow chest". PLAN.md milestone M5, item "Per side modes, filters, priorities" (the filters and priorities half).

## What Changes

- **Per side filters** on the item channel: up to nine ghost items per side. An empty filter passes everything. The filter applies to items pulled in from that side and to items pushed out to it.
- **Per side priorities** on the item channel: an integer from -9 to 9, default 0. Routing offers items to the highest priority outputs first, round robin within equal priority, then lower ones.
- The duct GUI becomes a proper container menu: a side selector, mode buttons per channel for the selected side, the nine filter ghost slots, priority buttons, remove buttons and the player inventory. Ghost slots are set from the cursor or by shift clicking inventory items. This replaces the plain screen and the two configuration payloads with menu buttons.
- Filters and priorities persist with the block and survive dismantling only in the sense that a new duct starts clean (channels drop as items).

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `logistics/utility-duct`: adds filter, priority and GUI requirements.

## Non-goals

- Tag or mod based filters, blacklists, per channel priorities for energy.
- Facades, goggles, dynamic model (next changes).

## Impact

- `ItemChannelNetwork` endpoints carry priority and consult the duct's filter.
- Payload classes for mode and removal are deleted in favour of menu button ids.
