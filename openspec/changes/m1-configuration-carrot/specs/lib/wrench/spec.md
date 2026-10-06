# Spec Delta

## MODIFIED Requirements

### Requirement: Wrench is obtainable and listed
The item that fills the wrench role is the Configuration Carrot. It SHALL be craftable from vanilla materials and SHALL appear in the Servus creative tab, which exists whenever Servus Lib is installed. No item named wrench SHALL exist.

#### Scenario: Crafting
- **WHEN** the player places a carrot, an iron ingot and one redstone dust in the Configuration Carrot pattern (for example a column with redstone on top, the ingot in the middle and the carrot at the bottom)
- **THEN** the crafting result is one Configuration Carrot

#### Scenario: Creative tab
- **WHEN** the player opens the creative inventory
- **THEN** a tab named "Servus" exists with the Configuration Carrot as its icon and lists the Configuration Carrot

#### Scenario: Old wrench id
- **WHEN** a command refers to the item id `servus_lib:wrench`
- **THEN** no such item exists

## ADDED Requirements

### Requirement: Configuration Carrot appearance
The Configuration Carrot SHALL render as a carrot without its green leaves. Where the leaves would be, a short grey metal bar SHALL stick out of the carrot, ending in a red tip. Its display name SHALL be "Configuration Carrot" and it SHALL not stack.

#### Scenario: Item icon
- **WHEN** the player looks at the Configuration Carrot in an inventory slot
- **THEN** an orange carrot body is shown with a grey bar and a red tip on top and no green anywhere in the icon

#### Scenario: Stack size
- **WHEN** the player picks up two Configuration Carrots
- **THEN** they occupy two inventory slots

#### Scenario: Not food
- **WHEN** the player holds right click with the Configuration Carrot while looking at air
- **THEN** nothing is eaten and the item stays in the hand
