# Spec Delta

## Purpose

Station numbers give every Inbound, Outbound and Packing Station of a warehouse a short number and an optional name, so the GUI, reports and stock rule states can tell the player which station they mean without coordinates.

## ADDED Requirements

### Requirement: A station is numbered by its warehouse
An Inbound, Outbound or Packing Station without a number, or with a number from another warehouse, SHALL get a number when it belongs to an online warehouse. A station SHALL keep its number while the warehouse is offline and while it is out of the warehouse.

#### Scenario: Offline warehouse
- **WHEN** a station stands on a rail of a network without a controller
- **THEN** it gets no number until a controller makes the warehouse online

#### Scenario: Old world
- **WHEN** a world saved before station numbers loads a warehouse with three Inbound Stations
- **THEN** the three stations get Inbound #1 to #3 once the warehouse is online, and the warehouse works as before

#### Scenario: Packing Station saved with an Inbound number
- **WHEN** a world loads where Inbound #1, a Packing Station that took Inbound #2 and Inbound #3 stand in one warehouse
- **THEN** the Packing Station becomes Packing #1 once the warehouse is online, the Inbound Stations keep #1 and #3, and the next new Inbound Station is #4

### Requirement: Numbers count per kind and are never reused
Each warehouse SHALL count Inbound Stations, Outbound Stations and Packing Stations separately, each counter starting at 1, and SHALL never give a number twice for the same kind, also after the station that had it was broken. Stations that get their numbers at the same moment, such as all stations of a warehouse after a world loads, SHALL be numbered in a fixed order of their positions.

#### Scenario: First stations
- **WHEN** a player builds a warehouse and links two Outbound Stations and one Inbound Station to it by rail, one after the other
- **THEN** the Outbound Stations are Outbound #1 and Outbound #2 and the Inbound Station is Inbound #1

#### Scenario: Packing Station counts on its own
- **WHEN** a player links an Inbound Station, then a Packing Station, then a second Inbound Station to one warehouse
- **THEN** they are Inbound #1, Packing #1 and Inbound #2

#### Scenario: Number not reused
- **WHEN** Outbound #2 is broken and a new Outbound Station is linked to the same warehouse
- **THEN** the new station is Outbound #3

#### Scenario: Survives a reload
- **WHEN** Inbound #1 and #2 exist, the world is saved and loaded again, and a new Inbound Station is linked
- **THEN** the old stations keep their numbers and the new station is Inbound #3

### Requirement: The number follows the station
A station dismantled with the Configuration Carrot SHALL keep its number, its name and the warehouse that numbered it in the dropped item. Placed back into that warehouse it SHALL keep its number; placed into another warehouse it SHALL get a new number from that warehouse and keep its name. A station broken any other way SHALL drop without number or name.

#### Scenario: Moved within the warehouse
- **WHEN** the player dismantles Outbound #2 with the carrot and places it at another rail of the same warehouse
- **THEN** it is still Outbound #2 and the next new Outbound Station is #3

#### Scenario: Moved to another warehouse
- **WHEN** the player dismantles Outbound #2 named "Compost Dock" and places it in a second warehouse whose last Outbound Station is #4
- **THEN** it reads "Compost Dock (#5)"

### Requirement: The controller carries the warehouse's count
The warehouse's identity and station counters SHALL travel with the Warehouse Controller when it is dismantled with the carrot, so its stations stay numbered in it when the controller is placed again. A controller broken any other way SHALL start a new warehouse whose counters start at 1.

#### Scenario: Controller moved
- **WHEN** the player dismantles the controller with the carrot and places it elsewhere on the same rails
- **THEN** every station keeps its number and the next new Inbound Station continues the count

#### Scenario: Controller broken
- **WHEN** the controller is broken with a pickaxe and a new one is placed on the same rails
- **THEN** the stations are numbered again from Inbound #1 and Outbound #1

### Requirement: Station name
A station SHALL have an optional name. The GUI of an Inbound, Outbound or Packing Station SHALL show a text field in its title row that shows the station's label as a hint while no name is set; the name SHALL be saved when the field loses focus or the GUI closes. Only a player who may open the station's GUI SHALL be able to rename it.

#### Scenario: Name a station
- **WHEN** the player opens Outbound #3, types "Compost Dock" and closes the GUI
- **THEN** the station's label reads "Compost Dock (#3)"

#### Scenario: Name a Packing Station
- **WHEN** the player opens Packing #1, types "Parcel Dock" and closes the GUI
- **THEN** the station's label reads "Parcel Dock (#1)" and its title row shows "#1" at the right

### Requirement: Station names are cleaned
A typed name SHALL be cleaned like a site name: control and formatting characters removed, trimmed and cut to the site name length (example 32). A name that is empty after cleaning SHALL remove the name.

#### Scenario: Name cleaned
- **WHEN** the player types "  Compost\nDock  "
- **THEN** the name is "CompostDock"

#### Scenario: Name removed
- **WHEN** the player clears the field of "Compost Dock (#3)" and closes the GUI
- **THEN** the label reads "Outbound #3"

### Requirement: Station label
A station SHALL be named by its label wherever one station is named, in its GUI title and in reports. A station with a number and no name SHALL read "Inbound #n", "Outbound #n" or "Packing #n"; a station with a number and the name "Compost Dock" SHALL read "Compost Dock (#n)"; a station without a number SHALL read "Inbound Station", "Outbound Station" or "Packing Station", or its name when it has one. The label SHALL be computed by pure logic that is unit tested without Minecraft.

#### Scenario: Unnamed
- **WHEN** Outbound #3 has no name
- **THEN** its GUI title reads "Outbound #3"

#### Scenario: Named
- **WHEN** Inbound #1 is named "Quarry Intake"
- **THEN** its GUI title reads "Quarry Intake (#1)"

#### Scenario: Packing Station
- **WHEN** Packing #2 has no name
- **THEN** its GUI title, its Data Potato report and any station state naming it read "Packing #2", never an Inbound number

#### Scenario: Not numbered yet
- **WHEN** an Outbound Station was never part of an online warehouse
- **THEN** its GUI title reads "Outbound Station"

### Requirement: Data Potato station report
Using the Data Potato on an Inbound, Outbound or Packing Station SHALL print a report into the player's chat and SHALL NOT open the GUI or change the station. The report SHALL name the station by its label, the site name of its warehouse and whether it is online, or the offline reason.

#### Scenario: Online station
- **WHEN** a player uses the potato on Outbound #3 named "Compost Dock" of the site "North Mine"
- **THEN** chat shows "Compost Dock (#3)", "North Mine" and that it is online, and no GUI opens

#### Scenario: Offline station
- **WHEN** a player uses the potato on a station whose rail reaches no controller
- **THEN** chat shows its label and that it has no controller
