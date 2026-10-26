# shuttles/station-numbers Specification

## Purpose
Station numbers give every Inbound, Outbound and Packing Station of a warehouse a short number and an optional name, so the GUI, reports and stock rule states can tell the player which station they mean without coordinates.

## Requirements

### Requirement: Data Potato station report
Using the Data Potato on an Inbound, Outbound or Packing Station SHALL print a report into the player's chat and SHALL NOT open the GUI or change the station. The report SHALL start with the station's label, as for every named machine, followed by the site name of its warehouse and whether it is online, or the offline reason.

#### Scenario: Online station
- **WHEN** a player uses the potato on Outbound #3 named "Compost Dock" of the site "North Mine"
- **THEN** chat shows "Compost Dock (Outbound #3)", "North Mine" and that it is online, and no GUI opens

#### Scenario: Offline station
- **WHEN** a player uses the potato on a station whose rail reaches no controller
- **THEN** chat shows its label and that it has no controller
