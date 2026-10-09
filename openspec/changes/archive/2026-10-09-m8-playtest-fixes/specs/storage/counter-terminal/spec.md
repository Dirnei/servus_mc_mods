# Spec Delta

## ADDED Requirements

### Requirement: Content shows when the screen returns
The Counter Terminal screen SHALL show its terminal's last known content whenever it is shown, including when the same screen comes back after another screen was in front of it, and SHALL NOT fall back to "Loading..." while content for that terminal exists in the session.

#### Scenario: Back from the recipe viewer
- **WHEN** the player looks up a recipe in the recipe viewer from the Counter Terminal and returns
- **THEN** the stock and requests are shown at once

### Requirement: Right click clears the search
Right clicking the search box SHALL clear it and show the unfiltered list.

#### Scenario: Clear
- **WHEN** the search box holds "plank" and the player right clicks it
- **THEN** the box is empty and every entry is listed again
