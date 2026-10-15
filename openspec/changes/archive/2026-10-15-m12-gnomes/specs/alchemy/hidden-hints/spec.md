## MODIFIED Requirements

### Requirement: The old gnome under the sight
Under Elemental Sight the Ancient Hollow SHALL show the old gnome in front of its open face as a House Gnome (see `alchemy/gnomes`). Using Paracelsus' Journal on the Ancient Hollow or on him under the sight SHALL show a dialogue line marked as a placeholder (`[storyteller] ...`), record the `find` event `servus_alchemy:old_gnome` for the player's team and mark the owning team's old gnome as found. Without the sight the journal SHALL do nothing special on it.

#### Scenario: Finding him
- **WHEN** a player under the sight uses the journal on the Ancient Hollow
- **THEN** the placeholder dialogue line shows, the `find` event `servus_alchemy:old_gnome` counts and the old gnome is found

#### Scenario: Without the sight
- **WHEN** a player without the sight uses the journal on it
- **THEN** no dialogue shows and nothing is found
