## ADDED Requirements

### Requirement: Notes identity
Servus: Notes SHALL load with the mod id `servus_notes` and the display name "Servus: Notes", and SHALL declare no dependency, required or optional, on any other Servus mod.

#### Scenario: Mod list
- **WHEN** a game starts with `servus_notes`
- **THEN** the Mods screen lists "Servus: Notes" with its version and the license "MIT"

### Requirement: Notes loads standalone
Servus: Notes SHALL load in a Minecraft 26.1.2 game on NeoForge 26.1.2.x with no other Servus mod present, and SHALL load together with any combination of the other Servus mods.

#### Scenario: Only Notes installed
- **WHEN** a client or dedicated server starts with `servus_notes` and no other Servus mod
- **THEN** the game reaches the title screen or the server reaches the "Done" state without errors about missing dependencies

#### Scenario: Whole family installed
- **WHEN** a game starts with every Servus mod
- **THEN** all of them, Notes included, appear in the mod list and the game reaches the title screen or the server reaches the "Done" state

### Requirement: English names
Every name and text Servus: Notes shows SHALL come from its `en_us` language file; it SHALL ship no other language.

#### Scenario: No missing keys
- **WHEN** a player opens the notebook and the key binding screen
- **THEN** every title, label and key name reads as English text, never as a raw translation key
