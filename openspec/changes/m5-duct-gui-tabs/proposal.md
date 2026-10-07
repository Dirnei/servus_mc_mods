# Proposal

## Why

Play test (2026-10-07): the duct endpoint GUI is cramped once a duct carries both lines, with two rows of tiny channel buttons, a filter row and the priority squeezed above the inventory. The player asked for tabs per line like LaserIO, so each line gets the whole panel.

## What Changes

- **One tab per present line.** Tabs sit on the left edge of the GUI, one for the Item Pipe and one for the Power Cable, each showing the line's item. The active tab merges into the panel. A duct with one line shows one tab. Switching tabs is a client side view.
- **One page per line.** The page shows the line's mode as a wide button, the eight channels as large colour cells with the current one framed, and the remove button. The Item Pipe page adds the filter slots and the priority with its minus and plus buttons; the Power Cable page shows only mode, channels and remove. Filter slots are hidden and inert on the Power Cable page.
- **Standard width.** The panel is 176 wide like every other GUI, with the tabs outside it.

## Capabilities

### Modified Capabilities
- `logistics/utility-duct`: the Duct GUI requirement describes the tabs and pages.

## Non-goals

- Server side tab state or per tab permissions.
- Changing what the buttons do.

## Impact

- `logistics`: `UtilityDuctScreen` (tabs, page rebuild, layout), `UtilityDuctMenu` (page aware filter slots, slot positions), GUI texture and lang keys for the tab tooltips.
