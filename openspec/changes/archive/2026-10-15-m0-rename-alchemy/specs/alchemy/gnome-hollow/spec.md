# Spec Delta

## MODIFIED Requirements

### Requirement: Veins are data
A vein SHALL be a JSON file at `data/<namespace>/servus_alchemy/vein/<name>.json` with sample item tags, a yield item, an optional journal chapter, the number of ripening steps, the ticks per step, the principles per step and the ticks per yielded item.

#### Scenario: Iron
- **WHEN** the shipped data holds an iron vein with samples `c:ores/iron` and `c:raw_materials/iron`, yield raw iron and chapter Villach
- **THEN** raw iron or any iron ore seeds an iron vein for a team that finished Villach
