# Tasks

## 1. Storage Terminal

- [x] 1.1 In `shuttles/.../client/StorageTerminalScreen.java` `mouseClicked`: before the other handling, when the search box has focus and the press is not over it, unfocus the box (`search.setFocused(false)`, and `setFocused(null)` when it is the screen's focused element), then let the press go on as before. Keep `setInitialFocus(search)` and the right click clear. Verify with `gradlew :shuttles:build`.
- [x] 1.3 Play check fix: `StorageTerminalScreen.gridItemAt` gives the grid item under the mouse with its corner, and `integration/jei/ServusShuttlesJeiPlugin` reports it from `getClickableIngredientUnderMouse`, so JEI's R and U work on grid items. Check: `gradlew build`.
- [x] 1.2 Full `gradlew build`, tests green, game launches with all mods; in the play check type in the search box, then click the grid, a slot, a side button, the empty panel and outside the panel: each takes the focus and keeps the text; E closes the terminal and number keys swap with the hotbar; clicking the box focuses it again.

## Workflow follow-up

- Archive with `/opsx:archive`, add a Shuttles CHANGELOG line under Unreleased (Fixed: clicking anywhere in the Storage Terminal takes the focus from the search box).
