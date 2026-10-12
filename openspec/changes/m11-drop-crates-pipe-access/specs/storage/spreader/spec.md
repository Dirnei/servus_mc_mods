## MODIFIED Requirements

### Requirement: Spreader module
The Spreader SHALL be a craftable tool module. It SHALL go into a shuttle through the install slot of the Shuttle Workshop, into the shuttle's one tool place, replacing a fitted tool. A shuttle with a Spreader SHALL be field only and SHALL take only fertilize runs: no warehouse picks, no put-aways and no harvest runs. A shuttle without a Spreader SHALL NOT take fertilize runs. The Spreader SHALL be wide, never riding a lift car, and its tooltip SHALL say what it does and that it does not fit into lifts.

#### Scenario: Fit a Spreader
- **WHEN** a player puts a Mk1 shuttle into the workshop and a Spreader into the install slot
- **THEN** the shuttle has the Spreader as its tool

#### Scenario: Spreaders stay out of the warehouse
- **WHEN** a warehouse has only a Spreader shuttle and a player requests cobblestone from a rack on the rails
- **THEN** the request is refused because it needs a shuttle and the Spreader stays in its garage

#### Scenario: Spreaders do not harvest
- **WHEN** a warehouse has only a Spreader shuttle and its field has a harvest due
- **THEN** no shuttle harvests the field

#### Scenario: Tooltip
- **WHEN** a player hovers over a Spreader item
- **THEN** the tooltip says it fertilizes fields and does not fit into lifts

### Requirement: Fertilizer from stock above the reserve
Before it drives to the field the Spreader SHALL pick the field's fertilizer from the warehouse: the plots that need a dose times `doses_per_plot` (in `field.json`, example 2), but never more than the warehouse holds above the head's reserve and never more than its cargo has room for. It SHALL drive to a rack holding the fertilizer, and to further ones while it is short. Racks it reaches only by lift SHALL be skipped. With nothing above the reserve no run SHALL go out.

#### Scenario: Picks what the plots need
- **WHEN** two plots need a dose, `doses_per_plot` is 2, the reserve is 0 and a rack holds 20 bone meal
- **THEN** the Spreader takes 4 bone meal from the rack

#### Scenario: Stock equal to the reserve
- **WHEN** a field's reserve is 10 and the warehouse holds exactly 10 bone meal
- **THEN** no Spreader drives out and the rack keeps its 10 bone meal

#### Scenario: Only the part above the reserve
- **WHEN** four plots need a dose, `doses_per_plot` is 2, the reserve is 5 and the warehouse holds 8 bone meal
- **THEN** the Spreader takes 3 bone meal

## ADDED Requirements

### Requirement: Leftover goes back to a rack
When no plot needs a dose any more, fertilizer the Spreader still carries SHALL go back into warehouse storage by the put-away routing of the harvest, to racks it can reach; when nothing has room it SHALL wait at its garage and try again about once a second. Then it SHALL go home.

#### Scenario: Leftover back in the rack
- **WHEN** a Spreader took 3 bone meal for a wheat plot one dose short of ripe
- **THEN** the plot gets one dose and the other 2 bone meal go back into the rack

## REMOVED Requirements

### Requirement: Leftover put away
**Reason**: Leftovers could go to blocks that take their part at once (crates and foreign inventories), which are gone.
**Migration**: See "Leftover goes back to a rack".
