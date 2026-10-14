# Spec Delta

## MODIFIED Requirements

### Requirement: Three principle fluids
Alchemy SHALL register three fluids, Salt, Sulfur and Mercury (`servus_alchemy:salt`, `servus_alchemy:sulfur`, `servus_alchemy:mercury`), each with its own fluid type, a source and a flowing form and a distinct colour.

#### Scenario: Shown in a tank
- **WHEN** a Fluid Tank holds 1,000 mB of Sulfur
- **THEN** the tank shows a yellow fluid and its read out names Sulfur
