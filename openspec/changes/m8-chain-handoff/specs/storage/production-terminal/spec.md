# Spec Delta

## ADDED Requirements

### Requirement: Crafting tab names handoffs
While the first unfinished step of a job sends outputs straight to the next step's producer, its row SHALL say so in plain words with the step's machine, the item and the receiving machine.

#### Scenario: Stone to the station
- **WHEN** a furnace adapter step sends stone to a Crafting Station
- **THEN** the job row reads "Furnace (adapter): sending Stone to Crafting Station"
