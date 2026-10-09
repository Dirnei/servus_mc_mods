# Spec Delta

## ADDED Requirements

### Requirement: Calciner reports to the journal
The Calciner SHALL remember the player who placed it as its owner and SHALL report every finished cycle to the journal of the owner's team: one `calcine` event for the item, and one `principle` event per principle with the mB it made.

#### Scenario: Cobblestone counts
- **WHEN** a player's Calciner calcines one cobblestone
- **THEN** the player's team gets a `calcine` event for cobblestone and `principle` events of 100 Salt and 10 Mercury
