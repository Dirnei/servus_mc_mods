# Spec Delta

## Purpose

Facades hide ducts behind the look of ordinary blocks; the overlay brings them back for whoever needs to see them.

## ADDED Requirements

### Requirement: Facade crafting
Crafting any full, block entity free block together with one iron ingot SHALL give six Facades of that block. The facade item SHALL name its block.

#### Scenario: Stone facade
- **WHEN** stone and an iron ingot are in the crafting grid
- **THEN** six "Facade (Stone)" result

#### Scenario: Not a full block
- **WHEN** a slab and an iron ingot are in the grid
- **THEN** nothing results

### Requirement: Applying and removing
Using a Facade on a duct without one SHALL set the facade and consume the item. Sneak wrench on a facaded duct SHALL drop the facade item and keep the duct; breaking the duct SHALL drop the facade too.

#### Scenario: Apply and take off
- **WHEN** a player uses a stone facade on a duct and then sneak wrenches it
- **THEN** the duct looks like stone in between and the facade item comes back

### Requirement: Rendering as the block
A facaded duct SHALL render as its facade block, with that block's render types and particles, and SHALL have a full cube shape. Without a facade the duct renders its centre and arms.

#### Scenario: Looking at a wall
- **WHEN** a row of ducts carries stone facades in a stone wall
- **THEN** the wall looks uniform

### Requirement: Hidden duct overlay
While a player wears Engineer Goggles or holds the wrench, facaded ducts within 32 blocks SHALL be drawn as a translucent overlay visible through blocks, showing the centre and the connected arms.

#### Scenario: Reveal
- **WHEN** a player puts on the goggles next to a facaded duct row
- **THEN** the ducts show through the wall; taking the goggles off hides them again
