# Spec Delta

## MODIFIED Requirements

### Requirement: Chest sized inventory with a GUI
A Heavy Duty Rack SHALL hold any items in a fixed number of slots (`slots` in `data/<ns>/storage/rack.json`, example 27). Right clicking its front face with an empty hand or a non tool item SHALL open a chest style GUI with the rack slots above the player inventory, with shift click moving stacks between them. Clicking any other face SHALL behave like clicking a plain block, so a held block is placed against it.

#### Scenario: Open and store
- **WHEN** a player right clicks the front of a rack and shift clicks a stack of planks from the inventory
- **THEN** the planks are in a rack slot and the GUI shows them

#### Scenario: Full rack
- **WHEN** all slots hold items and a player shift clicks another stack
- **THEN** the stack stays in the player's inventory

#### Scenario: Rack placed against a rack
- **WHEN** a player holding a rack right clicks the side of a rack
- **THEN** a new rack is placed beside it and no GUI opens

#### Scenario: Front with a rack in hand
- **WHEN** a player holding a rack right clicks the front of a rack
- **THEN** the GUI opens and nothing is placed
