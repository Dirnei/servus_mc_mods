# Proposal

## Why

The Calciner and the Coagulator are builds now; the Athanor, the heart of the lab, already grows with the rings built around it, but it can stand on its own and its stages are only described in words. It becomes a build like the others: it stands on a brick hearth, is formed with the Journal, and every stage of its structure is shown as a schematic in the journal. Done autonomously as a first proposal; the structure is to be discussed with the user.

PLAN.md milestone: M9 (Manufacturing becomes alchemy), item 6d `m9-athanor-multiblock`.

## What Changes

- The Athanor stands on a hearth of 3 by 3 vanilla Bricks directly below it. Right clicking the Athanor with Paracelsus' Journal checks the hearth and forms the Athanor, making the player its owner; a missing brick is named.
- Only a formed Athanor crafts. The tier rings (Albedo's Salt Crystal ring and later ones) stay as they are: data, checked on use and slowly. The same slow check also checks the hearth and unforms the Athanor when a brick is gone; a broken brick right under it unforms it at once.
- Three Foundation Fluid Tanks join the build (play check 2026-10-10): two blocks from the Athanor to its left, behind it and to its right, seen from its front, on its level and outside the Albedo ring; the front stays open. They never touch, so they never merge; a tank grows by stacking. Any tank holds any principle. The Athanor takes each craft's principles straight from them; its own tanks, its bucket input and its fluid sides go away, and pipes and buckets fill the tanks.
- The formed Athanor gets a look and loses its GUI (play check 2026-10-10): a brick tower furnace about one and a half blocks tall with a small iron fire door showing the tier coloured flame and an iron plate on top where the item rests; copper pipes arch from the tower over the ring into each tank. The item is put on top by hand, the result then lies on top and an empty hand takes it; sneaking with an empty hand takes the item back. Hoppers and Item Pipes put items in and take results out. Jade shows the tier, the progress and what the tanks hold.
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

- Changes to the tier data or Athanor recipes.
- Citrinitas and Rubedo stages.
