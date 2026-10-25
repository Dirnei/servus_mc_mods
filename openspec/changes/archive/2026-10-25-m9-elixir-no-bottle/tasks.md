# Tasks

## 1. Elixir leaves no bottle

- [x] 1.1 Remove `.usingConvertsTo(Items.GLASS_BOTTLE)` from `ElixirOfSightItem.properties()` in `alchemy/.../sight/ElixirOfSightItem.java` and reword its Javadoc; change the drink GameTest in `alchemy/.../gametest/AlchemyGameTests.java` to assert the hand is empty after drinking. Verify with `gradlew :alchemy:runGameTestServer`.
- [x] 1.2 Full `gradlew build`, tests green, game launches with all mods.

## Workflow follow-up

- Archive with `/opsx:archive`, add an Alchemy CHANGELOG line under Unreleased (Changed: drinking the Elixir of Sight no longer gives back a glass bottle).
