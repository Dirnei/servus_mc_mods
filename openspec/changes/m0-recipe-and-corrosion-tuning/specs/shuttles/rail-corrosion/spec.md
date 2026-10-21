## MODIFIED Requirements

### Requirement: Aging by water used
A Field Head serving a field SHALL count the water it uses to hydrate the field. Every `corrosion_water` mB of that water (from `data/<ns>/shuttles/field.json`, example 9000) one aging step SHALL be due. When an aging step is due and the head holds an anti-corrosion item, the head SHALL use one of them and the stage SHALL stay; otherwise the field SHALL go one stage further. The counted water SHALL be kept when the world is saved. Aging SHALL happen only when water is used, never by a timer or a scan.

#### Scenario: Step without upkeep
- **WHEN** a copper field's head has used `corrosion_water` mB and holds no anti-corrosion item
- **THEN** the field becomes exposed

#### Scenario: Honeycomb holds the stage
- **WHEN** a weathered field's head holding three honeycomb reaches an aging step
- **THEN** the field stays weathered and the head holds two honeycomb

#### Scenario: No water, no aging
- **WHEN** a head has no water and its field is not hydrated
- **THEN** its field does not age
