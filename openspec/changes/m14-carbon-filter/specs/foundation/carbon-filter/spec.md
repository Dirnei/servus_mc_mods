# Spec Delta

## Purpose

The Carbon Filter is Foundation's shared, wearing filter: a charcoal based item any Servus mod can spend cleaning points from, made through the Mill and the vanilla Blast Furnace, which turns into a compostable Spent Filter when used up.

## ADDED Requirements

### Requirement: Charcoal Powder from the Mill
Foundation SHALL provide the item Charcoal Powder and SHALL ship a milling recipe that turns 1 Charcoal into Charcoal Powder (example: 2 Charcoal Powder for 100 work). No other shipped milling recipe SHALL take Charcoal.

#### Scenario: Milling charcoal
- **WHEN** a driven Mill holds 1 Charcoal
- **THEN** it makes Charcoal Powder

#### Scenario: Coal is not charcoal
- **WHEN** a player tries to put Coal into the Mill's input
- **THEN** the slot refuses it, as no milling recipe takes Coal

### Requirement: Activated Charcoal from the Blast Furnace
Foundation SHALL provide the item Activated Charcoal and SHALL ship a vanilla blasting recipe that turns 1 Charcoal Powder into 1 Activated Charcoal. It SHALL ship no furnace or smoker recipe for it.

#### Scenario: Blasting
- **WHEN** a lit Blast Furnace holds Charcoal Powder
- **THEN** it smelts it into Activated Charcoal

#### Scenario: Plain furnace
- **WHEN** a player puts Charcoal Powder into a plain Furnace
- **THEN** it does not smelt

### Requirement: Crafting the Carbon Filter
Foundation SHALL provide the item Carbon Filter and SHALL ship a crafting recipe from Activated Charcoal, one starch binder and Paper (example: 2 Activated Charcoal, 1 binder and 1 Paper give 1 Carbon Filter). A starch binder SHALL be any item in the Foundation item tag `servus_foundation:filter_binders`, which SHALL hold everything in `c:crops/wheat` and, when present, everything in `c:flours`.

#### Scenario: With wheat
- **WHEN** a player crafts 2 Activated Charcoal, 1 Wheat and 1 Paper
- **THEN** the result is 1 Carbon Filter

#### Scenario: With flour
- **WHEN** Servus: Delicacies is installed and a player uses its Flour instead of Wheat
- **THEN** the recipe works the same

### Requirement: Filters are data
Which items are filters SHALL come from the Foundation item data map `servus_foundation:filter`, synced to clients. An entry SHALL give the filter's cleaning points and the item a worn out filter turns into. Foundation SHALL ship one entry: the Carbon Filter, with its points (example 512) and the Spent Filter. A datapack or another mod MAY add entries or change the points.

#### Scenario: Shipped entry
- **WHEN** a world loads with only the Servus mods
- **THEN** the Carbon Filter is a filter with 512 points (example) that turns into a Spent Filter

#### Scenario: Datapack filter
- **WHEN** a datapack adds an entry making Wool a filter with 32 points that turns into String
- **THEN** any filter holder accepts and wears Wool like a Carbon Filter

#### Scenario: Datapack changes the points
- **WHEN** a datapack sets the Carbon Filter's points to 1024
- **THEN** a fresh Carbon Filter lasts twice as long

### Requirement: Wear is shown on the item
A filter stack SHALL remember how many of its points are used. A filter with used points SHALL show a durability bar for the points left and a tooltip line with the points left of all points. A fresh filter SHALL show no bar. Fresh filters SHALL stack (example: up to 16); a filter with used points SHALL NOT stack with fresh ones.

#### Scenario: Partly used
- **WHEN** 200 of a Carbon Filter's 512 points are used
- **THEN** its bar shows about 61% and its tooltip says 312 / 512

#### Scenario: Fresh filters stack
- **WHEN** a player crafts two Carbon Filters
- **THEN** they sit in one slot as a stack of 2 without a bar

### Requirement: Wearing a filter
Foundation SHALL offer an API that wears a filter stack by a number of points and returns how many points were spent. It SHALL spend at most the points the filter has left. When the last point is spent, the stack SHALL become the filter's spent item. The API SHALL spend nothing and return 0 for an empty stack, a stack of an item that is not a filter, or a stack holding more than one filter.

#### Scenario: Normal wear
- **WHEN** a mod wears a fresh Carbon Filter (512 points, example) by 40 points
- **THEN** 40 is returned and the filter shows 472 / 512

#### Scenario: Last points
- **WHEN** a mod wears a Carbon Filter with 30 points left by 40 points
- **THEN** 30 is returned and the stack is now one Spent Filter

#### Scenario: Not a filter
- **WHEN** a mod wears a stack of Paper by 10 points
- **THEN** 0 is returned and the Paper is unchanged

#### Scenario: Stack of several
- **WHEN** a mod wears a stack of 3 fresh Carbon Filters by 10 points
- **THEN** 0 is returned and the stack is unchanged

### Requirement: Spent Filter composts
Foundation SHALL provide the item Spent Filter. It SHALL be compostable in the vanilla Composter with a chance from the NeoForge compostables data map (example 0.65), so used filters end as Bone Meal.

#### Scenario: Composting
- **WHEN** a player puts Spent Filters into a Composter
- **THEN** each raises the fill level with the shipped chance, and a full Composter gives Bone Meal

### Requirement: Items in the creative tab and JEI
Charcoal Powder, Activated Charcoal, Carbon Filter and Spent Filter SHALL appear in the Servus creative tab with English names. Their recipes SHALL show in JEI through the milling category and the vanilla blasting and crafting categories.

#### Scenario: Looking up the filter
- **WHEN** a player looks up the Carbon Filter in JEI
- **THEN** JEI shows its crafting recipe, and looking up Activated Charcoal shows its blasting recipe

### Requirement: Wear logic is pure
Spending points against a filter's points and used points, and deciding when it is worn out, SHALL be plain Java without Minecraft classes and covered by unit tests.

#### Scenario: Unit tested
- **WHEN** the unit tests run
- **THEN** they cover normal wear, wearing past the last point, wearing a worn out filter, zero and negative requests, and the points left for the bar
