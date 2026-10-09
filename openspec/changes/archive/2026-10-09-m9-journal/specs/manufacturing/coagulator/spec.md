# Spec Delta

## ADDED Requirements

### Requirement: Coagulator reports to the journal
The Coagulator SHALL remember the player who placed it as its owner and SHALL report every finished cycle to the journal of the owner's team as a `coagulate` event for its result with the result's count.

#### Scenario: Salt Crystal counts
- **WHEN** a player's Coagulator makes a Salt Crystal
- **THEN** the player's team gets a `coagulate` event for one Salt Crystal

## MODIFIED Requirements

### Requirement: The player picks the recipe
The Coagulator SHALL run exactly one selected coagulation recipe. Its GUI SHALL list every coagulation recipe with its result and needs, and clicking one SHALL select it, unless the recipe names a journal chapter the owner's team has not finished. The selection SHALL persist and survive dismantling with the carrot. Without a selection the Coagulator SHALL do nothing.

#### Scenario: Switch recipe
- **WHEN** the player clicks the Gunpowder recipe in a Coagulator that was making Salt Crystals
- **THEN** the Coagulator makes Gunpowder from then on and the selection survives a world reload

#### Scenario: Locked by the journal
- **WHEN** the player clicks a recipe that needs a chapter their team has not finished
- **THEN** the selection stays as it was
