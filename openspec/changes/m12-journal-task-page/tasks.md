## 1. Progress to the client

- [x] 1.1 `JournalProgressPayload` with stream codec and pure `JournalProgress.snapshot(chapters, journal)` (open and finished chapters only) in `alchemy/.../journal/`; JUnit `JournalProgressTest` (open chapter listed, locked chapter left out, finished flag); run `:alchemy:test`
- [x] 1.2 Register the payload (`ServusAlchemyPayloads`), client cache `ClientJournalProgress`, dirty team set in `Journal` drained per server tick, sends at login and datapack sync; run `:alchemy:build`

## 2. Page

- [ ] 2.1 Page type `JournalTasksPage` (codec, stream codec) registered in common setup; datagen model for the page in `JournalEntries`; run `runData` and `:alchemy:build`
- [ ] 2.2 Client renderer `JournalTasksPageRenderer`: boxes, check marks, counts, bar, finished line; registered on the client; run `:alchemy:build`
- [ ] 2.3 Task page first after the title in every chapter entry, prose task lists removed, task name lang keys; run `runData` and `:alchemy:build`

## 3. Tests

- [ ] 3.1 GameTests: every shipped task has a lang key; recording an event marks the team dirty and the drain clears it; run `:alchemy:runGameTestServer`
- [ ] 3.2 Launch the client from main and open the journal: the page renders without errors

## 4. Play check

- [ ] 4.1 User play check: the Villach page shows live counts while the Calciner works, check marks appear, a finished chapter says so
