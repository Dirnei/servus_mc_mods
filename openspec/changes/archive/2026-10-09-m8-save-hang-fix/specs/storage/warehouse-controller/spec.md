# Spec Delta

## ADDED Requirements

### Requirement: The network never loads chunks
The warehouse network SHALL NOT load a chunk when a member joins or leaves, when candidates are rebuilt, or when it looks up blocks of its members; a lookup in a chunk that is not loaded SHALL count as nothing there. A world whose warehouse spans several chunks SHALL save and stop like a world without one.

#### Scenario: Stop with a warehouse across chunks
- **WHEN** a server stops while a warehouse whose rails cross several chunk borders is loaded
- **THEN** every chunk unloads once, the worlds are saved and the server exits

#### Scenario: Member leaves beside an unloaded chunk
- **WHEN** a member leaves a network whose other members lie in a chunk that is not loaded
- **THEN** that chunk stays unloaded
