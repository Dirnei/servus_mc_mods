# Tasks

## 1. Pure logic

- [x] 1.1 `notes/model/PinLines`, `notes/model/NewTicks`; JUnit `PinLinesTest`, `NewTicksTest`; verify `gradlew :notes:test`

## 2. Server

- [x] 2.1 `NotesData` pins and codec; `network/PinPayload`, `network/PinnedPayload`; pin cleared with `ListGonePayload`; extend `NotesCodecsTest`; verify `gradlew build`

## 3. Client

- [x] 3.1 `config/NotesClientConfig`; `client/HudPin` layer; "Toggle HUD pin" in `NotesKeys`; verify `gradlew build`
- [x] 3.2 Pin and Unpin on the right page, "pinned" mark on the contents page; tick toasts in `ClientNotes` (max three per snapshot); lang keys, `runData`; verify `gradlew build`

## 4. GameTests

- [x] 4.1 GameTest: pin refused for an invisible list, cleared when a guest is removed; verify `gradlew :notes:runGameTestServer` and `gradlew build test`

## 5. Final

- [x] 5.1 Full gradle build, tests green, `clientAll` and `:notes:runClient` launch; pin a list, try corners and lines, hide with the key, see a toast when a second player ticks
- [x] 5.2 Play check by the user
