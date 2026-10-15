## Why

Once a Coagulator has a recipe the player cannot take it back; the only way to stop it is to break the build or starve it. Players want to pick "no recipe" to stop it.

## What Changes

- The Coagulator's recipe popup starts with a "No recipe" entry (a barrier icon). Picking it clears the selection and the Coagulator stops; the select button reads "No recipe".
- The entry is marked in the popup while nothing is selected and has its own tooltip.

## Capabilities

### New Capabilities

### Modified Capabilities
- `alchemy/coagulator`: the player can clear the recipe selection.

## Impact

- `alchemy/.../CoagulatorMenu.java`: a button id that clears the selection.
- `alchemy/.../client/CoagulatorScreen.java`: the extra popup entry, its tooltip and the button label.
- Lang keys for the entry (datagen).
