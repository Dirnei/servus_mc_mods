# Spec Delta

## ADDED Requirements

### Requirement: Pushed outputs count
Items of a step's recipe output that the step's producer pushes into an Access Point through the face touching the producer SHALL count for that step while it delivers or produces, up to what the step still needs; when several steps of the Access Point's data line match, the oldest order's step SHALL be served first. Items entering an Access Point through a face that does not touch the producer SHALL count for no step.

#### Scenario: Producer beside the Access Point
- **WHEN** a sticks step runs on a producer directly beside an Access Point and the producer pushes 4 sticks into it
- **THEN** the 4 sticks are put away, the step counts them and the order is done

#### Scenario: Other face
- **WHEN** 4 sticks enter that Access Point through a face that touches a hopper instead of the producer
- **THEN** the sticks are put away and the step counts none of them

#### Scenario: No double count
- **WHEN** the CPU moves the step's outputs as packets into an Access Point through its data duct
- **THEN** they count once, when the packets land
