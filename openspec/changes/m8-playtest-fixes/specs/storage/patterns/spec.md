# Spec Delta

## ADDED Requirements

### Requirement: Pattern Storage online lamp
Pattern Storage SHALL show an online lamp on its front next to the pattern lights, lit while a formed Crafting CPU or an online warehouse is on its data line and dark otherwise, refreshed about once a second.

#### Scenario: On a line with a CPU
- **WHEN** a Pattern Storage touches a data duct whose line has a formed CPU
- **THEN** its lamp is lit within about a second, and the pattern lights keep showing the stored count
