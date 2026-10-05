# Spec Delta

## ADDED Requirements

### Requirement: Powered machines
A powered machine SHALL have an FE buffer that accepts energy through the energy capability on every side, SHALL spend its energy per tick only while it has work and enough energy, and SHALL pause with its progress kept when energy runs out.

#### Scenario: Enough energy
- **WHEN** a powered machine has work and at least its per tick cost in the buffer
- **THEN** it advances one tick of progress and the buffer drops by the per tick cost

#### Scenario: Out of energy
- **WHEN** the buffer holds less than the per tick cost
- **THEN** progress does not advance and is not reset

#### Scenario: Buffer never gives energy away
- **WHEN** a cable or generator tries to extract from a powered machine
- **THEN** it receives nothing
