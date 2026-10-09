# Spec Delta

## ADDED Requirements

### Requirement: Drinking the elixir counts
Drinking an Elixir of Sight SHALL report a `sight` event to the journal of the drinker's team. Schwaz SHALL ask for it once.

#### Scenario: First sight
- **WHEN** a player whose team has Schwaz open drinks an Elixir of Sight
- **THEN** Schwaz's task "drink the Elixir of Sight" is done
