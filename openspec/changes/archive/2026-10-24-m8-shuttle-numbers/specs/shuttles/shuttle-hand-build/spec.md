## MODIFIED Requirements

### Requirement: The plate shows
A shuttle with a name plate SHALL carry the name in its item name (so tooltips, the garage GUI and every inventory show it): a numbered shuttle by its label, as "Berta (#12)", a shuttle without a number as "Berta (Shuttle Mk2)". The Field Head SHALL name the shuttle while that shuttle works its field. An anvil name SHALL NOT show on the shuttle in the world.

#### Scenario: No text on the model
- **WHEN** a Mk2 shuttle with the plate "Berta" drives along a rail
- **THEN** no text is drawn on the shuttle

#### Scenario: Plate in the garage
- **WHEN** the player hovers over shuttle 12 with the plate "Berta" in a garage slot
- **THEN** the tooltip names it "Berta (#12)"

#### Scenario: Field Head names the worker
- **WHEN** the harvester with the plate "Berta" works a field and a player opens the field's head
- **THEN** the head says that Berta is working the field

#### Scenario: Anvil name only on the item
- **WHEN** a Mk1 shuttle renamed at an anvil drives along a rail
- **THEN** no name is drawn on it
