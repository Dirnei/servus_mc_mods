# alchemy/receptaculum Specification

## Purpose

The Receptaculum is the heated receiver under the Vas Hermeticum: a Hearth Chest that keeps the homunculus warm, catches what it harvests and holds the gifts and the Elixir it needs. It is the only block through which pipes and the Crafting CPU reach grown ore.

## Requirements

### Requirement: A Hearth Chest that always burns
The Receptaculum SHALL look like a Hearth Chest: a brick hearth with a grated fire and flames at the front, an iron banded deepslate brick chest body, a deepslate tile lid and a brass collar on top. Its fire SHALL burn at all times, working or idle, give off light and send flames and smoke from the grate. It SHALL face the player who places it. It is not hidden: it looks the same with and without Elemental Sight.

#### Scenario: Placed in the dark
- **WHEN** a player places a Receptaculum in a dark room
- **THEN** it faces the player, its fire burns and the blocks beside it are lit

### Requirement: Yield, gift and Elixir slots
The Receptaculum SHALL hold 27 yield slots, 3 gift slots and 1 Elixir slot. Players SHALL only take from the yield slots. The gift slots SHALL take any item but the Elixir of Sight. The Elixir slot SHALL take only the Elixir of Sight. Its screen SHALL show the yield slots, the gift slots and the Elixir slot with their names, the player inventory and the Foundation side panel.

#### Scenario: Elixir slot
- **WHEN** a player puts a cookie into the Elixir slot
- **THEN** it is refused, and an Elixir of Sight goes in

#### Scenario: Yield is take only
- **WHEN** a player tries to put Raw Iron into a yield slot
- **THEN** it is refused

### Requirement: Pipes reach the yield
The Receptaculum SHALL expose the standard item capability with Foundation's side configuration (every side Auto, the front locked off): automation SHALL extract only from the yield slots and SHALL insert only into the gift slots and the Elixir slot, by the same rules as the screen. On sides set to push it SHALL push its yield into neighbours like every Foundation machine. Breaking it SHALL drop the Receptaculum and everything it holds.

#### Scenario: A pipe takes the ore
- **WHEN** the yield slots hold 5 Raw Iron and an Item Pipe pulls from the Receptaculum's side
- **THEN** the pipe receives the 5 Raw Iron

#### Scenario: A hopper feeds the Elixir
- **WHEN** a hopper above holds an Elixir of Sight and a cookie
- **THEN** the Elixir goes into the Elixir slot and the cookie into a gift slot

### Requirement: The Receptaculum is a producer
The Receptaculum SHALL expose the Foundation production capability. While a grown homunculus lives in the Vas on top, it SHALL list one recipe per distinct vein of the bound, ripe Gnome Hollows in reach: no inputs, the vein's yield as output (twice the count once the homunculus is freed), the vein's yield time as the recipe time. Without a grown homunculus the list SHALL be empty. A recipe SHALL be reported as active while the homunculus works (it has Elixir or is freed) and its Hollow is in reach. It SHALL keep no upkeep.

#### Scenario: An iron vein in reach
- **WHEN** a grown homunculus sits on a Receptaculum and a bound, ripe iron Hollow lies two blocks east of it on the same level
- **THEN** the production capability lists one recipe with no inputs and Raw Iron as output

#### Scenario: No homunculus
- **WHEN** the Receptaculum has no Vas on top, or the homunculus in it is still growing
- **THEN** the production capability lists no recipe

### Requirement: Making the Receptaculum
A shaped crafting recipe SHALL make one Receptaculum from a Chest, Ignis Perpetuus, Deepslate Bricks and Bricks. The journal's Workshop SHALL explain it in an entry "The Receptaculum" that opens with the Basel and Salzburg chapter (Rubedo), with the crafting recipe and how the slots are used. It SHALL NOT mention the Nether Star.

#### Scenario: Crafting
- **WHEN** a player lays Deepslate Bricks, a Chest, Ignis Perpetuus and Bricks in the recipe's pattern
- **THEN** the crafting table makes one Receptaculum
