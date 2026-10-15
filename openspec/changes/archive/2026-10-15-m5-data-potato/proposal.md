# Proposal

## Why

The Configuration Carrot does two jobs: it configures (side modes, duct GUI, dismantle) and, while held, it reveals investigation views such as the data duct load overlay. Holding the carrot to configure floods the screen with load colours, and there is no tool whose job is to look at what a duct or tank is doing. A Data Potato takes the investigation role, so the carrot only configures.

This is a follow-up to the done milestones M5 Logistics (goggles) and M8 (data cable load), the third of three duct changes explored on 2026-10-15. It is applied after `m5-duct-filter-picker` and `m5-named-channels`; its duct inspect names the side's channel from the named channel list.

## What Changes

- New item **Data Potato** in Foundation, crafted like the carrot's sibling, stacks to 1.
- Holding the Data Potato in either hand shows the data duct load overlay. Holding the carrot no longer does.
- Facade panels turn see-through for the carrot, the potato and worn goggles. Side markers stay with the carrot and goggles. Goggles keep revealing everything.
- The one reveal rule splits into a configure rule (carrot or goggles), an analyse rule (potato or goggles) and the panel rule (any of the three).
- Right clicking a duct with the potato prints a short report into chat: for the clicked arm, per present line, the mode, the channel (name and colour), the priority for items, the size of the connected run and how many sides on that channel take items in and give them out; for the data line the tier, bandwidth and load of the last second. Clicking the centre reports every line of the duct without side details.
- Right clicking a Fluid Tank with the potato prints the tank's fluid, amount, capacity and size into chat.
- The empty hand summaries (data duct action bar line, tank fill line) stay as they are.

## Capabilities

### New Capabilities

- `foundation/data-potato`: the Data Potato item, its recipe, what it reveals and its duct and tank inspect reports.

### Modified Capabilities

- `foundation/goggles`: the reveal rule splits into configure, analyse and panel rules.
- `foundation/data-cable`: the load overlay shows for the goggles or the Data Potato, no longer for the carrot.
- `foundation/facades`: panels turn see-through for the carrot, the goggles and the Data Potato.

## Non-goals

- Throughput per channel, "why is this stuck" reasons and any new counters in the networks (a later inspect change).
- Inspect reports for machines, storage blocks, CPU or racks.
- A GUI or HUD overlay for reports; chat only.
- Changing what the carrot configures or the side markers.

## Impact

- `foundation/item`: new `DataPotatoItem`; `GogglesItem.reveals` replaced by the three rules; `ConfigurationCarrotItem` unchanged.
- `foundation/registry/ServusFoundationItems`, datagen (item model, recipe, lang), a new item texture with its generator under `tools/`.
- `foundation/data/DuctLoadSync`, `logistics/client/DuctOverlayRenderer` (analyse rule), `logistics/client/PanelReveal` (panel rule), `client/SideMarkerRenderer` (configure rule, same behaviour).
- `foundation/logistics/duct/UtilityDuctBlock` and `logistics/tank/FluidTankBlock`: let the potato's click reach the item.
- New report builders for duct and tank, a pure endpoint tally with JUnit tests, GameTests for the reports.
- `PLAN.md`: a later open point for deeper inspection (throughput, stuck reasons, machines).
