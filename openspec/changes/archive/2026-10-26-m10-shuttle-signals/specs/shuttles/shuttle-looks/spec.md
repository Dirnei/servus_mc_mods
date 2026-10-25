# Spec Delta

## MODIFIED Requirements

### Requirement: The item looks like the shuttle
A shuttle item SHALL show the same composed look as the shuttle on the rails, in inventories, the garage GUI, the Shuttle Workshop and when held or dropped, except for the lights and the number plate, which only a shuttle on the rails or on a lift car shows. Installing or removing an upgrade or a tool, or a tier build, SHALL change the item's look at once.

#### Scenario: Installing a tool
- **WHEN** a player installs a Harvester into a Mk1 shuttle in the workshop
- **THEN** the shuttle in the workshop slot shows the reel on its front

#### Scenario: Numbered shuttle in a slot
- **WHEN** a player looks at Shuttle #12 in a garage slot
- **THEN** the slot shows the shuttle without lights or number plate, and the tooltip names it "Shuttle #12"
