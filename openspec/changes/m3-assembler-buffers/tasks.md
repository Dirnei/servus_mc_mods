# Tasks

## 1. Buffers and GUI

- [ ] 1.1 Resize the assembler to 18 inputs and 4 outputs with remainders routed to outputs, add the inventory offset and energy position hooks, widen the screen and regenerate the texture. Verify `gradlew build` passes.

## 2. Verification

- [ ] 2.1 Server check over RCON: cake recipe configured, ingredients in the inputs, powered; after one work period the outputs hold a cake and three buckets. `gradlew :manufacturing:runGameTestServer` passes. `gradlew clean build` green and `gradlew runClientAll` reaches the title screen. Commit.
