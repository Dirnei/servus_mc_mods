## ADDED Requirements

### Requirement: Anti-corrosion slot
Anti-corrosion items SHALL be the item tag `servus_storage:anti_corrosion`, holding honeycomb by default, so packs can add their own. The Field Head SHALL have one upkeep slot that takes only those items, filled by hand in its GUI or by packets of the data line through the head's packet inlet. The head SHALL register no item capability, so hoppers and item pipes neither insert into nor extract from it. The head SHALL be an upkeep holder for that slot: a data line with a supplier on it keeps the slot at `anti_corrosion_level` (from `field.json`, example 8) of the item in the slot, or of the first item of the tag while the slot is empty. The slot's items SHALL drop when the head is broken.

#### Scenario: Filled by hand
- **WHEN** a player puts honeycomb into the anti-corrosion slot of the head's GUI
- **THEN** the honeycomb stays in the slot

#### Scenario: Hopper cannot fill it
- **WHEN** a hopper holding honeycomb points into a Field Head
- **THEN** the honeycomb stays in the hopper

#### Scenario: Other items refused
- **WHEN** packets of wheat seeds are sent to the head
- **THEN** the head takes none

#### Scenario: Network keeps it stocked
- **WHEN** a head with two honeycomb sits on a data line with a warehouse holding honeycomb in a rack served by a shuttle and an Access Point, and `anti_corrosion_level` is 8
- **THEN** the network asks for six honeycomb and the head ends up with eight

## REMOVED Requirements

### Requirement: Anti-corrosion upkeep
**Reason**: Hoppers and ducts filled the slot through the head's item capability, which the head no longer registers.
**Migration**: See "Anti-corrosion slot": fill it by hand or let the data line keep it stocked.
