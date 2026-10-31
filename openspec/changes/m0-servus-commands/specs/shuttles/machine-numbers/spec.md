## ADDED Requirements

### Requirement: Warehouses are numbered in the world
Every warehouse SHALL get a number unique among all warehouses of the world, the first one W1, when its controller first becomes a site, and no number SHALL ever be given twice, also after a warehouse is gone. The number SHALL stay with the warehouse's identity: a controller dismantled with the Configuration Carrot and placed again SHALL keep its number, and a controller broken any other way SHALL start a warehouse with a new number. Warehouses of a world saved before numbers existed SHALL get numbers when they load, in a fixed order of their positions.

#### Scenario: Two warehouses
- **WHEN** a player places a controller in a world without warehouses and then a second one elsewhere
- **THEN** they are W1 and W2

#### Scenario: Moved with the carrot
- **WHEN** W2 is dismantled with the carrot and placed at another spot
- **THEN** it is still W2 and the next new warehouse is W3

#### Scenario: Broken controller
- **WHEN** the controller of W2 is broken with a pickaxe and placed again
- **THEN** it becomes W3 and no warehouse is W2 any more
