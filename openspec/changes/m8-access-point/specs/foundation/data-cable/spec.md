# Spec Delta

## MODIFIED Requirements

### Requirement: Members by capability
A block SHALL be a member of a data network on a side that touches a duct's data line when it exposes, on that side, the production capability (role producer), the stock provider or stock sink capability (role stock), or the data link capability `servus_foundation:data_link` (role link). A block with several kinds SHALL have all their roles. The duct SHALL show a data arm toward a member. Blocks without these capabilities, such as chests, hoppers and rails, SHALL NOT be members and the duct SHALL show no data arm toward them.

#### Scenario: Crafting Station
- **WHEN** a duct with the data line touches a Crafting Station
- **THEN** the duct shows a data arm toward it and the station is a producer of that network

#### Scenario: Warehouse Controller
- **WHEN** a duct with the data line touches a Warehouse Controller
- **THEN** the controller is a stock member of that network

#### Scenario: Link member
- **WHEN** a duct with the data line touches an Access Point
- **THEN** the duct shows a data arm toward it and the Access Point is a link member of that network

#### Scenario: Chest
- **WHEN** a duct carrying only the data line touches a chest
- **THEN** the duct shows no arm toward it and the network has no member there
