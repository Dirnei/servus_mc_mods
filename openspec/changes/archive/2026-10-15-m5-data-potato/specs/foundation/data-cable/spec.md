# Spec Delta

## MODIFIED Requirements

### Requirement: Load on the goggles
While a player is analysing (goggles worn or Data Potato held), data ducts SHALL show their load with a colour from idle to full, based on the share of their bandwidth used in the last second. Holding the Configuration Carrot alone SHALL not show the load.

#### Scenario: Busy duct
- **WHEN** a delivery uses the full bandwidth of a duct and the player wears goggles
- **THEN** that duct shows the full load colour while idle ducts show the idle colour

#### Scenario: Potato shows the load
- **WHEN** a player without goggles holds the Data Potato near the busy duct
- **THEN** that duct shows the full load colour

#### Scenario: Carrot does not
- **WHEN** a player without goggles holds only the Configuration Carrot near the busy duct
- **THEN** no load colour is drawn
