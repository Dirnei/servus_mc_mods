## MODIFIED Requirements

### Requirement: Ripe share bar
The front of the Field Head SHALL show a ripe share bar in its lower panel: a small dark window that fills from the left with wheat gold in a few steps as the ripe share of the planted plots approaches the head's harvest threshold, and that is full and green while a harvest is due. The ripe share shown SHALL be empty when nothing is ripe or the head serves no field. The head SHALL change the ripe share shown only when its ripe count, planted count, crop, harvest only mode or threshold changes, never by looking at the field per tick. While the head has a problem, the same problems that put a red marker on a tab of its GUI (it serves no field, its work status is a warning such as no controller, no crop, no Harvester shuttle or no way to the field, the field is dry or corroded, or the fertilizing is held up), the bar SHALL be full and red instead, so the player sees it without opening the GUI; once the problem is gone the bar SHALL show the ripe share again. The head SHALL notice problems that come from the warehouse (controller, shuttles, the way to the field) within about a second, by asking its warehouse now and then, not by looking at the field.

#### Scenario: Bar follows the ripe share
- **WHEN** a head without a problem, with wheat set and a threshold of 50 percent, sees one of four planted plots ripe
- **THEN** its bar shows wheat gold, part way filled, and not green

#### Scenario: Bar turns green when due
- **WHEN** the ripe share reaches the head's threshold and a harvest is due
- **THEN** the bar is full and green

#### Scenario: Lowering the threshold
- **WHEN** the threshold is lowered in the GUI to the current ripe share
- **THEN** the bar turns green without any crop changing

#### Scenario: Problem turns the bar red
- **WHEN** a head's field has water, wheat set and a Harvester shuttle in a garage of its warehouse, and the player then clears the crop
- **THEN** the bar on the head turns full and red, and the Field tab of its GUI has a red marker

#### Scenario: Unreachable field
- **WHEN** the only Harvester shuttle cannot reach the head's field without riding a lift car
- **THEN** the bar on the head is red
