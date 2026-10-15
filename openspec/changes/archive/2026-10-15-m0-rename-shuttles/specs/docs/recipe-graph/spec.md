# Spec Delta

## ADDED Requirements

### Requirement: Colours and filter per mod
Nodes SHALL be coloured by the mod of their id: one colour each for Foundation, Alchemy and Shuttles, and a neutral colour for vanilla items and tags. The user SHALL be able to show or hide each mod's recipes; an item node SHALL stay visible while any visible recipe uses or produces it.

#### Scenario: Only Shuttles
- **WHEN** the user hides Foundation and Alchemy
- **THEN** only Shuttles recipes and their inputs and outputs remain, and copper wire is gone

## REMOVED Requirements

### Requirement: Mod colours and filter
**Reason**: Servus: Storage is renamed Servus: Shuttles; its scenario names the old mod.
**Migration**: See "Colours and filter per mod".
