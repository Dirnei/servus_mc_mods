# Spec Delta

## MODIFIED Requirements

### Requirement: Exact or Any in the recipe editor
The recipe editor SHALL have the shared grid view's mode button, which shows and switches the pattern's mode, Exact or Any. For a grid whose cells take no other items, or where another item makes another item or count, the button SHALL show Exact, be greyed and say in its tooltip why Any is not possible. A new pattern SHALL start as Any when Any is possible, also for a material grid; an existing pattern SHALL open with its own mode. A material grid set to Any SHALL show its variants' results in turn. The Patterns tab SHALL mark Any patterns, and their tooltip SHALL name what each Any cell accepts and, for a material pattern, what the material follows.

#### Scenario: New sticks pattern
- **WHEN** the player opens the editor with plus and puts a plank above a plank
- **THEN** the button shows Any, and pressing it switches to Exact

#### Scenario: Rack by JEI
- **WHEN** the player presses JEI's transfer button on the Heavy Duty Rack recipe
- **THEN** the editor opens with the rack recipe, the button shows Any and the result shows racks of different woods in turn

#### Scenario: Iron block
- **WHEN** the grid holds an iron block recipe
- **THEN** the button shows Exact, is greyed and its tooltip says the cells take no other items

#### Scenario: Marked in the list
- **WHEN** the line has an Any sticks pattern and an Any rack pattern
- **THEN** the Patterns tab marks both as Any, the sticks tooltip says "Any planks" and the rack tooltip also says the wood follows the planks
