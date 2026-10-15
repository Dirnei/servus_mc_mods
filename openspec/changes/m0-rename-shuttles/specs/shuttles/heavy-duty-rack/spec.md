# Spec Delta

## MODIFIED Requirements

### Requirement: Chest sized inventory with a GUI
A Heavy Duty Rack bay SHALL hold any items in a fixed number of slots (`slots` in `data/<ns>/shuttles/rack.json`, example 27). Right clicking the front face of any bay with an empty hand or a non tool item SHALL open a chest style GUI for the whole section the bay belongs to: the slots of every bay of the section, bay after bay from left to right as seen from the front, all shown at once above the player inventory, with shift click moving stacks between them. The GUI SHALL grow with the section (example: 9 rows for a section of three bays of 27 slots) and SHALL NOT scroll. Each bay SHALL store its own part of the slots. The GUI SHALL close when a bay of its section is removed or the section changes. Clicking any other face SHALL behave like clicking a plain block, so a held block is placed against it.

#### Scenario: Open and store
- **WHEN** a player right clicks the front of a single rack and shift clicks a stack of planks from the inventory
- **THEN** the planks are in a rack slot and the GUI shows them

#### Scenario: Section GUI
- **WHEN** a player right clicks the front of the middle bay of a section of three
- **THEN** the GUI shows 81 slots in 9 rows: the left bay's slots first, then the middle bay's, then the right bay's

#### Scenario: Shift click fills the section
- **WHEN** the left bay of a section of two is full and a player shift clicks a stack into the section GUI
- **THEN** the stack lands in the right bay's slots

#### Scenario: Full rack
- **WHEN** all slots of the section hold items and a player shift clicks another stack
- **THEN** the stack stays in the player's inventory

#### Scenario: Bay broken from a section
- **WHEN** the right bay of a section of two that holds items in both bays is broken
- **THEN** only the right bay's items spill and the left bay keeps its items

#### Scenario: Section changes while open
- **WHEN** a player has the GUI of a section open and another player breaks one of its bays
- **THEN** the GUI closes

#### Scenario: Rack placed against a rack
- **WHEN** a player holding a rack right clicks the side of a rack
- **THEN** a new rack is placed beside it and no GUI opens

#### Scenario: Front with a rack in hand
- **WHEN** a player holding a rack right clicks the front of a rack
- **THEN** the GUI opens and nothing is placed
