# Proposal

## Why

A Heavy Duty Rack pattern saved with oak planks only ever makes oak racks, and the Elevator pattern only ever uses the one Rail it was saved with, because `m8-pattern-variants` allows Any only where the result stays the same. The user wants to press JEI's + on the rack recipe and get a pattern that takes any planks, with the rack's wood following the planks, without toggling anything by hand.

PLAN.md milestone M8 On-demand autocrafting. PLAN.md has no item for it: it is the non-goal "Any for recipes whose result changes with the ingredients" of `m8-pattern-variants`, asked for by the user in the play check. The rules come from the exploration of 2026-10-25: the result follows the inputs, a generic order uses the most stocked variant, a specific request picks the variant whose result matches, below an Exact pattern everything stays fixed, one variant per craft step.

**Order of work.** Applied after `m8-shared-recipe-grid`, which is applied after `m8-pattern-variants` and `m8-terminal-tabs` are archived. It changes the mode button rules and the result preview of the shared view that `m8-shared-recipe-grid` builds.

## What Changes

- **Any where the result follows the inputs.** A pattern may be Any also when other accepted items change only the result's components, not its item or count: the rack's wood follows the planks, the Elevator's material follows the Rail. Such a pattern is a material pattern. A recipe where another item gives another item or count stays Exact.
- **JEI + gives Any.** Since a rack grid now allows Any, JEI's transfer on the rack recipe opens the editor or the order grid set to Any, as for sticks.
- **Specific requests pick their variant.** A request, rule or recipe step that needs a spruce rack uses the spruce planks variant of an Any rack pattern; with no spruce planks in stock the CPU crafts them first. One variant per craft step.
- **Generic orders take the most stocked variant.** The Storage Terminal lists one craftable entry per material pattern, the item without its material (for example "Heavy Duty Rack, wood follows the planks"). Crafting it makes the variant whose inputs the warehouse holds most of, and the request then names that variant.
- **Rails and other component inputs.** A cell whose item carries a material (the Elevator's Rail) accepts the stocked Rails and the Rails the line can make, so a spruce Elevator uses or crafts a spruce Rail.
- **Preview.** The shared grid's result of a material pattern set to Any cycles through the results of its variants once per second, with a tooltip line "Wood follows the planks"; the amount panel shows the variant the CPU will make.
- **Duplicates.** A line holds one Any pattern per recipe and result item; an Exact pattern of the same recipe for one wood can sit beside it.
- **Exact stays fixed.** An Exact pattern needs exactly its items one level down: an Exact oak Elevator pattern asks for an oak Rail, which an Any Rail pattern then makes from oak planks.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `shuttles/patterns`: when a pattern can be Any (result components may follow the inputs), how the CPU plans material patterns for specific and generic orders, the duplicate rule.
- `shuttles/storage-terminal`: one craftable entry per material pattern; the order grid's mode button for material grids.
- `shuttles/production-terminal`: the recipe editor's mode button and the Patterns tab tooltip for material patterns.

## Non-goals

- Mixing variants in one craft (two oak and two spruce planks in one rack).
- A generic request or stock rule for "any wood" that ships whatever racks are in stock; generic entries only craft.
- Material variants for Machine Adapter recipes or recipes whose item or count changes with the ingredients.
- Changing the wood rule of `RackWood` or `MaterialPick`.

## Impact

- `shuttles/pattern/PatternVariants` (material verdict, varying components, candidates of component cells), new pure `pattern/VariantExpansion` (the variants a material pattern offers, capped), `pattern/PatternRunners` (one planner recipe per variant), `pattern/EncodedPattern` (`withKeys` re-assembles the result for material patterns, `duplicates` by result item).
- `order/OrderBook` (generic key resolution before planning, `runs` for material variants), `cpu/CpuBlockEntity.patternFor`, `cpu/CpuOrders`, `terminal/TerminalCraftables` (one generic entry), `terminal/TerminalRequests` (the request names the resolved variant).
- `client/PatternGridClient` (cycling preview, tooltip), `client/ProductionTerminalScreen` (Patterns tab tooltip), lang entries.
- No saved data change: material patterns are Any patterns; rack and Elevator patterns saved before stay Exact.
