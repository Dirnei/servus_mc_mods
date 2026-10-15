## 1. Ledger

- [x] 1.1 Add `JournalLedger.finish` in `alchemy/src/main/java/io/github/dirnei/mc/servus/alchemy/journal/JournalLedger.java` with JUnit tests in `JournalLedgerTest` (finishes the parent chain, already finished returns empty, unknown id changes nothing); run `:alchemy:test`

## 2. Command

- [ ] 2.1 Add `Journal.finish` and the `journal finish <chapter>` operator command with suggestions in `Journal.java` and `JournalCommand.java`, lang keys in `ServusAlchemyLanguageProvider`; run `runData` and `:alchemy:build`
- [ ] 2.2 GameTest: the command path through `Journal.finish` finishes Villach and Schwaz for a team; run `:alchemy:runGameTestServer`

## 3. Play check

- [ ] 3.1 User play check: `/servus_alchemy journal finish servus_alchemy:schwaz` in the play world unlocks the Citrinitas entries
