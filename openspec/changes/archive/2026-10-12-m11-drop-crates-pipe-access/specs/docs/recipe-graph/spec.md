## MODIFIED Requirements

### Requirement: Ancestor highlight
Selecting an item SHALL highlight that item, every recipe that produces it and, repeating backwards, their inputs and producing recipes down to the leaves. All other nodes and edges SHALL be dimmed. Selecting the item again or pressing Escape SHALL clear the highlight.

#### Scenario: Shuttle
- **WHEN** the user selects the shuttle
- **THEN** the shuttle recipe, the rail and its recipe, iron ingot, redstone and stick are highlighted and the Heavy Duty Rack recipe is dimmed
