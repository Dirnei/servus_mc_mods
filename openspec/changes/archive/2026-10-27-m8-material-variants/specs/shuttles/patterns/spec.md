# Spec Delta

## ADDED Requirements

### Requirement: When a pattern can be Any
A pattern SHALL be allowed to be Any when some used cell accepts other items and every accepted item, put in place of the pattern's item in all cells holding it, makes a result of the same item and count. When the result's components change with the items (the rack's wood, the Elevator's material) the pattern SHALL be a material pattern. Saving Any where this does not hold SHALL save Exact.

#### Scenario: Chest from any planks
- **WHEN** the player saves a chest pattern from eight oak planks as Any
- **THEN** the pattern is Any and not a material pattern, since a chest from any planks is the same chest

#### Scenario: Rack follows the planks
- **WHEN** the player saves a Heavy Duty Rack pattern with oak planks as Any
- **THEN** the pattern is an Any material pattern

#### Scenario: Iron block stays Exact
- **WHEN** the player saves an iron block pattern as Any
- **THEN** the pattern is Exact, since its cells take no other items

### Requirement: Material patterns make every variant
A material pattern SHALL let the CPU make each variant its accepted items give: one item per ingredient, used in every cell of that ingredient, and the result the recipe makes from them. A request, stock rule or recipe step that needs one variant (a spruce rack) SHALL use that variant's items and SHALL craft them first when they are not in stock.

#### Scenario: Spruce rack from an oak pattern
- **WHEN** the line has an Any rack pattern saved with oak planks, the warehouse holds iron and 20 spruce planks, and a request asks for 3 spruce Heavy Duty Racks
- **THEN** the step takes 3 spruce planks and the iron, and 3 spruce racks are made

#### Scenario: Planks crafted for the variant
- **WHEN** the same line has an Any spruce planks pattern from logs, the warehouse holds spruce logs but no spruce planks, and a request asks for 3 spruce racks
- **THEN** the order crafts spruce planks from spruce logs first and then the spruce racks

### Requirement: Generic orders take the most stocked variant
An order for the item of a material pattern without its material (a Heavy Duty Rack without a wood) SHALL make the variant whose ingredients the warehouse holds most of after earlier reservations, ties going to the pattern's own items, none in stock to the pattern's own items. The order and its request SHALL then name that variant.

#### Scenario: Most stocked wood
- **WHEN** the line has an Any rack pattern saved with oak planks, the warehouse holds 5 oak planks, 40 birch planks and iron, and the player crafts 2 Heavy Duty Racks from the generic entry
- **THEN** 2 birch racks are made from birch planks and the request lists birch Heavy Duty Racks

#### Scenario: Nothing stocked
- **WHEN** the warehouse holds no planks but oak logs, and an oak planks pattern is on the line
- **THEN** the generic order crafts oak planks and then oak racks

### Requirement: Cells with a material accept stocked and made variants
A cell whose item carries a material (an Elevator's Rail) SHALL accept, in a material pattern, every stack of that item the warehouse holds and every one a recipe of the line makes, each with its own material.

#### Scenario: Spruce Elevator uses a spruce Rail
- **WHEN** the line has an Any Elevator pattern saved with an oak Rail, the warehouse holds a spruce Rail and the rest, and a request asks for a spruce Elevator
- **THEN** the step takes the spruce Rail and a spruce Elevator is made

#### Scenario: Rail crafted for the Elevator
- **WHEN** no spruce Rail is in stock and an Any Rail pattern and spruce planks are on the line
- **THEN** the order crafts a spruce Rail first and then the spruce Elevator

### Requirement: Exact patterns stay fixed below
An Exact pattern SHALL need exactly its own items; a material pattern below it SHALL make exactly the variant the Exact pattern needs.

#### Scenario: Exact oak Elevator
- **WHEN** the line has an Exact Elevator pattern with an oak Rail, an Any Rail pattern saved with spruce planks, oak planks in stock and no Rail, and a request asks for an oak Elevator
- **THEN** the order crafts an oak Rail from oak planks and then the oak Elevator

## MODIFIED Requirements

### Requirement: One Any pattern per recipe and result
Saving an Any pattern SHALL be refused like a duplicate when the line already has an Any pattern for the same recipe whose result is the same item in the same count, whatever items either was saved with and whatever material their results carry. An Exact pattern SHALL NOT count as a duplicate of an Any pattern unless it has the same recipe and the same items.

#### Scenario: Second Any sticks
- **WHEN** the line has an Any sticks pattern saved with oak planks and the player saves an Any sticks pattern with birch planks
- **THEN** nothing is stored and the action bar says the line already has this pattern

#### Scenario: Second Any rack
- **WHEN** the line has an Any rack pattern saved with oak planks and the player saves an Any rack pattern with spruce planks
- **THEN** nothing is stored and the action bar says the line already has this pattern

#### Scenario: Exact rack beside an Any rack
- **WHEN** the line has an Any rack pattern saved with oak planks and the player saves an Exact rack pattern with spruce planks
- **THEN** the Exact pattern is stored

## REMOVED Requirements

### Requirement: Any only where the result stays the same
**Reason**: Replaced by "When a pattern can be Any": a result whose components follow the inputs no longer forces Exact.
**Migration**: None. Patterns saved Exact stay Exact; the player can switch a rack or Elevator pattern to Any in the editor.
