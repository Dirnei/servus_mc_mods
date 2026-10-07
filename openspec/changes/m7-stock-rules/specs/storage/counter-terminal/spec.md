# Spec Delta

## ADDED Requirements

### Requirement: Stock rules from the terminal
The amount prompt SHALL have a Rule mode with minimum and maximum fields, Save and Remove, that sets or deletes the stock rule of the clicked item. Items with a rule SHALL show a marker in the stock grid and the rule with its state in their tooltip.

#### Scenario: Save a rule
- **WHEN** the player opens the prompt for sticks, switches to Rule, enters 8 and 16 and saves
- **THEN** the sticks entry shows a marker and the tooltip reads "keep 8 to 16"

#### Scenario: Remove
- **WHEN** the player opens the prompt for sticks in Rule mode and presses Remove
- **THEN** the marker and tooltip line are gone
