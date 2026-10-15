# Spec Delta

## MODIFIED Requirements

### Requirement: Batched polling
The network SHALL poll at most `sources_per_tick` sources per tick (default 4, from `data/<ns>/shuttles/warehouse.json`), round robin, and SHALL refresh a storage node's snapshot on the tick after it reports a change.

#### Scenario: Large warehouse
- **WHEN** a network has 40 sources
- **THEN** every source is refreshed at least once per 10 ticks and no tick reads more than 4

### Requirement: Controller FE buffer
The controller SHALL have an FE buffer of `controller_energy_capacity` (example 100,000, `shuttles/power.json`) that takes FE from every side through the standard energy capability, for example from the Utility Duct power line, and never gives FE out. The FE stored SHALL be saved with the controller and SHALL NOT travel in a dismantled item. The controller GUI SHALL show the FE stored and the capacity.

#### Scenario: Power line
- **WHEN** a Utility Duct with the power line fed by a Coal Generator touches the controller
- **THEN** the duct shows a power arm toward the controller and its FE stored rises

#### Scenario: No FE out
- **WHEN** a machine next to the controller tries to pull FE from it
- **THEN** it gets none

### Requirement: Wireless cost per item and distance
Each item a transfer moves SHALL cost `wireless_fe_per_item` plus `wireless_fe_per_item_per_block` times the straight distance in blocks between the two controllers (examples 4 and 0.02, `shuttles/power.json`), paid from the receiving site's controller buffer when the item moves. The sending site SHALL pay nothing for the wireless hop. Without enough FE at the receiver nothing SHALL move and the transfer SHALL wait.

#### Scenario: Cost
- **WHEN** 10 items move between two sites whose controllers are 500 blocks apart, with 4 FE per item and 0.02 FE per item and block
- **THEN** the receiving controller's buffer loses 140 FE and the sending controller's buffer is unchanged

#### Scenario: Receiver runs dry
- **WHEN** the receiving controller holds FE for 3 items of a transfer of 10
- **THEN** 3 items move and the rest waits until FE arrives

### Requirement: Controller exposes stock
A Warehouse Controller SHALL expose the Lib stock provider capability on every side for its online warehouse: counts SHALL come from the index, and taking from the provider SHALL give nothing, because items leave a warehouse only by shuttle (other mods and the data line ask the supplier capability, see `shuttles/packing-station`). The controller SHALL NOT expose a stock sink. An offline controller SHALL report no stock.

#### Scenario: Another mod counts stock
- **WHEN** a mod asks the controller's stock provider for cobblestone while racks hold 80
- **THEN** it gets 80

#### Scenario: Taking gives nothing
- **WHEN** a mod asks the controller's stock provider to take 20 cobblestone while racks hold 80
- **THEN** it gets nothing and the racks still hold 80

#### Scenario: Another mod hands items in
- **WHEN** a mod looks for a stock sink on the controller to hand in 20 bread
- **THEN** there is none and the bread stays with the mod

#### Scenario: Offline
- **WHEN** the network has two controllers
- **THEN** the stock provider reports zero

### Requirement: Wireless Upgrade is obtainable
The Wireless Upgrade SHALL be a Shuttles item crafted at a crafting table from an ender eye, iron ingots, redstone and a copper ingot (example), with a tooltip saying it lets the warehouse trade with other sites over a wireless line for FE. It SHALL NOT be a Foundation multiplier upgrade.

#### Scenario: Recipe
- **WHEN** the player crafts the pattern with an ender eye in the middle
- **THEN** one Wireless Upgrade is produced
