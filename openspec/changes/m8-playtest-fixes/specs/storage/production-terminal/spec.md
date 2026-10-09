# Spec Delta

## ADDED Requirements

### Requirement: Content shows when the screen returns
The Production Terminal screen SHALL show its terminal's last known content whenever it is shown, including when the same screen comes back after another screen was in front of it (the recipe viewer, a transfer into the pattern editor). It SHALL NOT fall back to "Loading..." while content for that terminal exists in the session, and fresh content SHALL replace it as it arrives.

#### Scenario: Back from the recipe viewer
- **WHEN** the player opens the recipe viewer from the Production Terminal, transfers a recipe into the pattern editor and saves the pattern
- **THEN** every tab shows content, and the Patterns tab lists the new pattern without reopening the terminal
