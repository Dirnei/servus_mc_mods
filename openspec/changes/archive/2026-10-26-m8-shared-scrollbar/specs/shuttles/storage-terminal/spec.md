# Spec Delta

## MODIFIED Requirements

### Requirement: Dragging the grid scrollbar
The item grid's scrollbar SHALL be the shared Foundation scrollbar, for the Stock tab's grid and the Rules tab's rule rows alike, and SHALL take presses only while no dialog is open. Its thumb SHALL show the share of the rows that fits the grid. Pressing on the thumb SHALL grab it where pressed; pressing on the track SHALL centre the thumb on the mouse, as far as the grid can scroll. Moving the mouse with the button held SHALL keep the thumb under the mouse, also when the mouse leaves the bar, until the button is released. The wheel SHALL keep scrolling as before.

#### Scenario: Drag to the end
- **WHEN** the grid lists more items than fit and the player presses on the thumb and drags it to the bottom of the bar
- **THEN** the grid shows its last rows and the thumb sits at the bottom of the bar

#### Scenario: Jump on press
- **WHEN** the player presses the left button halfway down the track, away from the thumb, of a grid with 20 rows to scroll
- **THEN** the grid jumps to about row 10

#### Scenario: Nothing to scroll
- **WHEN** all items fit in the grid and the player drags on the scrollbar
- **THEN** the grid stays where it is and the thumb fills the bar in grey

#### Scenario: Thumb shows the share
- **WHEN** the grid shows 6 rows of a stock that fills 60 rows
- **THEN** the thumb is about a tenth of the bar long

#### Scenario: Grab without a jump
- **WHEN** the grid is at its top and the player presses on the lower end of the thumb
- **THEN** the grid stays at its top until the mouse moves

#### Scenario: Rules tab
- **WHEN** the Rules tab lists more rules than fit and the player drags the thumb to the bottom
- **THEN** the last rules show and the add row stays on top

#### Scenario: Dialog open
- **WHEN** the item popup is open over the grid and the player presses on the scrollbar
- **THEN** the grid does not scroll
