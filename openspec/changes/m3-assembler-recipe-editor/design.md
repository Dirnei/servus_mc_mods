# Design

## Context

`AssemblerMenu` holds machine slots, nine `PatternSlot`s over the block entity's pattern handler and button ids for add, clear and remove. `AssemblerScreen` draws the recipe panel outside the GUI. JEI sends `ConfigureRecipePayload` which calls `addRecipe`. See proposal.md.

## Decisions

### D1: One menu, an editing state
The editor is not a second menu. `AssemblerMenu` keeps an extra `SimpleContainerData(1)` with `editing`: -1 closed, -2 new recipe, otherwise the index being edited. Slots are fixed at popup coordinates; `PatternSlot.isActive` is true only while editing and the machine input and output slots are inactive while editing, so the screen neither draws nor clicks them under the popup. Player inventory slots stay active (the popup ends above them).

### D2: Buttons
Ids: 102 open new (clear pattern, editing -2), 200 plus index open existing (load the template into the pattern, editing index), 103 save, 104 delete, 105 cancel. Save calls `addFromPattern` or `replaceFromPattern(index)`, both returning success; on success the pattern is cleared and editing is -1. Delete removes the edited index. Cancel clears and closes.

### D3: Block entity helpers
`match(level, cells)` (static) finds the configured recipe for a layout: assembling recipes first, then crafting; used by save and by the client preview. `loadPattern(template)` copies a template into the pattern with counts. `replaceFromPattern(index, player)` applies the duplicate and research rules, then replaces the entry and resets progress when it was active. `configuredFor(id, player)` resolves a recipe id to a `ConfiguredRecipe` for the JEI path.

### D4: JEI
The payload handler calls `menu.openEditorWith(id, player)`: resolve the recipe, load its template into the pattern and set editing to -2 (or keep the current index when already editing).

### D5: Screen
Main screen: recipe cells at (124, 17 + i * 18), plus button at (146, 17), progress bar at (182, 38), outputs and energy unchanged. Popup at (48, 8) size 160 by 72: grid at (54, 20), preview at (116, 38), Save, Delete and Cancel at x 140, y 20, 38, 56. The preview resolves `match` on the client level each frame. Tooltips explain cells and buttons. The texture loses the middle grid.

## Pure Java vs Minecraft

All Minecraft. Verified by build, the Manufacturing GameTest (the assembler path still adds recipes by id) and the client launch; the popup flow is checked in game by the user.

## Risks / Trade-offs

- [Pattern grid shared between players of one assembler] -> Same as before; only one editor is normally open per machine.
