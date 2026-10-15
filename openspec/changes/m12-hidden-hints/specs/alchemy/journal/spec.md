## ADDED Requirements

### Requirement: Find events
The journal SHALL count a `find` event when a team finds something hidden: picking up a trace counts with the trace item as the event's item (filters are an item id or `#tag`), finding the old gnome counts with the name `servus_alchemy:old_gnome` as filter.

#### Scenario: Trace found
- **WHEN** a player picks up a Tiny Pickaxe
- **THEN** a `find` event for the Tiny Pickaxe counts for their team

### Requirement: The Old Gnome chapter
The journal SHALL ship a side chapter "The Old Gnome" (`servus_alchemy:old_gnome`) with Schwaz as its parent and two tasks, both "for the first time": find a trace (`find` with filter `#servus_alchemy:hint_traces`) and find the old gnome (`find` with filter `servus_alchemy:old_gnome`). Finishing it grants its journal advancement. It SHALL NOT be the parent of any main chapter, so the main chapters never wait for it. Its Opus entry SHALL open with Schwaz and tell, in Paracelsus' voice, of the knockers of the mines.

#### Scenario: Chapter finished
- **WHEN** a team with Schwaz finished picks up a trace and finds the old gnome
- **THEN** The Old Gnome is finished

#### Scenario: Main chapters do not wait
- **WHEN** a team finishes Wandering years without ever finding the old gnome
- **THEN** Basel and Salzburg opens as before
