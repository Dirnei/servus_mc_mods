## MODIFIED Requirements

### Requirement: Binding a gnome counts
Binding a gnome SHALL report a `bind` event to the journal of the Hollow's team; it names no vein, since the gnome moves in before anything is planted. A vein that ripens SHALL report a `ripen` event for the vein's yield. Schwaz SHALL ask for one bound gnome; Wandering years SHALL ask for a ripe gold vein and Basel and Salzburg for a ripe diamond vein.

#### Scenario: First gnome
- **WHEN** a team with Schwaz open binds its first gnome
- **THEN** Schwaz's task "bind a gnome" is done

#### Scenario: Gold ripens
- **WHEN** a gold vein of a team with Wandering years open ripens
- **THEN** Wandering years' task "a gold vein" is done
