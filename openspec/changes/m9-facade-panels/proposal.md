# Proposal

## Why

A facade today turns the whole duct into a full block of its material. That hides the duct completely, but it is not what a facade should be: a thin cover on one side of a duct, like a wall panel. Players also have no way to stop two neighbouring ducts or cables from joining, because ducts link to every neighbour that carries the same line. Panels solve both: a solid panel covers a side and closes it, a hollow panel covers a side and lets the duct pass through.

PLAN.md section 2 already promises "Solid or hollow variants", and hollow facades were deferred from M5. This is a play feel follow-up found during M9, added to the M9 list as item 1d after `m9-facade-jei`.

## What Changes

- A Facade becomes a 1 pixel thick panel on one face of a Utility Duct, placed on the face the player clicks. A duct can carry up to six panels, one per face, each of its own block. The whole-block facade is removed; six solid panels give the same look.
- A solid panel closes its side: the duct draws no arm there and connects to nothing through that face, neither to a neighbouring duct nor to a machine or inventory. Item, energy, fluid and data networks split at a solid panel.
- A new Hollow Facade is a panel with a hole in the middle. The duct's arm passes through the hole and the side keeps connecting as before.
- Eight Facades of the same block in a ring around an empty centre craft eight Hollow Facades of that block. The Facade recipe (full block plus iron ingot gives six) is unchanged.
- Sneaking with the Configuration Carrot on a panel takes that one panel off. On a duct without a panel at the clicked spot it removes the duct as today, dropping the duct's panels as well.
- Panels have their own outline and collision on top of the duct's shape.
- The Engineer Goggles overlay shows every duct that carries at least one panel.
- The creative tab and JEI also list hollow examples of the example blocks, with the ring recipe as their JEI example.
- PLAN.md gets M9 item 1d, and the facade line in section 2 describes the panels.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `foundation/facades`: placing and removing per face, solid panels closing a side, hollow panels and their recipe, panel rendering and shape, the overlay rule, hollow examples in the creative tab.
- `foundation/duct-appearance`: "Facades cover everything" becomes per side panels drawn next to the duct's own parts.

## Non-goals

- Panels on blocks other than Utility Ducts (no standalone cover block).
- Turning a Hollow Facade back into a Facade.
- A block rendering item icon for facades; both items keep a generic icon (the hollow one gets its own).
- Light blocking by panels, and goggles on the player model.
- Migration of existing whole-block facades in worlds. The project is unreleased; such a duct loses its facade.

## Impact

- `foundation` module, `logistics/duct`: `UtilityDuctBlockEntity` stores six panel slots instead of one facade, `computeArm`, `dataSideOpen` and the item and fluid capabilities respect solid panels, the item, energy and fluid network types get a connection rule that refuses a closed side, `UtilityDuctBlock` gets panel shapes, panel picking for the carrot and per face `getAppearance`.
- `logistics/facade`: `FacadeItem` places per face and knows solid or hollow, a new `HollowFacadeRecipe`, a new `hollow_facade` item.
- `logistics/client`: `UtilityDuctModel` draws panels next to the duct parts, `DuctBlockColor` tints per panel, `DuctOverlayRenderer` picks ducts with panels.
- JEI plugin, creative tab, datagen (item model, recipe, lang), a hollow facade item texture.
- Builds on `m9-facade-jei` (example list, JEI subtypes, per block recipes), which should be archived first.
- `PLAN.md` M9 list and section 2.
