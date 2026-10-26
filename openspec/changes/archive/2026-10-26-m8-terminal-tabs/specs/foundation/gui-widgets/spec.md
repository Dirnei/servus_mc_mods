# Spec Delta

## ADDED Requirements

### Requirement: One line number stepper
Foundation SHALL offer a one line number stepper: a minus button, a digits only field and a plus button side by side, one button high. A click on plus or minus SHALL step by 1, a shift click by a full stack up or down like the stack step of the number stepper. The screen SHALL choose the stack size, the lowest and highest value and what happens on a change; the value SHALL stay between the lowest and highest value.

#### Scenario: Plain steps
- **WHEN** a one line stepper shows 15 and the player clicks plus twice and minus once
- **THEN** it shows 16, then 17, then 16

#### Scenario: Shift steps a stack
- **WHEN** a one line stepper with a stack of 64 shows 10 and the player shift clicks plus, then shift clicks minus
- **THEN** it shows 64, then 1 when the lowest value is 1

#### Scenario: Stays in range
- **WHEN** a one line stepper with lowest value 0 shows 0 and the player clicks minus
- **THEN** it still shows 0

### Requirement: One line stepper input
Turning the mouse wheel over the one line stepper's field SHALL step by 1. A typed number SHALL be clamped to the lowest and highest value when the field loses focus or Enter is pressed, and the screen SHALL be told every new value.

#### Scenario: Wheel over the field
- **WHEN** the mouse is over the field showing 7 and the player turns the wheel down once
- **THEN** it shows 6 and the screen is told 6

#### Scenario: Typed number clamped
- **WHEN** the highest value is 99999 and the player types 250000 and presses Enter
- **THEN** the field shows 99999
