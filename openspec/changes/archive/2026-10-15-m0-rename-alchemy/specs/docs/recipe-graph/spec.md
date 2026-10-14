# Spec Delta

## ADDED Requirements

### Requirement: Graph draws Servus recipes only
The Recipe graph SHALL draw every production and crafting recipe from the game data whose id is in a `servus_*` namespace, whatever it outputs. Recipes in any other namespace SHALL NOT appear. Items and tags SHALL appear only as inputs or outputs of the drawn recipes.

#### Scenario: Servus recipe is shown
- **WHEN** the game data has the Foundation power cable recipe
- **THEN** the graph shows that recipe with power cable as its output

#### Scenario: Vanilla recipe is hidden
- **WHEN** the game data has the vanilla recipe smelting raw iron into an iron ingot
- **THEN** that recipe is not in the graph

#### Scenario: Redstone is a leaf
- **WHEN** no Servus recipe outputs redstone and the power cable recipe uses it
- **THEN** redstone appears as a node with no incoming recipe

## MODIFIED Requirements

### Requirement: Mod colours and filter
Nodes SHALL be coloured by the mod of their id: one colour each for Foundation, Alchemy and Storage, and a neutral colour for vanilla items and tags. The user SHALL be able to show or hide each mod's recipes; an item node SHALL stay visible while any visible recipe uses or produces it.

#### Scenario: Only Storage
- **WHEN** the user hides Foundation and Alchemy
- **THEN** only Storage recipes and their inputs and outputs remain, and copper wire is gone

## REMOVED Requirements

### Requirement: Graph shows only Servus recipes
**Reason**: Its example recipe (Manufacturing grit smelting) no longer exists.
**Migration**: See "Graph draws Servus recipes only".
