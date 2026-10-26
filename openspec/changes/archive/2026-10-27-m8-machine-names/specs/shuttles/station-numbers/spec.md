# Spec Delta

## REMOVED Requirements

### Requirement: A station is numbered by its warehouse
**Reason**: Stations are one kind of named machine; numbering for every kind lives in `shuttles/machine-numbers`.
**Migration**: See "A machine is numbered by its warehouse" in `shuttles/machine-numbers`; station behavior is unchanged.

### Requirement: Numbers count per kind and are never reused
**Reason**: Moved to `shuttles/machine-numbers`, which counts every named machine kind, stations included.
**Migration**: See "Numbers count per kind and are never reused" in `shuttles/machine-numbers`.

### Requirement: The number follows the station
**Reason**: Moved to `shuttles/machine-numbers` ("The number follows the machine") and `foundation/machine-names` ("Name and number follow the dismantled machine").
**Migration**: Station behavior is unchanged.

### Requirement: The controller carries the warehouse's count
**Reason**: Moved to `shuttles/machine-numbers`, where the controller carries the counters of every kind.
**Migration**: Station numbers were never released; worlds from play tests number their stations again from #1.

### Requirement: Station name
**Reason**: Every named machine is renamed the same way; see "Machine name" and "Name in the title row" in `foundation/machine-names`.
**Migration**: Station behavior is unchanged.

### Requirement: Station names are cleaned
**Reason**: Moved to "Machine name" in `foundation/machine-names`.
**Migration**: Same cleaning rules.

### Requirement: Station label
**Reason**: Moved to "Machine label" in `foundation/machine-names`.
**Migration**: Station labels read as before ("Outbound #3", "Outbound Station"), except that a named station also names its kind: "Compost Dock (Outbound #3)".

## MODIFIED Requirements

### Requirement: Data Potato station report
Using the Data Potato on an Inbound, Outbound or Packing Station SHALL print a report into the player's chat and SHALL NOT open the GUI or change the station. The report SHALL start with the station's label, as for every named machine, followed by the site name of its warehouse and whether it is online, or the offline reason.

#### Scenario: Online station
- **WHEN** a player uses the potato on Outbound #3 named "Compost Dock" of the site "North Mine"
- **THEN** chat shows "Compost Dock (Outbound #3)", "North Mine" and that it is online, and no GUI opens

#### Scenario: Offline station
- **WHEN** a player uses the potato on a station whose rail reaches no controller
- **THEN** chat shows its label and that it has no controller
