package io.github.dirnei.mc.servus.storage.shuttle;

/**
 * Where a shuttle is heading. Field runs drive TO_FIELD, work a rail while
 * WORKING, fetch a supply run's items TO_SUPPLY, stop at the
 * Field Head's rail TO_HEAD for its buffer, put the cargo away
 * TO_STORE and wait at home in WAIT_STORE while nothing has room for it.
 */
enum Leg { TO_SOURCE, TO_TARGET, TO_STATION, TO_TERMINAL, WAITING_ROOM, HOME, TO_FIELD, WORKING, TO_SUPPLY, TO_HEAD, TO_STORE, WAIT_STORE }
