# Spec Delta

## MODIFIED Requirements

### Requirement: Fertilize jobs
At every field check (`field_check_interval`) a warehouse SHALL queue a fertilize run for a field on its rails when it has a Spreader shuttle that reaches the field without a lift, no fertilize run for that field is queued or under way, the field's head has fertilizing on and plots that need a dose, the head's buffer holds the chosen fertilizer, and at least `fertilize_interval` ticks (in `field.json`, example 600) passed since the field last called a Spreader. Any free Spreader shuttle of the warehouse SHALL serve any of its fields.

#### Scenario: Freshly planted field calls a Spreader
- **WHEN** a field set to fertilize after planting gets two newly planted wheat plots and the head's buffer holds bone meal
- **THEN** within a few seconds a Spreader shuttle leaves its garage

#### Scenario: Interval between calls
- **WHEN** a field finished a fertilize run and still has plots that need a dose
- **THEN** no new run goes out before `fertilize_interval` ticks passed since the last call

#### Scenario: One Spreader for two fields
- **WHEN** a warehouse has one Spreader shuttle and two fields that need fertilizing
- **THEN** the Spreader serves one field and then the other

### Requirement: Fertilizer from stock above the reserve
The Spreader SHALL take the field's fertilizer from the head's buffer at the farm rail beside the head, never from racks: the plots that need a dose times `doses_per_plot` (in `field.json`, example 2), but never more than the buffer holds and never more than its cargo has room for. Only supply runs take fertilizer out of the warehouse, and only above the head's reserve. With an empty buffer no run SHALL go out.

#### Scenario: Picks what the plots need
- **WHEN** two plots need a dose, `doses_per_plot` is 2 and the head's buffer holds 20 bone meal
- **THEN** the Spreader takes 4 bone meal from the buffer

#### Scenario: Stock equal to the reserve
- **WHEN** a field's reserve is 10, its buffer is empty and the warehouse holds exactly 10 bone meal
- **THEN** no supply run takes bone meal, no Spreader drives out and the rack keeps its 10 bone meal

#### Scenario: Only the part above the reserve
- **WHEN** four plots need a dose, `doses_per_plot` is 2, the reserve is 5, the buffer is empty and the warehouse holds 8 bone meal
- **THEN** a supply run brings 3 bone meal into the buffer and the Spreader takes those 3

### Requirement: Leftover goes back to a rack
When no plot needs a dose any more, fertilizer the Spreader still carries SHALL go back into the head's buffer up to its level; the rest SHALL go into warehouse storage by the put-away routing of the harvest, to racks it can reach, and when nothing has room it SHALL wait at its garage and try again about once a second. Then it SHALL go home.

#### Scenario: Leftover back in the rack
- **WHEN** a Spreader took 3 bone meal for a wheat plot one dose short of ripe
- **THEN** the plot gets one dose and the other 2 bone meal go back into the head's buffer
