# Spec Delta

## ADDED Requirements

### Requirement: Locked recipes cannot be selected
An assembling recipe with a `tech` field SHALL be shown as locked in the recipe panel and SHALL be refused as a selection until the player's team has completed that tech. Recipes without a `tech` field are always available.

#### Scenario: Locked recipe
- **WHEN** a recipe requires a tech the team has not completed and the player clicks it
- **THEN** the selection does not change and the entry stays greyed out

#### Scenario: Unlocked after research
- **WHEN** the team completes the tech
- **THEN** the recipe becomes selectable the next time the panel is drawn
