# Spec Delta

## ADDED Requirements

### Requirement: Site list scrollbar
The "Other sites" panel SHALL show at most 16 sites at once. While it lists sites, the shared Foundation scrollbar SHALL show beside the rows; with more sites than rows, dragging the thumb, a press on the track and the mouse wheel over the panel SHALL scroll the list one site per wheel notch, stopping at the first and last site. Hovering a site SHALL show the tooltip of the site under the mouse at the current scroll position.

#### Scenario: Many outposts
- **WHEN** the team has 20 other sites and the panel shows 16
- **THEN** a scrollbar beside the rows shows the visible share, and dragging its thumb to the bottom shows sites 5 to 20

#### Scenario: Few outposts
- **WHEN** the team has two other sites
- **THEN** the panel lists both and the scrollbar shows a greyed thumb over the whole track

#### Scenario: No list
- **WHEN** the controller is not wireless capable and the panel shows its hint
- **THEN** no scrollbar shows
