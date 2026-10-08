# Spec Delta

## MODIFIED Requirements

### Requirement: Wrench is obtainable and listed
The item that fills the wrench role is the Configuration Carrot. It SHALL be craftable from vanilla materials and SHALL appear in the Servus creative tab, which exists whenever Servus: Foundation is installed. No item named wrench SHALL exist.

#### Scenario: Crafting
- **WHEN** the player places a carrot, an iron ingot and one redstone dust in the Configuration Carrot pattern (for example a column with redstone on top, the ingot in the middle and the carrot at the bottom)
- **THEN** the crafting result is one Configuration Carrot

#### Scenario: Creative tab
- **WHEN** the player opens the creative inventory
- **THEN** a tab named "Servus" exists with the Configuration Carrot as its icon and lists the Configuration Carrot

#### Scenario: Old wrench id
- **WHEN** a command refers to the item id `servus_foundation:wrench` or `servus_lib:wrench`
- **THEN** no such item exists
