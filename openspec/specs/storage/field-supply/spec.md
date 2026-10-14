# storage/field-supply Specification

## Purpose
Supply runs keep a Field Head's buffer of seeds, fertilizer and anti-corrosion items stocked by shuttle, so field shuttles that never ride a lift find what they need at the head.

## Requirements

### Requirement: Supply jobs
At every field check (`field_check_interval`) a warehouse SHALL queue a supply run for a field on its rails when the field's head serves it, no supply run for that head is queued or under way, and a part of the head's buffer is below half its level while the warehouse holds that item: seeds of the crop while a crop is set, the fertilizer while fertilizing is on and the warehouse holds more of it than the head's reserve, and anti-corrosion items always. Supply runs SHALL go to shuttles without a tool, like picks, and those shuttles MAY ride the lift car.

#### Scenario: Low on seeds
- **WHEN** a field with wheat set has 20 wheat seeds in its buffer, `seed_buffer` is 64, and a rack holds wheat seeds
- **THEN** a shuttle without a tool leaves its garage on a supply run for that head

#### Scenario: Half full is enough
- **WHEN** every part of a head's buffer holds at least half its level
- **THEN** no supply run goes out

#### Scenario: No tool-less shuttle
- **WHEN** a warehouse's garages hold only a Harvester and a Spreader and a head's buffer is empty
- **THEN** no supply run goes out and the head's GUI says no shuttle can bring supplies

### Requirement: Supply run
A supply run SHALL pick, for each part of the head's buffer, what it is short of up to its level: seeds of the crop, the fertilizer only above the head's reserve, and the head's anti-corrosion item only above its anti-corrosion reserve, as far as its cargo has room. It SHALL drive to the racks holding them, riding the lift where needed, then to the farm rail beside the head, and unload into the buffer. Items in the buffer that are no longer the field's crop, fertilizer or anti-corrosion item SHALL be taken along and put away. Then it SHALL go home.

#### Scenario: Stock behind a lift
- **WHEN** the only bone meal lies in a rack on another floor reached only by an elevator shaft, and a field with bone meal fertilizing has an empty buffer
- **THEN** a shuttle without a tool rides the lift, brings bone meal into the head's buffer, and a Spreader then doses the field

#### Scenario: Reserve kept
- **WHEN** a head's reserve is 10, its fertilizer buffer is empty, `fertilizer_buffer` is 64 and the warehouse holds 30 bone meal
- **THEN** the supply run brings 20 bone meal and 10 stay in the rack

#### Scenario: Crop changed
- **WHEN** a head's buffer holds wheat seeds and the player sets carrots
- **THEN** the next supply run brings carrots and takes the wheat seeds back to storage

### Requirement: Supply runs that cannot go on
When the Field Head is removed or no longer serves the field during a supply run, the shuttle SHALL put what it carries away and go home. When the rail it stands on or drives over is removed, its garage loses the way home, its garage is removed or the player recalls the garage's shuttles, it SHALL be back in its garage and its cargo SHALL drop at the garage. A supply run that ends in one of these ways SHALL NOT be queued again by itself.

#### Scenario: Head broken on the way
- **WHEN** the player breaks the Field Head while a supply run drives toward it with seeds
- **THEN** the shuttle puts the seeds into storage and parks
