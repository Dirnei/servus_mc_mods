## ADDED Requirements

### Requirement: Anchor Upgrade slots
The controller SHALL have `anchor_upgrade_slots` upgrade slots (example 4, `warehouse.json`) that take only the Foundation Anchor Upgrade, one per slot. The site's capacity SHALL be the number of upgrades times `chunks_per_upgrade` (example 9, `warehouse.json`). Upgrades SHALL drop when the controller is broken or dismantled. Hoppers and ducts SHALL NOT reach the slots.

#### Scenario: Two upgrades
- **WHEN** a controller holds two Anchor Upgrades with 9 chunks per upgrade
- **THEN** its capacity is 18 chunks

#### Scenario: Wrong item
- **WHEN** the player tries to put a Speed Upgrade into a controller slot
- **THEN** the slot refuses it

## MODIFIED Requirements

### Requirement: Over capacity is an error
A controller with at least one Anchor Upgrade whose site spans more chunks than its capacity SHALL be in an error state: its GUI SHALL say the site is over capacity in red and its activity lamp SHALL be red. A controller in the no power state SHALL show the red lamp as well, and its GUI SHALL say which of the two applies. Otherwise the lamp SHALL be green while online. A controller without upgrades SHALL show no capacity error.

#### Scenario: Over capacity
- **WHEN** a controller with one upgrade of 9 chunks spans 10 chunks
- **THEN** its lamp is red and the GUI says 10 of 9 chunks, over capacity

#### Scenario: Back within capacity
- **WHEN** a second upgrade goes into that controller
- **THEN** the lamp turns green and the GUI says 10 of 18 chunks

#### Scenario: No upgrade
- **WHEN** a controller without upgrades and with FE spans 3 chunks
- **THEN** its lamp is green

#### Scenario: No power
- **WHEN** a controller within capacity holds no FE
- **THEN** its lamp is red and the GUI says the warehouse has no power

### Requirement: Controller GUI
Right clicking a controller with an empty hand SHALL open its GUI with the site name as title, a name field, the online state with the warehouse summary as its tooltip, the chunk count and capacity, a chunk map, the Anchor Upgrade slots, the Wireless Upgrade slot, the FE stored with the capacity, the "Other sites" panel and the player inventory. The chunk map SHALL draw every chunk of the span as a square around the controller's chunk, mark the controller's chunk, and draw in red while over capacity. The GUI SHALL refresh while open and SHALL close when the player leaves reach.

#### Scenario: Map
- **WHEN** a site spans its own chunk and the chunk to the east
- **THEN** the map shows two squares side by side with the controller's chunk marked

#### Scenario: Energy shown
- **WHEN** a controller holds 12,000 of 100,000 FE and a player opens its GUI
- **THEN** the GUI shows 12,000 of 100,000 FE and the value follows while FE arrives

### Requirement: Wireless Upgrade slot
The controller SHALL have one slot that takes only the Wireless Upgrade, apart from its Anchor Upgrade slots. A second Wireless Upgrade SHALL add nothing and does not fit. The upgrade SHALL drop when the controller is broken or dismantled. Hoppers and ducts SHALL NOT reach the slot.

#### Scenario: Insert the upgrade
- **WHEN** the player puts a Wireless Upgrade into the controller's wireless slot
- **THEN** the slot takes it and the controller reports that it holds a Wireless Upgrade

#### Scenario: Wrong item
- **WHEN** the player tries to put a Anchor Upgrade or a Speed Upgrade into the wireless slot
- **THEN** the slot refuses it

#### Scenario: Dismantled
- **WHEN** a controller holding a Wireless Upgrade is dismantled with the Configuration Carrot
- **THEN** the Wireless Upgrade drops beside the controller item

## REMOVED Requirements

### Requirement: Chunk Loader Upgrade slots
**Reason**: The upgrade is renamed Anchor Upgrade and moves to Foundation.
**Migration**: Replaced by "Anchor Upgrade slots" with the same behavior.

### Requirement: Chunk Loader Upgrade is obtainable
**Reason**: The upgrade is no longer a Storage item.
**Migration**: The Anchor Upgrade and its recipe are specified in `foundation/ender-anchor`.
