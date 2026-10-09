# Spec Delta

## ADDED Requirements

### Requirement: Results go straight to the requesting terminal
A work order made for a Counter Terminal request SHALL remember that terminal. When the CPU collects the outputs of a step that makes the order's item, it SHALL send up to the amount the request still needs (and at most the order's amount) directly from the producer to the terminal as packets; the rest, and every output of a step that makes another item, SHALL go to an Access Point and be put away. Outputs SHALL count for the step wherever they land. When the terminal cannot be reached from the producer's data line the outputs SHALL go to the Access Point. The link and the amount already sent SHALL be saved with the order.

#### Scenario: Sticks for a request
- **WHEN** a request for 8 sticks makes an order and the station crafts 8 sticks
- **THEN** the sticks travel from the station straight into the terminal's output slots and never enter the warehouse

#### Scenario: Batch surplus
- **WHEN** a request for 2 sticks makes an order whose one craft gives 4 sticks
- **THEN** 2 sticks go to the terminal and 2 are put away

#### Scenario: Chain
- **WHEN** a request for sticks makes an order that first crafts planks from logs
- **THEN** the planks go to storage and only the sticks go to the terminal

#### Scenario: Stock rule
- **WHEN** a stock rule makes an order
- **THEN** its outputs are put away as before
