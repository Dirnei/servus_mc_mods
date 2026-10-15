## Context

`CoagulatorBlockEntity.select(null)` already clears the selection and the Coagulator idles without one. The screen offers only recipes through `RecipePickerPopup`, indexed by `CoagulatorMenu.RECIPE_BUTTON_FIRST + index`.

## Decisions

- Menu button `RECIPE_BUTTON_NONE = RECIPE_BUTTON_FIRST - 1` (99, unused) calls `select(null)`.
- The screen hands the popup the barrier item as entry 0 followed by the recipe results, so popup index `i` is recipe `i - 1`; the marked index is `selectedIndex() + 1`, which is 0 while nothing is selected. The Foundation popup stays unchanged.
- The select button shows the barrier and "No recipe" while nothing is selected.

## Assumptions to confirm with the user

- The "No recipe" entry is the first cell of the popup, drawn as a barrier.
