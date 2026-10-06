# Tasks

## 1. Editor

- [ ] 1.1 Add `match`, `loadPattern`, `replaceFromPattern` and `configuredFor` to `AssemblerBlockEntity`; rewrite `AssemblerMenu` with the editing state, slot activity and new button ids; rewrite `AssemblerScreen` with recipe cells, plus button and the popup; point the JEI payload at `openEditorWith`; update lang and the GUI texture. Verify `gradlew build` and `gradlew :manufacturing:runData` pass.

## 2. Verification

- [ ] 2.1 `gradlew :manufacturing:runGameTestServer` passes, `gradlew clean build` green, `gradlew runClientAll` reaches the title screen; the user checks the popup flow in game. Commit.
