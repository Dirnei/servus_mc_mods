# Craft panel for the Storage Terminal

## Why

Players found the order grid confusing: its result sits in a real slot frame and looks like something to take, a pattern can only be saved after pressing Start (as the question "No pattern has this recipe. Save it?"), and the amount, six step buttons, status and preview crowd one panel. The craft dialog (click a craftable item) repeats the same amount, steps and preview in its own layout. Long late game crafts do not fit the three preview rows. The play check of `m8-request-slots` also found that JEI's plus button on a crafting recipe always says there is no CPU, and that a dragged stack disappears behind the craft dialog.

This is a follow-up of PLAN.md M8 (Storage Terminal, order grid and craft dialog), explored with the user on 2026-10-24 with the mockup `temp/craft-flow.html`.

## What Changes

- The order grid only builds the recipe: ghost grid, arrow, the result as a bare icon with one craft's count and its normal item tooltip, and a Save button next to it ("Save as pattern", shown only while no pattern of the line has the recipe with the same items) that saves without starting. Cancel on the left, Next on the right; Next is greyed out with the reason as tooltip while the grid matches nothing.
- Next opens the shared amount panel, which is also what clicking a craftable item opens (the craft dialog): title "Craft <item>", +1 +10 +stack above the amount field, the item to make with the count that gets crafted beside it, -1 -10 -stack below, then three scrollable lists with item icons: From stock, To craft and Missing (always shown, empty when nothing is missing), all in the panel's dark text colour. Back (order flow) or Cancel on the left, Start on the right. Start is greyed out with the error as tooltip while the order cannot start, and has no tooltip when it can. The amount starts at 1. No status line and no "Ready to start".
- Start orders right away; an order grid recipe without a pattern runs once. The question after Start and its "Save as pattern" and "Only this once" buttons go.
- Foundation gets two reusable client widgets: a number stepper (field with configurable plus and minus step buttons, stack steps snap to full stacks, wheel over the field) and a scrollable item list (heading, half size icons, count and name, wheel scroll, a thin scrollbar). The pure step and scroll math lives in Foundation with JUnit tests; Shuttles' `AmountSteps` moves there.
- Shuttles' copy of `GhostSlot` goes; the terminal and the other Shuttles menus use Foundation's.
- Fixes: the JEI plus button works while the line has a CPU and a Crafting Station (the terminal screen forgot its content when JEI's recipe view opened in front of it), and a carried stack is drawn above the dialogs.

## Capabilities

### New Capabilities
- `foundation/gui-widgets`: reusable GUI widgets of the mod family: the number stepper and the scrollable item list, with their pure step and scroll rules.

### Modified Capabilities
- `shuttles/storage-terminal`: craft dialog becomes the shared amount panel; the order grid layout, Next, Save button and Start change; JEI transfer and the carried stack fixes.

## Non-goals

- The right click amount prompt (request) and the stock rule dialog keep their layout. Whether the request prompt uses the amount panel is a new open point in PLAN.md.
- No change to how orders are planned, booked or crafted, or to what the preview computes.
- Other screens (Field Head amounts, Fluid Gantry keep buttons, Production Terminal lists) do not switch to the new widgets in this change.

## Impact

- Foundation: new `ui` package (pure) and two client widgets; Shuttles' `AmountSteps` and `AmountStepsTest` move to Foundation.
- Shuttles: `StorageTerminalScreen`, `TerminalLayout` (order grid and amount panel rects), `StorageTerminalMenu` (save without starting), `OrderStartPayload` (no save flag), a new `OrderSavePayload`, `ServusShuttlesPayloads`, lang via datagen, `OrderGridGameTests`, `TerminalLayoutTest`; `menu/GhostSlot` removed.
