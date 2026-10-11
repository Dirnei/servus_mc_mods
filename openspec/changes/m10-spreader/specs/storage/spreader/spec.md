## Purpose

Spreader shuttles fertilize the fields of their warehouse: they pick fertilizer from storage above each field's reserve, dose the unripe crops rail by rail and put what is left back into storage.

## ADDED Requirements

### Requirement: Spreader module
The Spreader SHALL be a craftable tool module. It SHALL go into a shuttle through the install slot of the Shuttle Workshop, into the shuttle's one tool place, replacing a fitted tool. A shuttle with a Spreader SHALL be field only and SHALL take only fertilize runs: no warehouse picks, no put-aways and no harvest runs. A shuttle without a Spreader SHALL NOT take fertilize runs. The Spreader SHALL be wide, never riding a lift car, and its tooltip SHALL say what it does and that it does not fit into lifts.

#### Scenario: Fit a Spreader
- **WHEN** a player puts a Mk1 shuttle into the workshop and a Spreader into the install slot
- **THEN** the shuttle has the Spreader as its tool

#### Scenario: Spreaders stay out of the warehouse
- **WHEN** a warehouse has only a Spreader shuttle and a player requests cobblestone from a crate beside a rail
- **THEN** the cobblestone is served instantly as without shuttles and the Spreader stays in its garage

#### Scenario: Spreaders do not harvest
- **WHEN** a warehouse has only a Spreader shuttle and its field has a harvest due
- **THEN** no shuttle harvests the field

#### Scenario: Tooltip
- **WHEN** a player hovers over a Spreader item
- **THEN** the tooltip says it fertilizes fields and does not fit into lifts

### Requirement: Fertilize jobs
At every field check (`field_check_interval`) a warehouse SHALL queue a fertilize run for a field on its rails when it has a Spreader shuttle that reaches the field without a lift, no fertilize run for that field is queued or under way, the field's head has fertilizing on and plots that need a dose, the warehouse holds more of the chosen fertilizer than the head's reserve, and at least `fertilize_interval` ticks (in `field.json`, example 600) passed since the field last called a Spreader. Any free Spreader shuttle of the warehouse SHALL serve any of its fields.

#### Scenario: Freshly planted field calls a Spreader
- **WHEN** a field set to fertilize after planting gets two newly planted wheat plots and the warehouse holds bone meal above the reserve
- **THEN** within a few seconds a Spreader shuttle leaves its garage

#### Scenario: Interval between calls
- **WHEN** a field finished a fertilize run and still has plots that need a dose
- **THEN** no new run goes out before `fertilize_interval` ticks passed since the last call

#### Scenario: One Spreader for two fields
- **WHEN** a warehouse has one Spreader shuttle and two fields that need fertilizing
- **THEN** the Spreader serves one field and then the other

### Requirement: Fertilizer from stock above the reserve
Before it drives to the field the Spreader SHALL pick the field's fertilizer from the warehouse: the plots that need a dose times `doses_per_plot` (in `field.json`, example 2), but never more than the warehouse holds above the head's reserve and never more than its cargo has room for. It SHALL drive to a storage block holding the fertilizer, and to further ones while it is short; a block no rail reaches SHALL give its part at once. Storage it reaches only by lift SHALL be skipped. With nothing above the reserve no run SHALL go out.

#### Scenario: Picks what the plots need
- **WHEN** two plots need a dose, `doses_per_plot` is 2, the reserve is 0 and a crate holds 20 bone meal
- **THEN** the Spreader takes 4 bone meal from the crate

#### Scenario: Stock equal to the reserve
- **WHEN** a field's reserve is 10 and the warehouse holds exactly 10 bone meal
- **THEN** no Spreader drives out and the crate keeps its 10 bone meal

#### Scenario: Only the part above the reserve
- **WHEN** four plots need a dose, `doses_per_plot` is 2, the reserve is 5 and the warehouse holds 8 bone meal
- **THEN** the Spreader takes 3 bone meal

### Requirement: Dosing the field
On the field the Spreader SHALL drive to the nearest farm rail whose plots need a dose and work them, then on to the next nearest rail, until no rail has plots that need a dose or its fertilizer is used up. Each plot SHALL get up to `doses_per_plot` doses in one round, each dose one item of the fertilizer applied like bone meal on the crop, and working a plot SHALL take `work_ticks` shortened by Handling like the Harvester. A plot SHALL stop getting doses once its crop is ripe; ripe plots and plots without an age crop SHALL never be dosed. Every dose SHALL let the head count the plot's growth like bone meal used by a player.

#### Scenario: Doses make crops grow
- **WHEN** the Spreader doses a wheat crop at its first age twice
- **THEN** the wheat is at least four ages older and the head counts it as it is now

#### Scenario: Ripe plots untouched
- **WHEN** a field set to fertilize until a ripe share has one ripe and one young wheat plot
- **THEN** only the young wheat gets doses

### Requirement: Leftover put away
When no plot needs a dose any more, fertilizer the Spreader still carries SHALL go back into warehouse storage by the put-away routing of the harvest, to blocks it can reach or that take their part at once; when nothing has room it SHALL wait at its garage and try again about once a second. Then it SHALL go home.

#### Scenario: Leftover back in the crate
- **WHEN** a Spreader took 3 bone meal for a wheat plot one dose short of ripe
- **THEN** the plot gets one dose and the other 2 bone meal go back into the crate

### Requirement: Fertilize runs that cannot go on
When the Field Head is removed, no longer serves the field or fertilizing is turned off during a run, the Spreader SHALL stop dosing, put away what it carries and go home. When the rail it stands on or drives over is removed, its garage loses the way home, its garage is removed or the player recalls the garage's shuttles, it SHALL be back in its garage and its cargo SHALL drop at the garage. A fertilize run that ends in one of these ways SHALL NOT be queued again by itself.

#### Scenario: Head broken mid run
- **WHEN** the player breaks the Field Head while the Spreader doses its field
- **THEN** the Spreader stops, puts the fertilizer it carries into storage and parks

#### Scenario: Recall
- **WHEN** the player recalls the garage while its Spreader is out
- **THEN** the Spreader is back in its garage and its fertilizer lies on the ground at the garage
