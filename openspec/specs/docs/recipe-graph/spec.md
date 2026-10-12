# docs/recipe-graph Specification

## Purpose

The Recipe graph page of the Balance lab shows how all Servus recipes connect, from vanilla raw materials to Servus products, so recipe and balance ideas can be discussed without starting Minecraft.

## Requirements

### Requirement: Graph shows only Servus recipes
The Recipe graph SHALL draw every production and crafting recipe from the game data whose id is in a `servus_*` namespace, whatever it outputs. Recipes in any other namespace SHALL NOT appear. Items and tags SHALL appear only as inputs or outputs of the drawn recipes.

#### Scenario: Grit smelting is shown
- **WHEN** the game data has the Manufacturing recipe smelting copper grit into a copper ingot
- **THEN** the graph shows that recipe with copper ingot as its output

#### Scenario: Vanilla recipe is hidden
- **WHEN** the game data has the vanilla recipe smelting raw iron into an iron ingot
- **THEN** that recipe is not in the graph

#### Scenario: Redstone is a leaf
- **WHEN** no Servus recipe outputs redstone and the power cable recipe uses it
- **THEN** redstone appears as a node with no incoming recipe

### Requirement: Layered layout from raw materials to products
The graph SHALL lay out item and recipe nodes in layers from left to right, with each recipe between its inputs and its outputs, so items nothing produces sit on the left and items nothing consumes sit on the right. Each edge SHALL show its count when it is more than one. The page SHALL stay usable at phone width by panning inside the graph box rather than scrolling the page.

#### Scenario: Power cable edges
- **WHEN** the graph draws the power cable recipe
- **THEN** copper ingot points into the recipe with label 6 and the recipe points to power cable with label 8

### Requirement: Recipe nodes show their kind
Each recipe node SHALL show what makes it: Crafting, Assembler, Crusher, Smelting or Blasting, or the recipe type for any other type. A special recipe SHALL show its name and a "special" note and SHALL have no edges.

#### Scenario: Facade
- **WHEN** the game data marks the facade recipe special
- **THEN** the graph shows a facade recipe node labelled special with no edges

### Requirement: Mod colours and filter
Nodes SHALL be coloured by the mod of their id: one colour each for Lib, Logistics, Manufacturing and Storage, and a neutral colour for vanilla items and tags. The user SHALL be able to show or hide each mod's recipes; an item node SHALL stay visible while any visible recipe uses or produces it.

#### Scenario: Only Storage
- **WHEN** the user hides Lib, Logistics and Manufacturing
- **THEN** only Storage recipes and their inputs and outputs remain, and copper wire is gone

### Requirement: Ancestor highlight
Selecting an item SHALL highlight that item, every recipe that produces it and, repeating backwards, their inputs and producing recipes down to the leaves. All other nodes and edges SHALL be dimmed. Selecting the item again or pressing Escape SHALL clear the highlight.

#### Scenario: Shuttle
- **WHEN** the user selects the shuttle
- **THEN** the shuttle recipe, the rail and its recipe, iron ingot, redstone and stick are highlighted and the Heavy Duty Rack recipe is dimmed

### Requirement: Item panel
Selecting an item SHALL open a panel with the item's name and mod, the drawn recipes that produce it with their inputs (shaped recipes drawn as their grid), and the drawn recipes that use it.

#### Scenario: Copper ingot uses
- **WHEN** the user selects copper ingot
- **THEN** "Used in" includes the power cable, data cable and copper wire recipes, and "Made from" lists the grit smelting and grit blasting recipes

### Requirement: Raw cost per item
The panel SHALL show the raw cost of one selected item: inputs divided by the recipe's output count, expanded through Servus items only, ending at vanilla items and tags. A Servus item no drawn recipe produces SHALL end as a marked "no recipe" entry, and a recipe cycle SHALL end as a marked cycle entry without looping.

#### Scenario: One power cable
- **WHEN** the user selects the power cable
- **THEN** the raw cost is 0.75 copper ingot, 0.25 redstone and 0.125 glass

#### Scenario: Vanilla items are not expanded
- **WHEN** the user selects copper wire, made from copper ingot in the Assembler
- **THEN** the raw cost is 0.5 copper ingot and does not continue into copper grit or raw copper

### Requirement: Route choice in the raw cost
When a Servus item in the cost tree has more than one producing recipe, the raw cost SHALL use one route per item, by default the first recipe by id, and the panel SHALL let the user switch that item's route and recompute the cost.

#### Scenario: Two routes
- **WHEN** a Servus item has a crafting recipe and an assembling recipe
- **THEN** the panel shows a route choice for it and the cost changes when the user switches route

### Requirement: Zoom and pan
The user SHALL be able to zoom the graph with the mouse wheel around the cursor and with a pinch on touch screens, pan it by dragging, and use `+`, `-` and Fit buttons. The graph SHALL start fitted to its box. Selecting an item SHALL zoom and pan so its highlighted ancestors fill the box. Clicking a node SHALL still select it.

#### Scenario: Fit
- **WHEN** the user presses Fit
- **THEN** the whole graph is visible inside the box

#### Scenario: Zoom to the selection
- **WHEN** the user selects the shuttle
- **THEN** the shuttle, the rail, iron ingot, redstone and stick are all inside the visible box

### Requirement: Item icons
An item with an exported icon SHALL show that icon next to its name on its node, in the panel's recipe grid and in the raw cost. Items without an icon and all tags SHALL show their name only.

#### Scenario: Servus icon
- **WHEN** the site is built from the repository
- **THEN** the shuttle node shows the shuttle icon

#### Scenario: Vanilla icon missing
- **WHEN** the site is built without the local vanilla icons
- **THEN** the redstone node shows "Redstone Dust" without an icon and nothing fails to load
