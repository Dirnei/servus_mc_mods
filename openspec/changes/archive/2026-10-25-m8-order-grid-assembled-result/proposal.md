# Proposal

## Why

A Heavy Duty Rack cannot be ordered or saved from the Storage Terminal's order grid. The grid shows the plain rack, while the server's preview carries the wood of the planks, so the screen never accepts the preview: Save as pattern never shows and Start stays greyed out with "Calculating..." and no reason. The same happens for every recipe that writes a component into its result (every wood or material block). The recipe viewer fill also picks items the warehouse does not hold: the Production Terminal takes the first item of each ingredient, and both terminals put a bare item without its wood (a Rail without material) into a cell whose real stock always carries one, so the pattern waits for an item that nothing stores or makes.

This is a bug found in play in M8 On-demand autocrafting (Storage Terminal order grid, Production Terminal, patterns).

## What Changes

- The order grid's result and the Production Terminal editor's preview show what the recipe really makes from the cells, with the components the recipe adds (an oak rack from oak planks), the same stack the server's preview and the saved pattern use. Save as pattern, Next and Start then work for these recipes.
- The JEI fill of both terminals sets each cell to the stocked stack that the recipe's ingredient accepts and that the line's warehouse has the most of, with that stack's components (an oak Rail, not a bare Rail). Only when the warehouse has none of them does a cell take the ingredient's first item.
- The Production Terminal's JEI fill now chooses by the warehouse's stock like the Storage Terminal's does, instead of always taking the first item.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `shuttles/storage-terminal`: order grid result is the assembled result; JEI fill chooses stocked stacks with their components.
- `shuttles/production-terminal`: editor preview is the assembled result; JEI fill chooses by the warehouse's stock.

## Impact

- Client screens: `StorageTerminalScreen` (order grid result), `ProductionTerminalScreen` (editor preview).
- Server fill: `StorageTerminalMenu.fillGhosts`, `ProductionTerminalMenu.openEditorWith`, `CraftingGrid.templateOf` (or a shared helper beside it).
- `m8-terminal-scrollbar-drag` and `m8-terminal-search-blur` are in flight and also touch `StorageTerminalScreen` and the storage-terminal spec, in other requirements and other code (scrollbar, search box). The user asked for this fix now; whichever lands second rebases.

## Non-goals

- Patterns that accept any variant of an ingredient (Exact or Any per pattern, the CPU picking the most stocked variant). That is the follow-up change `m8-pattern-variants`.
- Changing how a mismatched preview is shown; with matching results the case no longer comes up.
- Patterns already saved with a bare item or an unstocked wood are not rewritten; the player edits or saves them again.
