## ADDED Requirements

### Requirement: Shown as the Incinerator
Every text a player sees about this block SHALL call it the Incinerator: its item and block name, its GUI title, its forming, casing and chimney messages and the guide book. Its registry id SHALL stay `servus_foundation:sink`, so blocks placed in earlier versions stay in place as Incinerators.

#### Scenario: Name in the inventory
- **WHEN** a player hovers the block in the creative tab
- **THEN** the tooltip says "Incinerator"

#### Scenario: Old world
- **WHEN** a world built with a Sink in an earlier version is loaded
- **THEN** the block is still there, formed as before, and is named Incinerator

#### Scenario: Forming message
- **WHEN** a player clicks an incomplete cube with the Configuration Carrot
- **THEN** the message asks for a full 2x2x2 cube of Incinerator blocks
