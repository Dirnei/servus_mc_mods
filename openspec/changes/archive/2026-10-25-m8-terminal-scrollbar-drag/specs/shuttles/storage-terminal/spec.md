# Spec Delta

## ADDED Requirements

### Requirement: Dragging the grid scrollbar
Pressing the left mouse button on the item grid's scrollbar while no dialog is open SHALL scroll the grid so the thumb centres on the mouse, as far as the grid can scroll. Moving the mouse with the button held SHALL keep the thumb under the mouse, also when the mouse leaves the bar, until the button is released. The wheel SHALL keep scrolling as before.

#### Scenario: Drag to the end
- **WHEN** the grid lists more items than fit and the player presses on the thumb and drags it to the bottom of the bar
- **THEN** the grid shows its last rows and the thumb sits at the bottom of the bar

#### Scenario: Jump on press
- **WHEN** the player presses the left button halfway down the scrollbar of a grid with 20 rows to scroll
- **THEN** the grid jumps to about row 10

#### Scenario: Nothing to scroll
- **WHEN** all items fit in the grid and the player drags on the scrollbar
- **THEN** the grid stays where it is
