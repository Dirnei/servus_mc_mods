# Spec Delta

## MODIFIED Requirements

### Requirement: One head per field
A Field Head SHALL be a craftable block. It SHALL serve the field of a farm rail touching one of its four horizontal sides; when it touches farm rails of more than one field it SHALL serve the field of the first one in the order north, east, south, west. A field SHALL have at most one serving head: when several heads touch the same field, only one serves it and the others SHALL show that the field has another head. A field larger than the maximum size (`max_rails` in `data/<ns>/shuttles/field.json`, example 128 rails) SHALL NOT be served, and the head SHALL show that the field is too large. A head without a farm rail beside it SHALL show that it has no field.

#### Scenario: Head beside a run
- **WHEN** a Field Head is placed beside the end of a run of four farm rails
- **THEN** its GUI shows a field of four rails

#### Scenario: Second head
- **WHEN** a second Field Head is placed beside another rail of the same run
- **THEN** one of the two heads serves the field and the other shows that the field has another head

#### Scenario: No rail
- **WHEN** a Field Head stands with no farm rail beside it
- **THEN** its GUI shows that it has no field

### Requirement: Fertilize settings per head
Each Field Head SHALL keep its own fertilizer (an item of the tag `servus_shuttles:fertilizer`, bone meal by default in the tag, or none), a fertilize mode (Off, After planting, Until ripe share), a fertilize threshold in percent (default from `fertilize_share` in `field.json`, example 0.5) and a Keep in stock reserve (default from `fertilizer_reserve`, example 64, never below 0). Fertilizing SHALL be on only while a fertilizer is chosen and the mode is not Off. The settings SHALL be kept when the world is saved.

#### Scenario: Defaults
- **WHEN** a player places a new Field Head
- **THEN** fertilizing is off, no fertilizer is chosen and the reserve is `fertilizer_reserve`

#### Scenario: Reserve never below zero
- **WHEN** the reserve is 5 and the player clears its field and presses Enter, or a client sends -10 as the reserve
- **THEN** the reserve is 0

#### Scenario: Settings saved
- **WHEN** a head with bone meal, Until ripe share at 70 percent and reserve 20 is saved and loaded
- **THEN** it has the same fertilizer, mode, threshold and reserve

### Requirement: Anti-corrosion item and reserve per head
Each Field Head SHALL keep its own anti-corrosion item: an item of the tag `servus_shuttles:anti_corrosion`, the first item of the tag until the player chooses one, or Off. While it is Off the anti-corrosion part of the buffer SHALL have no level, the items it holds SHALL count as stale, and the field SHALL age without upkeep. The choice SHALL be kept when the world is saved, and the server SHALL refuse an item outside the tag.

Each Field Head SHALL also keep an anti-corrosion Keep in stock reserve (default from `anti_corrosion_reserve` in `field.json`, example 0, never below 0), kept when the world is saved. Supply runs SHALL never take the warehouse stock of the anti-corrosion item at or below it, and a low anti-corrosion part SHALL call a supply run only while the warehouse holds more than the reserve.

#### Scenario: Honeycomb kept
- **WHEN** a head's anti-corrosion reserve is 5, its anti-corrosion part is empty, `anti_corrosion_level` is 8 and the warehouse holds 10 honeycomb
- **THEN** a supply run brings 5 honeycomb, 5 stay in the warehouse, and no further supply run comes for honeycomb

#### Scenario: Default keeps nothing
- **WHEN** a head's anti-corrosion reserve was never set and `anti_corrosion_reserve` is 0
- **THEN** supply runs may take the last honeycomb of the warehouse

#### Scenario: Anti-corrosion Off
- **WHEN** the player chooses Off for the anti-corrosion item of a head whose buffer holds 3 honeycomb
- **THEN** no supply run brings honeycomb, the next supply run takes the 3 honeycomb back, and the field ages at its next step

#### Scenario: Refuse a non anti-corrosion item
- **WHEN** a client chooses dirt as the anti-corrosion item
- **THEN** the head keeps its anti-corrosion item

#### Scenario: Anti-corrosion reserve saved
- **WHEN** a head with anti-corrosion Off and an anti-corrosion reserve of 6 is saved and loaded
- **THEN** its anti-corrosion item is Off and its reserve is 6
