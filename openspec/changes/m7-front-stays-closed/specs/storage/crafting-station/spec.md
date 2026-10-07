# Spec Delta

## MODIFIED Requirements

### Requirement: Machine like sides
The station SHALL have a Lib side configuration like a machine: Auto, Input, Output, Both, Pull, Push and Off, all sides Auto by default except the front, which is fixed to Off, shown in the Lib side panel and cycled by the carrot. Through sides that allow insertion the inputs accept only items used by a configured recipe; through sides that allow extraction the outputs can be taken. Items in the outputs SHALL be pushed into adjacent inventories through Auto and Push sides, checking regularly, so an Inbound Station beside the station collects them, and items the recipes use SHALL be pulled from neighbours through Pull sides.

#### Scenario: Dock feeds and collects
- **WHEN** an Outbound Station faces the station with planks in its buffer, an Inbound Station touches the station and the sticks recipe is configured
- **THEN** the planks move into the station and the sticks end up in the Inbound Station's buffer

#### Scenario: Unused item refused
- **WHEN** a hopper offers cobblestone and no configured recipe uses it
- **THEN** the cobblestone is refused

#### Scenario: Side off
- **WHEN** the top side is Off and a hopper above offers planks
- **THEN** the planks stay in the hopper

#### Scenario: Front closed
- **WHEN** an Outbound Station pushes planks against the station's front
- **THEN** the planks stay in the outbound buffer
