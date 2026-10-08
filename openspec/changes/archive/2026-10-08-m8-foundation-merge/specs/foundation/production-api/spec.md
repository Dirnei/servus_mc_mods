# Spec Delta

## MODIFIED Requirements

### Requirement: Production capability
Foundation SHALL define a sided block capability `servus_foundation:production`. A provider SHALL list the recipes the block can run right now, each with a recipe id, the exact input stacks with counts, the output stacks with counts and the time in ticks, and SHALL say which recipe id it is working on, if any. Any mod SHALL be able to register a provider for its blocks and query one on any block.

#### Scenario: Query a producer
- **WHEN** a mod asks the production capability of a block whose provider knows one recipe making one iron gear from two iron ingots in 20 ticks
- **THEN** it gets one recipe with those inputs, that output and that time

#### Scenario: No producer
- **WHEN** a mod asks the production capability of a stone block
- **THEN** it gets nothing

### Requirement: Stock capabilities
Foundation SHALL define sided block capabilities `servus_foundation:stock_provider` and `servus_foundation:stock_sink`. A stock provider SHALL answer how many of an item like a given stack are available and SHALL take up to a requested amount of it. A stock sink SHALL accept a stack and return what did not fit.

#### Scenario: Count and take
- **WHEN** a mod asks a stock provider for cobblestone that holds 50 and takes 20
- **THEN** the count was 50, 20 cobblestone are returned and the count is 30 afterwards

#### Scenario: Sink full
- **WHEN** a mod hands 64 bread to a stock sink with room for 10
- **THEN** 54 bread are returned
