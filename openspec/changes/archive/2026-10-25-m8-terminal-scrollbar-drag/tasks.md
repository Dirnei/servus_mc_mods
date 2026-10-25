# Tasks

## 1. Pure helper

- [x] 1.1 Add `ListScroll.offsetAt(double along, int thumb, int track, int maxOffset)` in `foundation/.../ui/ListScroll.java`: the offset whose thumb, `thumb` long on a bar `track` long, centres on `along`, clamped to 0 and `maxOffset`, 0 when `maxOffset` is 0. JUnit in `ListScrollTest`: top and bottom of the bar give 0 and `maxOffset`, the middle gives half, positions outside the bar clamp, nothing to scroll gives 0. Verify with `gradlew :foundation:test`.

## 2. Storage Terminal

- [x] 2.1 In `shuttles/.../client/StorageTerminalScreen.java`: a left press on `layout.scrollbar()` with no dialog open starts dragging and sets `scrollRow` through `ListScroll.offsetAt` (thumb 15, track the bar's height); `mouseDragged` keeps setting it while dragging; `mouseReleased` ends it. Verify with `gradlew :shuttles:build`.
- [x] 2.2 Full `gradlew build`, tests green, game launches with all mods; in the play check drag the thumb up and down, press on the bar, and drag past its ends.

## Workflow follow-up

- Archive with `/opsx:archive`, add a Shuttles CHANGELOG line under Unreleased (Fixed: the Storage Terminal's scrollbar can be dragged with the mouse).
