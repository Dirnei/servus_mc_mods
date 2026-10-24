## ADDED Requirements

### Requirement: Number stepper
Foundation SHALL offer a number stepper widget: a digits only field with plus step buttons above it and the matching minus buttons below it. The screen SHALL choose the steps, the stack size, the lowest and highest value and what happens on a change. A plain step SHALL add or subtract its amount; a stack step SHALL go to the next full stack up or the previous one down. The value SHALL stay between the lowest and highest value.

#### Scenario: Steps of a stack of 64
- **WHEN** a stepper with steps 1, 10 and a stack of 64 shows 1 and the player presses +64, then +64, then -64
- **THEN** it shows 64, then 128, then 64

#### Scenario: Never below the lowest value
- **WHEN** a stepper with lowest value 1 shows 5 and the player presses -10
- **THEN** it shows 1

### Requirement: Number stepper input
Turning the mouse wheel over the stepper's field SHALL step by 1. A typed number SHALL be clamped to the lowest and highest value when the field loses focus or Enter is pressed.

#### Scenario: Wheel over the field
- **WHEN** the mouse is over the field showing 7 and the player turns the wheel up once
- **THEN** it shows 8

#### Scenario: Typed number clamped
- **WHEN** the highest value is 99999 and the player types 250000 and presses Enter
- **THEN** the field shows 99999 and the screen is told the new value

### Requirement: Scrollable item list
Foundation SHALL offer a scrollable item list widget: a heading and below it rows of a half size item icon, a count and the item name, cut to the list's width, with a thin scrollbar when there are more rows than fit. Turning the mouse wheel over the list SHALL scroll it by one row and SHALL stop at the first and last row. New rows SHALL keep the scroll position where it still fits. Hovering a row SHALL show the item's tooltip.

#### Scenario: More rows than fit
- **WHEN** a list three rows high gets eleven rows
- **THEN** it shows the first three rows and a scrollbar, and turning the wheel down twice shows rows three to five

#### Scenario: Scroll stops at the end
- **WHEN** the list shows its last three rows and the player turns the wheel down
- **THEN** it still shows the last three rows

#### Scenario: Fewer rows after an update
- **WHEN** the list is scrolled to row eight and gets four rows
- **THEN** it shows rows two to four

### Requirement: Widget rules are pure
The step rules of the number stepper and the scroll rules of the item list SHALL be plain Java without Minecraft classes and covered by JUnit tests.

#### Scenario: Unit tested
- **WHEN** the unit tests run
- **THEN** the stack, plain step, clamp and scroll limit rules are checked without starting Minecraft
