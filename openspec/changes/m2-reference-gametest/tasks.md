# Tasks

## 1. GameTest

- [ ] 1.1 Generate `data/servus_manufacturing/structure/empty_12x6x12.nbt`, add `ServusManufacturingGameTests` with the reference block test and register it in `ServusManufacturingEvents`. Verify `gradlew build` passes.
- [ ] 1.2 Run `gradlew :manufacturing:runGameTestServer`. Verify the log reports the test passed and the task exits with status 0. If the pack count is outside the window, record the measured count in design.md and adjust the layout, not the window. Commit.
