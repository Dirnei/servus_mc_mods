# Spec Delta

## MODIFIED Requirements

### Requirement: Principles leave through the sides
The Calciner SHALL hold one tank per principle (capacity from machine stats, example 4,000 mB), declared as output tanks Salt, Sulfur and Mercury of its Fluids layer. It SHALL offer each tank through the fluid capability only on sides whose Fluids layer is set to that tank or to All, push a tank into touching fluid handlers on those sides, and never accept fluid.

#### Scenario: Tank beside the Calciner
- **WHEN** a Fluid Tank touches a side of a working Calciner whose Fluids layer is All
- **THEN** the principles the Calciner makes flow into the tank (a single tank keeps the first principle it receives)

#### Scenario: Pipe drains one principle
- **WHEN** a Fluid Pipe takes fluid from the Calciner and only Sulfur is wanted downstream
- **THEN** the Calciner hands out Sulfur and keeps its Salt and Mercury

#### Scenario: Separate pipes per principle
- **WHEN** the Calciner's left side is set to Salt, its right side to Mercury, and a pipe arm on each side leads to a separate Fluid Tank
- **THEN** one tank fills with Salt only and the other with Mercury only
