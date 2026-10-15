# Spec Delta

## MODIFIED Requirements

### Requirement: Channel ring at endpoints
An arm that touches an inventory or machine SHALL carry a thin ring at its outer end in the colour of the channel of that line and side; the data line has no channels and its ring shows the colour of its tier. With several lines on the arm each line's band carries its own ring. Arms toward other ducts SHALL have no ring. When a channel's colour is edited, every loaded ring on that channel SHALL change colour.

#### Scenario: Channel 3 endpoint
- **WHEN** the item line's side toward a chest is on the red channel "Iron ore"
- **THEN** the arm toward the chest ends in a red ring

#### Scenario: Ring follows the change
- **WHEN** the player switches that side to the green channel "Copper" in the GUI
- **THEN** the ring turns green without replacing the block

#### Scenario: Fluid ring
- **WHEN** the fluid line's side toward a tank is on the blue channel "Water"
- **THEN** the fluid arm toward the tank ends in a blue ring

#### Scenario: Ring follows a recolour
- **WHEN** a team member changes "Water" from blue to black
- **THEN** every loaded ring on "Water" turns black and stays visible against the duct

#### Scenario: Data ring
- **WHEN** a Data Cable duct touches a Warehouse Controller
- **THEN** the data arm toward it ends in a white ring
