package io.github.dirnei.mc.servus.shuttles.shuttle;

/**
 * Where a shuttle is heading. Field runs drive TO_FIELD, work a rail while
 * WORKING, fetch a supply run's items TO_SUPPLY, stop at the
 * Field Head's rail TO_HEAD for its buffer and put the cargo away
 * TO_STORE. A shuttle whose cargo no rack takes is STRANDED: it drives home
 * and parks there with the cargo. WAITING_ROOM is only read from old saves.
 */
enum Leg { TO_SOURCE, TO_TARGET, TO_STATION, TO_TERMINAL, WAITING_ROOM, HOME, TO_FIELD, WORKING, TO_SUPPLY, TO_HEAD, TO_STORE, STRANDED }
