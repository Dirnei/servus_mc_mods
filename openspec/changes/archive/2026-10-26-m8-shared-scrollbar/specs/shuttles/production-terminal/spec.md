# Spec Delta

## ADDED Requirements

### Requirement: List scrollbar
The list of every tab (Crafting, Patterns, Machines, CPUs, Storage) SHALL show the shared Foundation scrollbar right of the list, for the tab on display. Its thumb SHALL show the share of the tab's rows that fits the list. Dragging the thumb, a press on the track and the mouse wheel over the list or the bar SHALL scroll the list, the wheel by the same step as before. Each tab SHALL keep its own scroll position.

#### Scenario: Many jobs
- **WHEN** the Crafting tab lists twelve jobs with buffer lines, more than the list shows
- **THEN** a scrollbar right of the list shows the visible share, and dragging its thumb to the bottom shows the last job

#### Scenario: Patterns tab
- **WHEN** Pattern Storage holds 40 patterns and the player presses halfway down the track
- **THEN** the list jumps to about the middle of the patterns

#### Scenario: Short tab
- **WHEN** the CPUs tab lists one CPU
- **THEN** the scrollbar shows a greyed thumb over the whole track and the list does not move

#### Scenario: Tabs keep their position
- **WHEN** the player scrolls the Machines tab to its end, opens the Storage tab and returns to Machines
- **THEN** Machines is still at its end and its thumb at the bottom of the track

#### Scenario: Editor open
- **WHEN** the pattern editor is open over the list
- **THEN** no scrollbar shows and the wheel does not scroll the list behind the editor

## MODIFIED Requirements

### Requirement: Screen layout
The Production Terminal screen SHALL show the title on its own row, the tab bar on the row below it and the list below the tab bar, so the title, the tabs and the rows never overlap; the list's scrollbar SHALL have its own column right of the list, beside the list's full height, so no row part reaches under it; each tab SHALL be as wide as its label plus an even share of the row's spare width, so all tabs fit the row; the parts of a job row (icon, name, progress bar, step text, cancel button), of a buffer line under a job, of a machine row (icon, name, state, upkeep bar), of a CPU row (icon, name, slot bar, buffer bar, buffered items) and of a storage row (name, numbers, bar) SHALL not overlap and SHALL fit the list width; the pattern editor SHALL cover the list area and the scrollbar column with the player inventory below it, centred inside the panel. The panel SHALL fit a 1080p screen at the automatic GUI scale.

#### Scenario: Title and tabs
- **WHEN** the screen opens with all five tabs
- **THEN** the title and the tabs do not overlap, the tabs fit the row, and every tab label is readable

#### Scenario: Job row
- **WHEN** a job row shows a long item name and a long step text
- **THEN** the texts are cut short before the progress bar and the cancel button

#### Scenario: CPU row
- **WHEN** a CPU row shows a CPU with many different buffered items
- **THEN** the items that do not fit the row are cut short with a count of the rest and listed in full in the tooltip

#### Scenario: Storage row
- **WHEN** a zone row shows a long zone name
- **THEN** the name is cut short before the numbers and the bar

#### Scenario: Scrollbar column
- **WHEN** a job row with its cancel button, a CPU row with its bars and a hovered pattern row show while the list scrolls
- **THEN** the cancel button, the bars and the hover shade end left of the scrollbar and nothing overlaps it
