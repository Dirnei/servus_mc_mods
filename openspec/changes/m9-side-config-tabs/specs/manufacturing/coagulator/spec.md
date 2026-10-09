# Spec Delta

## MODIFIED Requirements

### Requirement: Each principle into its own tank
The Coagulator SHALL hold one tank per principle (capacity from machine stats, example 4,000 mB), declared as input tanks Salt, Sulfur and Mercury of its Fluids layer. It SHALL accept through the fluid capability only Salt into the Salt tank, Sulfur into the Sulfur tank and Mercury into the Mercury tank, and each tank only on sides whose Fluids layer is set to that tank or to All. It SHALL never give fluid away.

#### Scenario: Mixed pipe
- **WHEN** a Fluid Pipe offers Salt and then water to a Coagulator side set to All
- **THEN** the Salt goes into the Salt tank and the water is refused

#### Scenario: Side for one principle
- **WHEN** a Coagulator side is set to Mercury and a pipe offers Salt there
- **THEN** the Coagulator accepts nothing on that side
