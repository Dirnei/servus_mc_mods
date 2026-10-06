# Spec Delta

## MODIFIED Requirements

### Requirement: Pull to cursor or inventory
Clicking an entry SHALL pull up to one stack of that item onto the cursor; shift clicking SHALL pull up to one stack into the player's inventory. What does not fit stays in the warehouse. When the warehouse has shuttles with a home rail and the item sits in a block a rail reaches, the pull SHALL instead become a pick job served by a shuttle, and the stack SHALL arrive in the player's inventory when the shuttle returns. Pulls of items that only sit in blocks no rail reaches SHALL stay instant.

#### Scenario: Pull a stack
- **WHEN** the player clicks cobblestone with an empty cursor and the warehouse holds 50 and has no shuttles
- **THEN** 50 cobblestone are on the cursor and the warehouse shows none

#### Scenario: Pull into inventory
- **WHEN** the player shift clicks iron ingots and the warehouse holds 7 and has no shuttles
- **THEN** 7 iron ingots land in the inventory

#### Scenario: Pull served by a shuttle
- **WHEN** the player clicks cobblestone, the warehouse has a shuttle on a home rail and the cobblestone sits in a crate beside a rail
- **THEN** the cursor stays empty, a shuttle fetches the stack and it lands in the player's inventory on return

#### Scenario: Instant despite shuttles
- **WHEN** the player clicks dirt that only sits in a crate touching the controller directly, with no rail beside it
- **THEN** the dirt arrives at once
