# Proposal

## Why

The second early storage block: a rack that shows what sits on it. Where crates hold one type in bulk, racks hold a few stacks of anything and present them on the shelf. PLAN.md milestone M4, item "Shelving racks with visible contents".

## What Changes

- **Shelving Rack**: four compartments in a two by two grid on the front, each holding one stack. Right click a compartment with an item puts the held stack there (merging with a matching stack); right click an empty compartment with an empty hand does nothing; right click a filled compartment with an empty hand takes the stack. Sneak right click takes one item.
- Item handler with four slots on every side, so hoppers and ducts work.
- Renderer draws each compartment's stack on the front face, plus the count when above one.
- Rack is a warehouse network node like the crate. Wrench rotates; sneak wrench dismantles with contents. Breaking spills.
- Crafting recipe: planks and sticks.

## Capabilities

### New Capabilities
- `storage/shelving-rack`: compartments, hand interaction, automation access, display.

### Modified Capabilities
None.

## Non-goals

- Variable compartment counts, item locks, labels.

## Impact

- Storage only. Shares the crate's wrench and network base patterns.
