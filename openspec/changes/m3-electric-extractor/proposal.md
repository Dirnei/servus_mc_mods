# Proposal

## Why

Research needs something to unlock. The Electric Extractor is the first tech reward in the plan: twice the extraction speed, powered instead of fueled. PLAN.md milestone M3, item "Electric Extractor".

## What Changes

- **Electric Extractor**: a powered machine that sits on a deposit and produces its yield every 2 seconds (40 ticks by default) at 20 FE/t. Output slot only, same deposit rules as the burner version.
- The shared deposit lookup moves into a small helper used by both extractors.
- Recipe: assembled, not hand crafted. An assembling recipe (burner extractor, iron gears, copper wire) carrying the `tech` field `electric_extractor`, so the assembler refuses it until the tech is researched. This sets the rule for every later research gated machine.
- Datagen, textures, GUI background, stats JSON, and the tech's unlock id pointing at the new recipe.

## Capabilities

### New Capabilities
- `manufacturing/electric-extractor`: what the electric extractor needs and produces, and how it is obtained.

### Modified Capabilities
None.

## Non-goals

- Extractor upgrades or Mk2 tiers.
- Changes to deposits or yields.

## Impact

- New block, block entity, menu, screen layout and GUI texture in Manufacturing.
- `AssemblingRecipeBuilder` gains `tech()`.
