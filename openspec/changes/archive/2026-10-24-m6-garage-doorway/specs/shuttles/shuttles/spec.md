## MODIFIED Requirements

### Requirement: Shuttles are drawn moving
Every player who can see a garage SHALL see its shuttles that are out on a job on the rails, each drawn with its own look (see `shuttles/shuttle-looks`), moving smoothly along their paths. A shuttle with an open hold SHALL show its cargo on top; a shuttle with a closed hold SHALL NOT show its cargo. Idle shuttles SHALL stand inside their garage behind the closed shutter and SHALL NOT be drawn on the rail. A leaving shuttle SHALL be drawn inside its garage, facing out, from when the shutter starts to open, and SHALL drive out through the doorway; a shuttle coming home SHALL drive in through the doorway and stay drawn inside, facing in, until the shutter is down, and only then disappear. Clients SHALL learn of a shuttle starting to move or coming home at once, not at the next regular sync. A shuttle SHALL show its cargo only on the legs where it carries something.

#### Scenario: Watching a pick
- **WHEN** a player stands in the aisle while a Mk1 shuttle runs a pick
- **THEN** the shuttle drives past along the rail, with the picked item visible in its open crate on its way to the terminal

#### Scenario: Closed hold
- **WHEN** a Mk2 shuttle carries iron ingots past the player
- **THEN** the shuttle shows its closed iron hold and no ingots

#### Scenario: Idle fleet
- **WHEN** a garage holds three idle shuttles
- **THEN** no shuttle is drawn on its rail, the shutter is down and the garage's right click says three shuttles, none out

#### Scenario: Watching a put-away
- **WHEN** a Mk1 shuttle carries cobblestone from the station to a rack
- **THEN** the shuttle shows cobblestone on that leg and nothing on the way home

#### Scenario: Leaving smoothly
- **WHEN** a player watches the doorway of a garage whose shuttle takes a job
- **THEN** the shutter rolls up, the shuttle rolls out of the garage from standing and speeds up on the rail, with no jump

#### Scenario: Coming home
- **WHEN** a player watches a shuttle come home
- **THEN** it slows down, rolls through the doorway, stops inside facing in, the shutter rolls down, and only then is it gone
