# Proposal

## Why

PLAN.md "Open: Foundation", `m5-duct-look`. The Utility Duct is the block players see most, and today it reads as flat coloured bars: the textures are stretched over every face whatever its length, a stripe runs along the arm on one side and across it on the next, and the line items are plain dots. In a texture preview (2026-10-14) the user picked a new look: the textures of option B (real hoses and cables) with the icons of option C (a cross-section that shows the cable colour at the end).

## What Changes

- **Line textures** in the style of option B, each a strip drawn so it runs along the duct:
  - Item Pipe: corrugated orange hose.
  - Fluid Pipe: blue hose with a spiral wrap and a brass sleeve.
  - Power Cable: ribbed red rubber.
  - Data line by tier: white braid (Data Cable), white and gray braid (Duplex), black braid with a cyan tracer (Fast).
- **Multi line bands** built from the same strips, so a two to four line duct shows each line's hose side by side.
- **Textures run along the duct**: on every side face of an arm or a straight centre the pattern runs along the arm and tiles block by block, the same on all four sides, never mirrored, never stretched.
- **Fittings**: every centre where arms meet from more than one axis, and every centre that bends between two heights, is a moulded black rubber fitting with a steel cap, for any number of lines (today only a junction of two or more lines is a neutral cube).
- **Ring** texture redrawn to sit on the new hoses; it stays tinted in the channel or tier colour.
- **Line item icons** in the style of option C: a short stub seen from the front showing its cross-section (rim, colour liner, core: an item, water, copper, one to three data cores). The docs icons are exported again.
- A committed texture generator, so the line textures, band combinations, fitting, ring and icons can be rebuilt.
- The resting pose rules stay as archived in `m9-duct-resting-pose`: height only, bends in the middle of the block, a junction at a machine keeps straight arms.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `foundation/duct-appearance`: "Each line has a color" (hose and cable patterns instead of plain colours, the data line by tier instead of teal, fittings at every multi axis or bending centre), "Channel ring at endpoints" (the data ring shows the tier, as the data cable spec already says), and new requirements for textures that run along the duct and for the line item icons.

## Impact

- Textures in `foundation/src/main/resources/assets/servus_foundation/textures/block/duct_*.png` and `textures/item/{item_pipe,fluid_pipe,power_cable,data_cable,duplex_data_cable,fast_data_cable}.png`; the `_v` band variants go.
- `foundation/.../logistics/datagen/DuctPartModels.java` (face UVs along the run, one band texture per line set, fitting texture), `DuctParts` (centre names), `client/DuctPoseQuads.java` and `client/UtilityDuctModel.java` (UVs along the arm after stretching, fitting for multi axis and grown centres, data tier reskin of the new names).
- A generator script under `tools/`, the docs icons in `docs/src/icons/servus_foundation/`, `docs/.../foundation.md`.
- No gameplay, network, save or datapack change.

## Non-goals

- Rounded or angled bends (the user wants the 90 degree bend in the middle of the block, as built).
- Changing duct sizes, collision or the resting pose.
- New looks for facades, panels, the Fluid Tank or machines.
- Animated textures (flowing water or moving items behind a window).
