# Spec Delta

## ADDED Requirements

### Requirement: Data carriers
Servus: Foundation SHALL let a block carry the data network to touching blocks like a piece of cable. A carrier SHALL link to every touching carrier and to every touching duct whose data side toward it is on, so touching carriers and ducts form one data network that merges and splits on placement and removal without scanning blocks every tick. A carrier SHALL be a member of its own network with the roles of its own capabilities and SHALL NOT make its other neighbours members. Carriers SHALL NOT count as cables in the summary. Packets SHALL route through carriers like through ducts.

#### Scenario: Two carriers touch
- **WHEN** two carriers that expose the data link capability touch and no duct is near
- **THEN** both report the same data network and both are link members of it

#### Scenario: Carrier beside a duct
- **WHEN** a carrier touches a duct whose data side toward it is on
- **THEN** the carrier and the duct share one network and the summary counts one cable

#### Scenario: Side off
- **WHEN** the duct's data side toward the carrier is off
- **THEN** the carrier is not part of the duct's network

#### Scenario: Neighbour of a carrier
- **WHEN** a block exposing the stock capability touches only a carrier
- **THEN** it is not a member of the carrier's network
