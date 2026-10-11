## MODIFIED Requirements

### Requirement: Shuttles are drawn moving
Every player who can see a garage SHALL see its shuttles that are out on a job on the rails, each drawn with its own look (see `storage/shuttle-looks`), moving smoothly along their paths. A shuttle with an open hold SHALL show its cargo on top; a shuttle with a closed hold SHALL NOT show its cargo. Idle shuttles SHALL sit inside their garage and SHALL NOT be drawn on the rail; a shuttle appears on the garage's home rail when it leaves and disappears into the garage when it is back. A shuttle SHALL show its cargo only on the legs where it carries something.

#### Scenario: Watching a pick
- **WHEN** a player stands in the aisle while a Mk1 shuttle runs a pick
- **THEN** the shuttle drives past along the rail, with the picked item visible in its open crate on its way to the terminal

#### Scenario: Closed hold
- **WHEN** a Mk2 shuttle carries iron ingots past the player
- **THEN** the shuttle shows its closed iron hold and no ingots

#### Scenario: Idle fleet
- **WHEN** a garage holds three idle shuttles
- **THEN** no shuttle is drawn on its rail and the garage's right click says three shuttles, none out

#### Scenario: Watching a put-away
- **WHEN** a Mk1 shuttle carries cobblestone from the station to a crate
- **THEN** the shuttle shows cobblestone on that leg and nothing on the way home
