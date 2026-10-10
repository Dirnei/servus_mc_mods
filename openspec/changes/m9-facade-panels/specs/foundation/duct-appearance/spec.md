# Spec Delta

## ADDED Requirements

### Requirement: Panels sit next to the duct's parts
A duct with panels SHALL render its lines, arms, bands and rings as without panels, and each panel on its face in addition. An arm toward a hollow panel SHALL show through the hole. A face closed by a solid panel SHALL show no arm and no ring. The hidden duct overlay SHALL NOT draw ducts behind panels; the player sees the panels.

#### Scenario: Hollow panel on a two line duct
- **WHEN** a hollow stone panel covers the east face of a duct carrying both lines with an arm to the east
- **THEN** the two line arm shows through the hole of the stone frame

#### Scenario: Solid panel on an endpoint
- **WHEN** a solid panel covers the face of a duct toward a chest
- **THEN** that face shows the panel and no arm or ring

## REMOVED Requirements

### Requirement: Facades cover everything
**Reason**: Facades are now panels on single faces and no longer replace the duct's look; see "Panels sit next to the duct's parts".
**Migration**: None needed; six solid panels of one block give the old full block look.
