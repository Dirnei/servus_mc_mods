# Spec Delta

## MODIFIED Requirements

### Requirement: Panels turn see-through while revealing
While a player wears Engineer Goggles or holds the Configuration Carrot or the Data Potato, every panel the player's client has loaded SHALL render half transparent, so the duct behind it shows. When the player stops revealing, the panels SHALL render opaque again. The duct itself SHALL render unchanged.

#### Scenario: Carrot in hand
- **WHEN** a player takes the Configuration Carrot into the hand next to a wall of stone panels over ducts
- **THEN** the panels turn half transparent and the ducts behind them show; putting the carrot away makes the panels opaque again

#### Scenario: Potato in hand
- **WHEN** a player takes the Data Potato into the hand next to the same wall
- **THEN** the panels turn half transparent; putting the potato away makes them opaque again
