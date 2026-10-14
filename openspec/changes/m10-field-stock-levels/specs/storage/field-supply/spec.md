# Spec Delta

## MODIFIED Requirements

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
