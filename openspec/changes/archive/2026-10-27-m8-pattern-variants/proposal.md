# Proposal

## Why

A pattern today holds one concrete item per cell, and the CPU only takes exactly that item from stock: a sticks pattern saved with oak planks never uses the spruce planks the warehouse is full of, and orders wait for oak planks or craft them first. Players expect "any planks" for recipes whose result does not care which planks went in. `m8-order-grid-assembled-result` left this as its follow-up.

Milestone M8 On-demand autocrafting (patterns, Crafting CPU, Production Terminal). PLAN.md has no item for it: it was found in the play check of the order grid.

## What Changes

- **Exact or Any per pattern.** A pattern gets a mode. Exact works as today. Any lets the CPU use, for each cell, any item the recipe's ingredient accepts there, as long as the result stays the same.
- **Any only where the result stays the same.** A pattern can be Any only when every item each cell's ingredient accepts gives the same result as the pattern's own items: sticks, chests, torches and crafting tables from any planks or logs. A recipe whose result takes its material from the ingredients, such as the Heavy Duty Rack's wood, stays Exact; the editor says why.
- **The CPU picks the most stocked variant.** When it plans an Any pattern, the CPU chooses for the cells that share an ingredient the accepted item the warehouse holds most of, and uses that variant for the step; with none in stock it keeps the pattern's own items. The Crafting Station crafts that variant.
- **Editor toggle.** The recipe editor of the Production Terminal gets an Exact / Any button beside Save. A new pattern starts as Any when Any is possible; existing patterns stay Exact until changed. The Patterns tab marks Any patterns.
- **Duplicates.** A line may hold one Any pattern per recipe and result; a second one is refused like a duplicate today.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `shuttles/patterns`: a pattern's mode, when Any is possible, how the CPU plans an Any pattern, and the duplicate rule for Any patterns.
- `shuttles/crafting-station`: a pattern job crafts the variant its step chose.
- `shuttles/production-terminal`: the Exact / Any button in the recipe editor and the Any mark in the Patterns tab.

## Non-goals

- Any for recipes whose result changes with the ingredients (one rack pattern for every wood); a later change if play asks for it.
- Mixing variants inside one craft (two oak and two spruce planks in one crafting table); one variant per ingredient per step.
- Any for Machine Adapter recipes, which already come from the recipe's ingredient one item at a time.
- An Any toggle in the Storage Terminal's order grid; "Save as pattern" from there saves with the default (Any when possible).

## Impact

- `shuttles/pattern/EncodedPattern` gains the mode (codec with Exact as default, so saved patterns load unchanged), `PatternEditing` computes whether Any is possible and enforces the duplicate rule.
- `shuttles/order/OrderBook` turns an Any pattern into a planner recipe with the chosen variant; `OrderStep` and the Crafting Station job carry the variant template.
- `shuttles/menu/ProductionTerminalMenu`, `client/ProductionTerminalScreen`, `foundation/block/RecipeEditorButtons` and `foundation/client/RecipeEditorPopup`: the toggle.
- No new blocks, items or datapack values.
