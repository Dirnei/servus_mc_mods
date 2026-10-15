# Spec Delta

## MODIFIED Requirements

### Requirement: Yield, gift and Elixir slots
The Receptaculum SHALL hold 27 yield slots, 3 gift slots and 1 Elixir slot. Players SHALL only take from the yield slots. By hand the gift slots SHALL take any item but the Elixir of Sight. The Elixir slot SHALL take only the Elixir of Sight. Its screen SHALL show the yield slots, the gift slots and the Elixir slot with their names, the player inventory and the Foundation side panel.

#### Scenario: Elixir slot
- **WHEN** a player puts a cookie into the Elixir slot
- **THEN** it is refused, and an Elixir of Sight goes in

#### Scenario: Yield is take only
- **WHEN** a player tries to put Raw Iron into a yield slot
- **THEN** it is refused

### Requirement: Pipes reach the yield
The Receptaculum SHALL expose the standard item capability with Foundation's side configuration (every side Auto, the front fixed to Off, so nothing connects there): automation SHALL extract only from the yield slots and SHALL insert only into the gift slots and the Elixir slot. Into the gift slots automation SHALL insert only what gnomes like by the gnome gift data; into the Elixir slot only the Elixir of Sight. On sides set to push it SHALL push its yield into neighbours like every Foundation machine. Breaking it SHALL drop the Receptaculum and everything it holds.

#### Scenario: A pipe takes the ore
- **WHEN** the yield slots hold 5 Raw Iron and an Item Pipe on the Receptaculum's side leads to a chest
- **THEN** the chest receives the 5 Raw Iron

#### Scenario: A hopper feeds the Elixir
- **WHEN** a hopper above holds an Elixir of Sight and a cookie
- **THEN** the Elixir goes into the Elixir slot and the cookie into a gift slot

#### Scenario: No junk in the gift slots
- **WHEN** a hopper above holds Cobblestone and a cookie
- **THEN** the cookie goes into a gift slot and the Cobblestone stays in the hopper

#### Scenario: No pipe at the front
- **WHEN** an Item Pipe is placed against the Receptaculum's front
- **THEN** the pipe draws no arm to it
