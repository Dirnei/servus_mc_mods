# Proposal

## Why

The assembler GUI shows the pattern grid all the time although it is only needed while adding a recipe, and removing a recipe is a bare click on its icon. Playtest feedback (October 6, 2026): make it a two step UI. The main screen shows the configured recipes; a popup editor opens for adding, editing and deleting. Revises the M3 change "assembler crafting".

## What Changes

- Main assembler screen: inputs, outputs, energy, progress and the configured recipe cells inside the GUI, plus a plus button. No pattern grid.
- **Recipe editor popup**: opened by plus (empty) or by clicking a configured recipe (prefilled with its layout). Holds the three by three ghost grid, a live preview of the matched result, Save, Delete (only for an existing recipe) and Cancel. Save replaces the edited recipe or adds a new one with the existing cap, duplicate and research rules; failures keep the editor open and show the reason.
- JEI transfer opens the editor prefilled with the recipe instead of adding it straight away.
- While the editor is open the machine slots underneath are inactive so clicks go to the popup.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `manufacturing/assembler`: recipe selection and recipe viewer transfer requirements.

## Non-goals

- More than one pattern per machine shown at once, drag and drop between recipes, sorting.

## Impact

- `AssemblerMenu` gains an editing state synced through container data; button ids change. `AssemblerBlockEntity` gets pattern load and replace helpers. New GUI texture without the middle grid.
