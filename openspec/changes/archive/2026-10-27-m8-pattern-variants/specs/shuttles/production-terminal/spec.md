## ADDED Requirements

### Requirement: Exact or Any in the recipe editor
The recipe editor SHALL have a button beside Save that shows and switches the pattern's mode, Exact or Any. For a grid whose result changes with the items the recipe accepts the button SHALL show Exact, be greyed and say in its tooltip why Any is not possible. A new pattern SHALL start as Any when Any is possible; an existing pattern SHALL open with its own mode. The Patterns tab SHALL mark Any patterns, and their tooltip SHALL name what each Any cell accepts.

#### Scenario: New sticks pattern
- **WHEN** the player opens the editor with plus and puts a plank above a plank
- **THEN** the button shows Any, and pressing it switches to Exact

#### Scenario: Rack
- **WHEN** the grid holds a Heavy Duty Rack recipe
- **THEN** the button shows Exact, is greyed and its tooltip says the result changes with the planks

#### Scenario: Marked in the list
- **WHEN** the line has an Any sticks pattern and an Exact rack pattern
- **THEN** the Patterns tab marks the sticks pattern as Any and its tooltip says "Any planks"
