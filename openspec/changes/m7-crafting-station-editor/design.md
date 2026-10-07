# Design

## Context

- The Assembler (`manufacturing`) already has the editor flow: `AssemblerMenu` keeps an editor state in a synced data slot (`-1` closed, `-2` new, else the index being edited), nine `PatternSlot` ghost slots that are active only while editing, buttons `BUTTON_OPEN_NEW`, `BUTTON_SAVE`, `BUTTON_DELETE`, `BUTTON_CANCEL`, `BUTTON_OPEN_EDIT_FIRST + i`, and `openEditorWith(recipeId, player)` for JEI. `AssemblerScreen` draws the popup over a dimmed machine area with a preview slot and three buttons. JEI transfer is `AssemblerRecipeTransferHandler`, which sends `ConfigureRecipePayload(recipeId)`.
- The Crafting Station (`storage/crafting`) has a ghost grid on the main screen, a Save widget and cells that delete on click (`CraftingStationMenu`, `CraftingStationScreen`, 176 wide, with the Lib `SideConfigPanel` beside it).
- Storage may depend only on Lib, so it cannot use the Assembler's classes.

## Goals / Non-Goals

**Goals:**
- One editor look for both blocks, with the shared drawing code in Lib.
- The station's server side mirrors the Assembler's editor state machine.

**Non-Goals:**
- Moving the menu logic (slots, save rules) into Lib: the rules differ (assembling recipes, research, recipe slots from machine stats) and each menu stays in its mod.

## Decisions

### D1. Lib `RecipeEditorPopup` for drawing and hit testing
`lib/client/RecipeEditorPopup` draws the popup frame, the dimming over a given machine area, the 3 by 3 grid frame at the pattern slot positions, a preview slot with the matched result, and the Save, Delete (only when editing an existing recipe) and Cancel buttons; it answers which button a click hit. It takes the positions as constructor values and a supplier for the preview stack. Lib `block/RecipeEditorButtons` (common code, the menus use it on the server) holds the shared button ids, the editor states and the grid offset (open new, save, delete, cancel, open edit base) so both menus use the same numbers. `AssemblerScreen` switches to it without changing its positions, so the Assembler looks and behaves as before.
Alternative: copy the popup into Storage. Rejected, two copies of the same UI drift apart; this is the second user, which the PLAN's "grow Lib when a second mod needs it" rule asks for.

### D2. Station editor state like the Assembler's
`CraftingStationMenu` gets a synced data slot for the editor state and moves the nine ghost slots into the popup, active only while editing (`Slot.isActive`). Buttons: open new clears the grid; open edit `i` loads recipe `i`'s template; Save adds or, when editing `i`, replaces recipe `i` through the existing `CraftingStationBlockEntity` rules (match, duplicate, slots full) and closes on success, else sends the action bar message and keeps it open; Delete removes recipe `i`; Cancel clears and closes. Closing the menu clears the grid. The preview is computed on the client from the grid with the same vanilla `RecipeManager` lookup the block entity uses.

### D3. Layout
`imageWidth` 200, `imageHeight` 166, machine area 80 tall: nine inputs as a 3 by 3 block at x 8, the recipe cells as a 2 by 2 block at x 72 (a column of four would not fit the height), plus at x 112, a progress bar at x 114, the four outputs as 2 by 2 at x 140, the player inventory at x 20, y 84. The popup sits at x 20, y 4. The active recipe cell gets the Assembler's highlight from the station's active index, synced by the menu together with the recipe slot count (the client of a dedicated server has no datapack stats). The side panel stays beside the GUI. A new texture `crafting_station.png` generated like the other GUI textures. Refusals go to the action bar instead of the old message line.

### D4. JEI transfer, soft dependency
`storage/build.gradle` gains the same `compileOnly` JEI API lines as Manufacturing (no `localRuntime`: dev client runs get JEI through Manufacturing, and the storage-only runs check that Storage loads without it). `storage/integration/jei/ServusStorageJeiPlugin` (`@JeiPlugin`) registers `CraftingStationRecipeTransferHandler` for `RecipeTypes.CRAFTING` and `CraftingStationMenu`; it sends a new `crafting/OpenStationEditorPayload(recipeId)` registered in `terminal/ServusStoragePayloads`, and the server calls `CraftingStationMenu.openEditorWith`. No class outside `integration/jei` references JEI, so Storage loads without it (checked by launching the server without JEI in its run).

### Pure Java vs Minecraft
- No new pure logic; the save rules are the existing ones.
- GameTest (menu driven with a mock player, as the Assembler tests do): open new, fill grid, save adds sticks; save with no match keeps the editor open; edit replaces; delete removes; cancel keeps; slots full refused.
- Manual: layout, popup, JEI transfer.

## Risks / Trade-offs

- [Refactoring the Assembler's popup could change its look] → Positions passed in unchanged; play check the Assembler too.
- [GameTests never open real menus over the network] → After touching `ServusStoragePayloads`, grep that every payload type is registered before launching (a missing registration only shows as a client crash).

## Migration Plan

None, nothing is released. Recipes configured on existing stations stay; the grid content of an open old GUI is not kept.
