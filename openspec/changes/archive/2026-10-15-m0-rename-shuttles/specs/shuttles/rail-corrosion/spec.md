# Spec Delta

## MODIFIED Requirements

### Requirement: Aging by water used
A Field Head serving a field SHALL count the water it uses to hydrate the field. Every `corrosion_water` mB of that water (from `data/<ns>/shuttles/field.json`, example 64000) one aging step SHALL be due. When an aging step is due and the head holds an anti-corrosion item, the head SHALL use one of them and the stage SHALL stay; otherwise the field SHALL go one stage further. The counted water SHALL be kept when the world is saved. Aging SHALL happen only when water is used, never by a timer or a scan.

#### Scenario: Step without upkeep
- **WHEN** a copper field's head has used `corrosion_water` mB and holds no anti-corrosion item
- **THEN** the field becomes exposed

#### Scenario: Honeycomb holds the stage
- **WHEN** a weathered field's head holding three honeycomb reaches an aging step
- **THEN** the field stays weathered and the head holds two honeycomb

#### Scenario: No water, no aging
- **WHEN** a head has no water and its field is not hydrated
- **THEN** its field does not age

### Requirement: Anti-corrosion slot
Anti-corrosion items SHALL be the item tag `servus_shuttles:anti_corrosion`, holding honeycomb by default, so packs can add their own. The anti-corrosion part of the Field Head's supply buffer SHALL take only those items. Supply runs SHALL keep it at `anti_corrosion_level` (from `field.json`, example 8) of the item it holds, or of the first item of the tag while it is empty. Players, hoppers, item pipes and the data line SHALL neither put items in nor take them out. Its items SHALL drop when the head is broken.

#### Scenario: Filled by hand
- **WHEN** a player tries to put honeycomb into the head's GUI
- **THEN** the honeycomb stays with the player

#### Scenario: Hopper cannot fill it
- **WHEN** a hopper holding honeycomb points into a Field Head
- **THEN** the honeycomb stays in the hopper

#### Scenario: Other items refused
- **WHEN** a supply run brings honeycomb and the head's anti-corrosion part already holds a different anti-corrosion item
- **THEN** the head takes only the item it holds and the rest goes back to storage

#### Scenario: Network keeps it stocked
- **WHEN** a head holds two honeycomb, `anti_corrosion_level` is 8 and a rack holds honeycomb
- **THEN** a supply run brings six honeycomb and the head ends up with eight
