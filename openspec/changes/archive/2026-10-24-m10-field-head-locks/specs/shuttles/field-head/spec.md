## MODIFIED Requirements

### Requirement: Supply buffer
Each Field Head SHALL keep a buffer of three parts: seeds of its crop up to `seed_buffer` (in `field.json`, example 64), its fertilizer up to `fertilizer_buffer` (example 64), and anti-corrosion items up to `anti_corrosion_level` (example 8). Only supply runs SHALL fill it, and only the field's Harvester and Spreader SHALL take from it. Players, hoppers and pipes SHALL neither put items in nor take them out: the head SHALL register no item capability, no packet inlet and no upkeep. Each part SHALL take slot locks (see `shuttles/slot-locks`): a supply run reserves the room it fills and a field shuttle reserves the items it takes, and nobody else fills reserved room or takes reserved items. The buffer and its locks SHALL be kept when the world is saved, a head saved without locks SHALL load with its buffer unlocked, and the buffer's items SHALL drop when the head is broken.

#### Scenario: Buffer survives a reload
- **WHEN** a head with 40 wheat seeds, 12 bone meal and 5 honeycomb in its buffer is saved and loaded
- **THEN** its buffer holds the same items

#### Scenario: Locks survive a reload
- **WHEN** a supply run has locked room for 24 wheat seeds in a head's buffer and the server restarts before the shuttle arrives
- **THEN** after the restart the room is still reserved for that run and the shuttle fills it

#### Scenario: Broken head drops its buffer
- **WHEN** a player breaks a head with 40 wheat seeds in its buffer
- **THEN** the 40 wheat seeds drop with the head

#### Scenario: Not on the data line
- **WHEN** a data line duct touches a Field Head
- **THEN** the duct finds no packet inlet and no upkeep on the head
