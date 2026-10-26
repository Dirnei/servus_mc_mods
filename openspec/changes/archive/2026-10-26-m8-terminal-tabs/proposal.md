# Proposal: m8-terminal-tabs

## Why

The Storage Terminal's amount prompt is cramped: a Rule toggle switches the same small box between a request and a stock rule, with buttons 36 to 46 pixels wide, and the craft dialog is a second popup with a different look. Stock rules can only be found by their marker in the grid and only be made for items the grid shows. The coming rule thresholds (`m8-rule-cap`, `m8-rule-export`) need room for two more numbers and an overview of all rules, so the terminal UI is reworked first.

PLAN.md milestone M8 (autocrafting and the Storage Terminal), "Open: Shuttles": "Storage Terminal request prompt and stock rule dialog on the Foundation number stepper and the amount panel look of the craft dialog". This change implements the Storage Terminal half of that point; the other screens named there stay open.

## What Changes

- Right clicking a stocked entry, clicking a craftable entry (as the craft dialog opens today) or clicking a row of the Rules tab opens one item popup in the look of the craft popup, with three tabs above it: Request, Craft and Rule. The tab that opens depends on the click; the others are one click away for the same item.
- **Request tab** replaces the amount prompt: the Foundation number stepper (+1, +10, +stack above, the minus steps below), the item with the amount beside it, the stock and crafted split with the server preview as today, Cancel and Request.
- **Craft tab** is the craft dialog's amount panel as today (stepper, From stock, To craft, Missing, Cancel, Start).
- **Rule tab** replaces the prompt's Rule mode: the item with its rule state, a "Make" group with "Order when below" and "Fill up to" as one line steppers (minus, field, plus), live validation, Remove, Cancel and Save. The tab leaves a row free below Make for the "Limit" group of the rule threshold changes.
- A tab that cannot do anything for the item is greyed out with the reason as its tooltip (nothing on the line makes it, out of stock, no warehouse).
- **BREAKING** (UI only): the amount prompt with its step row and Rule toggle is removed.
- The terminal gets two tabs above its panel: **Stock** (the item grid as today) and **Rules**. Rules lists every stock rule of the reached warehouse in place of the grid, sorted by item name and filtered by the search box: item icon, "8 to 16" and the state. Clicking a row opens the item popup on its Rule tab.
- The Rules tab has an add slot as its first row: dropping a carried item or a JEI ingredient on it opens the Rule tab for that item without taking the item, so rules work for items that are not in the grid.
- Foundation gets a one line number stepper next to the existing one.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `shuttles/storage-terminal`: Request items (right click opens the Request tab instead of the amount prompt), Request beyond stock is crafted (said by the Request tab), Craft dialog (becomes the Craft tab), Stock rules from the terminal (Rule tab and Rules tab), Screen layout (terminal tabs and popup tabs fit the screen); new requirements for the item popup tabs, the Rules tab and the add slot.
- `shuttles/stock-rules`: Rule editing in the terminal (Rule tab with steppers and validation instead of the Rule mode).
- `foundation/gui-widgets`: a one line number stepper.

## Impact

- `shuttles/.../client/StorageTerminalScreen` (dialogs replaced by the popup, Stock and Rules tabs, click routing), new `client/ItemPopup` (tabbed popup with the three tab pages) and `client/RulesList` (rows, add slot, scroll), `terminal/TerminalLayout` (terminal tab strip, popup tab strip, Rule tab rects, Rules list rows, `rowsFor` keeps room for the strip), `menu/StorageTerminalMenu` (no change to rule payloads expected).
- `shuttles/.../integration/jei/ServusShuttlesJeiPlugin` (ghost ingredient handler for the add slot).
- `foundation/.../client/InlineStepper` (new), using `foundation/.../ui/AmountSteps`.
- Lang strings in `shuttles/.../datagen/ServusShuttlesLanguageProvider` and the Foundation language provider.
- JUnit: `TerminalLayoutTest` (new areas, max height), `RuleInputTest` (Rule tab validation), Foundation `AmountStepsTest` for the inline steps.
- Spec deltas for `shuttles/storage-terminal`, `shuttles/stock-rules`, `foundation/gui-widgets`.

## Non-goals

- The Limit group (cap and export thresholds), optional thresholds and station columns in the Rules tab: `m8-rule-cap` and `m8-rule-export`.
- The other screens of the PLAN point (Field Head amounts, Fluid Gantry keep steps, Production Terminal lists) on the Foundation widgets.
- Moving the rule list into the Warehouse Controller GUI or adding SEND and KEEP rules between sites.
- The order grid and its amount panel (opened by the Craft button) keep their flow and get no tabs.
