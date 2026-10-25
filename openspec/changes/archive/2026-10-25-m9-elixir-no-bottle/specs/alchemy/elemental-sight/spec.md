# Spec Delta

## MODIFIED Requirements

### Requirement: Elixir of Sight
The Elixir of Sight SHALL be drunk like a potion, over a short time, SHALL give the drinker Elemental Sight for its duration (example 3,600 ticks), and SHALL be used up entirely, leaving no glass bottle. It SHALL stack to 16. Every drink SHALL also roll a poison chance (see "The dose makes the poison"); Elemental Sight SHALL be granted whether the roll poisons or not.

#### Scenario: Drink
- **WHEN** a player finishes drinking an Elixir of Sight and the roll does not poison
- **THEN** they have Elemental Sight for three minutes and the elixir is gone from their hand, with no glass bottle in its place
