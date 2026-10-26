# foundation/gui-widgets Specification

## Purpose
Reusable screen widgets in Foundation, a number stepper and a scrollable item list, so every Servus mod's screens enter amounts and show item lists the same way.

## Requirements

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
Foundation SHALL offer a scrollable item list widget: a heading and below it rows of a half size item icon, a count and the item name, cut to the list's width, with the shared scrollbar beside the rows. Turning the mouse wheel over the list SHALL scroll it by one row and SHALL stop at the first and last row; the scrollbar SHALL scroll it by dragging and by a press on its track. New rows SHALL keep the scroll position where it still fits. Hovering a row SHALL show the item's tooltip.

#### Scenario: More rows than fit
- **WHEN** a list three rows high gets eleven rows
- **THEN** it shows the first three rows and a scrollbar, and turning the wheel down twice shows rows three to five

#### Scenario: Scroll stops at the end
- **WHEN** the list shows its last three rows and the player turns the wheel down
- **THEN** it still shows the last three rows

#### Scenario: Fewer rows after an update
- **WHEN** the list is scrolled to row eight and gets four rows
- **THEN** it shows rows two to four

#### Scenario: Drag the list's scrollbar
- **WHEN** the To craft list of the amount panel holds twelve rows, shows three, and the player drags its thumb to the bottom
- **THEN** the list shows rows ten to twelve

### Requirement: Widget rules are pure
The step rules of the number stepper, the scroll rules of the item list and the scrollbar's rules (thumb length with its minimum, thumb position, the scroll position for a drag with its grab point and for a press on the track) SHALL be plain Java without Minecraft classes and covered by JUnit tests.

#### Scenario: Unit tested
- **WHEN** the unit tests run
- **THEN** the stack, plain step, clamp and scroll limit rules are checked without starting Minecraft

#### Scenario: Scrollbar rules unit tested
- **WHEN** the unit tests run
- **THEN** the thumb length and its minimum, the thumb position, a drag that keeps the grab point, a drag past either end and a drag with nothing to scroll are checked without starting Minecraft

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

### Requirement: Scrollbar
Foundation SHALL offer a scrollbar widget for a vertical list or grid: a sunken track beside the view and a raised thumb on it. The thumb's length SHALL be the track's length times the visible share of the content, never shorter than a minimum the player can grab (example: 6 pixels), and its position SHALL show how far the view is scrolled, at the top of the track for the first row and at the bottom for the last.

#### Scenario: Thumb shows the visible share
- **WHEN** a view shows 3 of 11 rows on a track 27 pixels long
- **THEN** the thumb is 7 pixels long and sits at the top of the track

#### Scenario: Thumb at the end
- **WHEN** the same view is scrolled to its last three rows
- **THEN** the thumb's bottom edge is at the bottom of the track

#### Scenario: Long content keeps a usable thumb
- **WHEN** a view shows 3 of 1000 rows on a track 27 pixels long
- **THEN** the thumb is 6 pixels long, the minimum, not 2 pixels

### Requirement: Dragging the scrollbar
Pressing the left mouse button on the thumb SHALL grab it at that point, and moving the mouse with the button held SHALL move the thumb with the mouse, scrolling the view to match. Pressing the left button on the track beside the thumb SHALL move the thumb so it centres on the mouse and then drag it the same way. The drag SHALL go on while the mouse leaves the bar and SHALL end when the button is released; the view SHALL stop at its first and last row.

#### Scenario: Grab the thumb without a jump
- **WHEN** the player presses the left button near the bottom edge of the thumb
- **THEN** the view does not move until the mouse moves

#### Scenario: Drag past the end
- **WHEN** the player drags the thumb down and moves the mouse below the bar
- **THEN** the view shows its last rows and the thumb stays at the bottom of the track

#### Scenario: Jump on the track
- **WHEN** the player presses the left button halfway down the track of a view with 20 rows to scroll
- **THEN** the view jumps to about row 10 and follows the mouse while the button is held

### Requirement: Scrollbar and the mouse wheel
Turning the mouse wheel over the view or over its scrollbar SHALL scroll the view by the view's own step and move the thumb to match, stopping at the first and last row.

#### Scenario: Wheel over the bar
- **WHEN** the mouse is over the scrollbar of a row based view and the player turns the wheel down once
- **THEN** the view moves one row down and the thumb follows

### Requirement: Scrollbar with nothing to scroll
When the whole content fits the view, the scrollbar SHALL still show where the view's layout puts it, with a greyed thumb that fills the whole track. Pressing or dragging on it SHALL leave the view where it is.

#### Scenario: Everything fits
- **WHEN** a view shows 3 rows and has 2
- **THEN** the thumb fills the track in grey, and a press or a drag on the bar does not move the view

### Requirement: Every scrollable view has a scrollbar
Every list or grid in a Servus screen or popup that holds more than it can show and scrolls SHALL show the shared scrollbar beside it, with the same look and behaviour in every mod. A view with several tabs or modes that scroll SHALL show the bar for the tab or mode on display, at that tab's scroll position.

#### Scenario: Item picker
- **WHEN** the player opens the filter picker of a Utility Duct, the crop picker of a Field Head or the fluid lock picker of a Fluid Gantry, and the list holds more entries than fit
- **THEN** a scrollbar beside the list shows the visible share and can be dragged

#### Scenario: Channel popup
- **WHEN** the channel popup lists ten channels on "All" and shows six
- **THEN** the player can drag its thumb down to see the last four, and a press on the track jumps there

#### Scenario: Claim an export picker
- **WHEN** the Claim an export picker of an Outbound Station lists more lines than its four rows
- **THEN** a scrollbar beside the rows shows the visible share, and dragging it or turning the wheel shows the rest

#### Scenario: Same look in every mod
- **WHEN** the player opens the Production Terminal, the Storage Terminal and a Utility Duct filter picker
- **THEN** their scrollbars have the same track, thumb and greyed state
