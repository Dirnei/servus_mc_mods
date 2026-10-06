# Spec Delta

## REMOVED Requirements

### Requirement: Four compartments
**Reason**: The shelving rack is folded into the crate family. The Quad Crate has four compartments that split the crate capacity.
**Migration**: Place a Quad Crate where a rack stood. The rack block, item and recipe no longer exist.

### Requirement: Hand interaction by compartment
**Reason**: Per compartment hand interaction now lives in the `storage/crate` requirement "Hand interaction".
**Migration**: Use the crate's compartment clicks; they behave the same way for putting and taking.

### Requirement: Automation access
**Reason**: One slot per compartment is now the `storage/crate` requirement "Automation access".
**Migration**: Hoppers and ducts see the same slot layout on a Quad Crate.

### Requirement: Visible contents
**Reason**: The per compartment front display is now the `storage/crate` requirement "Front display".
**Migration**: None, the Quad Crate shows the same two by two display.

### Requirement: Breaking and dismantling
**Reason**: Crates never spill; they are protected while they hold items and keep contents on dismantle.
**Migration**: Empty a crate or sneak dismantle it with the Configuration Carrot.
