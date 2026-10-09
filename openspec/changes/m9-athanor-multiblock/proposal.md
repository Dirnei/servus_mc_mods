# Proposal

## Why

The Calciner and the Coagulator are builds now; the Athanor, the heart of the lab, already grows with the rings built around it, but it can stand on its own and its stages are only described in words. It becomes a build like the others: it stands on a brick hearth, is formed with the Journal, and every stage of its structure is shown as a schematic in the journal. Done autonomously as a first proposal; the structure is to be discussed with the user.

PLAN.md milestone: M9 (Manufacturing becomes alchemy), item 6d `m9-athanor-multiblock`.

## What Changes

- The Athanor stands on a hearth of 3 by 3 vanilla Bricks directly below it. Right clicking the Athanor with Paracelsus' Journal checks the hearth and forms the Athanor, making the player its owner; a missing brick is named.
- Only a formed Athanor crafts. The tier rings (Albedo's Salt Crystal ring and later ones) stay as they are: data, checked on use and slowly. The same slow check also checks the hearth and unforms the Athanor when a brick is gone; a broken brick right under it unforms it at once.
- The journal shows the Nigredo hearth on the Athanor page and the Albedo ring on the Salt Crystal Block page as multiblock schematics with Visualize, generated from the hearth layout and the shipped tier data.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `manufacturing/athanor`: crafts only when formed on its hearth; the slow tier check also checks the hearth; forming with the Journal.
- `manufacturing/journal`: schematic pages for the Athanor's stages.

## Impact

- Manufacturing: `alchemy/athanor` (block, block entity, a pure `AthanorLayout`), `ServusManufacturingMultiblockProvider`, journal pages, lang, GameTests.
- PLAN.md M9 item 6d.

## Non-goals

- A new formed model or a GUI less Athanor: its look and its GUI stay for now, to be discussed.
- Changes to the tier data or Athanor recipes.
- Citrinitas and Rubedo stages.
